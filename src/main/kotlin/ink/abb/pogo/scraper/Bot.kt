/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.util.concurrent.AtomicDouble
import com.google.maps.GeoApiContext
import com.pokegoapi.api.PokemonGo
import com.pokegoapi.api.map.MapObjects
import com.pokegoapi.api.map.fort.Pokestop
import com.pokegoapi.api.player.PlayerProfile
import com.pokegoapi.api.pokemon.Pokemon
import ink.abb.pogo.scraper.controllers.ProgramController
import ink.abb.pogo.scraper.gui.SocketServer
import ink.abb.pogo.scraper.tasks.*
import ink.abb.pogo.scraper.util.Log
import ink.abb.pogo.scraper.util.cachedInventories
import ink.abb.pogo.scraper.util.directions.RouteProviderEnum
import ink.abb.pogo.scraper.util.inventory.size
import ink.abb.pogo.scraper.util.pokemon.getIv
import ink.abb.pogo.scraper.util.pokemon.getIvPercentage
import java.io.File
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Phaser
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.thread

class Bot(val api: PokemonGo, val settings: Settings) {

    private var runningLatch = CountDownLatch(0)
    var prepareWalkBack = AtomicBoolean(false)
    var walkBackLock = AtomicBoolean(true)
    var altitudeCache: MutableMap<String, Double> =
            try {
                ObjectMapper().readValue(File("altitude_cache.json").readText(), MutableMap::class.java) as MutableMap<String, Double>
            } catch (ex: Exception) {
                mutableMapOf()
            }

    lateinit private var phaser: Phaser
    var ctx = Context(
            api,
            api.playerProfile,
            AtomicDouble(settings.latitude),
            AtomicDouble(settings.longitude),
            AtomicLong(api.playerProfile.stats.experience),
            LocalDateTime.now(),
            Pair(AtomicInteger(0), AtomicInteger(0)),
            AtomicInteger(0),
            AtomicInteger(0),
            Pair(AtomicInteger(0), AtomicInteger(0)),
            AtomicDouble(settings.speed),
            mutableSetOf(),
            SocketServer(),
            AtomicBoolean(false),
            settings.restApiPassword,
            altitudeCache,
            geoApiContext = if (settings.followStreets.contains(RouteProviderEnum.GOOGLE) && settings.googleApiKey.startsWith("AIza")) {
                GeoApiContext().setApiKey(settings.googleApiKey)
            } else {
                GeoApiContext()
            }
    )

    @Synchronized
    fun start() {
        if (isRunning()) return

        if (settings.saveLocationOnShutdown && settings.savedLatitude!=0.0 && settings.savedLongitude!=0.0) {
            ctx.lat.set(settings.savedLatitude)
            ctx.lng.set(settings.savedLongitude)
            Log.normal("Loaded last saved location (${settings.savedLatitude}, ${settings.savedLatitude})")
        }

        ctx.walking.set(false)

        Log.normal("Name: ${ctx.profile.playerData.username} - Team: ${ctx.profile.playerData.team.name}")
        Log.normal("Level ${ctx.profile.stats.level}, Experience ${ctx.profile.stats.experience}; Pokecoin: ${ctx.profile.currencies[PlayerProfile.Currency.POKECOIN]}")
        Log.normal("Pokebank ${ctx.api.cachedInventories.pokebank.pokemons.size + ctx.api.inventories.hatchery.eggs.size}/${ctx.profile.playerData.maxPokemonStorage}; Stardust: ${ctx.profile.currencies[PlayerProfile.Currency.STARDUST]}; Inventory ${ctx.api.cachedInventories.itemBag.size()}/${ctx.profile.playerData.maxItemStorage}")

        val compareName = Comparator<Pokemon> { a, b ->
            a.pokemonId.name.compareTo(b.pokemonId.name)
        }
        val compareIv = Comparator<Pokemon> { a, b ->
            // compare b to a to get it descending
            if (settings.sortByIv) {
                b.getIv().compareTo(a.getIv())
            } else {
                b.cp.compareTo(a.cp)
            }
        }
        api.cachedInventories.pokebank.pokemons.sortedWith(compareName.thenComparing(compareIv)).map {
            val pnickname = if (!it.nickname.isEmpty()) " (${it.nickname})" else ""
            "Have ${it.pokemonId.name}$pnickname with ${it.cp}/${it.maxCpForPlayer} ${it.cpInPercentageActualPlayerLevel}% CP and IV (${it.individualAttack}-${it.individualDefense}-${it.individualStamina}) ${it.getIvPercentage()}% "
        }.forEach { Log.normal(it) }

        val keepalive = GetMapRandomDirection()
        val drop = DropUselessItems()
        val profile = UpdateProfile()
        val catch = CatchOneNearbyPokemon()
        val release = ReleasePokemon()
        val evolve = EvolvePokemon()
        val hatchEggs = HatchEggs()
        val export = Export()

        if (settings.export.length > 0)
            task(export)

        task(keepalive)
        Log.normal("Getting initial pokestops...")

        val sleepTimeout = 10L
        val originalInitialMapSize = settings.initialMapSize
        var retries = 0
        var reply: MapObjects?
        do {
            reply = api.map.getMapObjects(settings.initialMapSize)
            Log.normal("Got ${reply.pokestops.size} pokestops")
            if (reply == null || reply.pokestops.size == 0) {
                retries++
                if (retries % 3 == 0) {
                    if (settings.initialMapSize > 1) {
                        settings.initialMapSize -= 2
                        Log.red("Decreasing initialMapSize to ${settings.initialMapSize}")
                    } else {
                        Log.red("Cannot decrease initialMapSize even further. Are your sure your latitude/longitude is correct?")
                        Log.yellow("This is what I am trying to fetch: " +
                                "https://www.google.com/maps/@${settings.latitude},${settings.longitude},15z")
                    }
                }
                Log.red("Retrying in $sleepTimeout seconds...")
                Thread.sleep(sleepTimeout * 1000)
            }
        } while (reply == null || reply.pokestops.size == 0)
        if (originalInitialMapSize != settings.initialMapSize) {
            Log.red("Too high initialMapSize ($originalInitialMapSize) found, " +
                    "please change the setting in your config to ${settings.initialMapSize}")
        }
        val process = ProcessPokestops(reply.pokestops)

        runningLatch = CountDownLatch(1)
        phaser = Phaser(1)

        runLoop(TimeUnit.SECONDS.toMillis(settings.profileUpdateTimer), "ProfileLoop") {
            task(profile)
            task(hatchEggs)
            if (settings.export.length > 0)
                task(export)
            if (settings.evolveStackLimit > 0)
                task(evolve)
        }

        runLoop(TimeUnit.SECONDS.toMillis(5), "BotLoop") {
            task(keepalive)
            if (settings.catchPokemon && !ctx.pokemonInventoryFullStatus.get()) {
                try {
                    task(catch)
                } catch (e: Exception) {
                    // might have errored and paused walking
                    ctx.pauseWalking.set(false)
                }
            }
            if (settings.dropItems)
                task(drop)
            if (settings.autotransfer)
                task(release)
        }

        runLoop(500, "PokestopLoop") {
            if (!prepareWalkBack.get())
                task(process)
            else if (!ctx.walking.get())
                task(WalkToStartPokestop(process.startPokestop as Pokestop))
            if(checkForPlannedStop()){
                stop()
            }
        }

        Log.setContext(ctx)

        if (settings.guiPortSocket > 0) {
            Log.normal("Running socket server on port ${settings.guiPortSocket}")
            ctx.server.start(ctx, settings.guiPortSocket)
            /*var needPort = ""
            if (settings.guiPortSocket != 8001) {
                needPort = "#localhost:${settings.guiPortSocket}"
            }*/
            Log.green("Open the map on http://ui.pogobot.club/")
        }


        if (settings.timerWalkToStartPokestop > 0)
            runLoop(TimeUnit.SECONDS.toMillis(settings.timerWalkToStartPokestop), "BotWalkBackLoop") {
                if (!prepareWalkBack.get())
                    Log.cyan("Will go back to starting PokeStop in ${settings.timerWalkToStartPokestop} seconds")
                runningLatch.await(TimeUnit.SECONDS.toMillis(settings.timerWalkToStartPokestop), TimeUnit.MILLISECONDS)
                prepareWalkBack.set(true)
                while (walkBackLock.get()) {
                }
            }
    }

    fun runLoop(timeout: Long, name: String, block: (cancel: () -> Unit) -> Unit) {
        phaser.register()
        thread(name = "${settings.name}: $name") {
            try {
                var cancelled = false
                while (!cancelled && isRunning()) {
                    val start = api.currentTimeMillis()

                    try {
                        block({ cancelled = true })
                    } catch (t: Throwable) {
                        Log.red("Error running loop $name!")
                        t.printStackTrace()
                    }

                    if (cancelled) continue

                    val sleep = timeout - (api.currentTimeMillis() - start)

                    if (sleep > 0) {
                        runningLatchAwait(sleep, TimeUnit.MILLISECONDS)
                    }
                }
            } finally {
                phaser.arriveAndDeregister()
            }
        }
    }

    @Synchronized
    fun stop() {
        if (!isRunning()) return
        if (settings.saveLocationOnShutdown) {
            Log.normal("Saving current location (${ctx.lat.get()}, ${ctx.lng.get()})")
            settings.savedLatitude = ctx.lat.get()
            settings.savedLongitude = ctx.lng.get()
        }
        val socketServerStopLatch = CountDownLatch(1)
        thread {
            Log.red("Stopping SocketServer...")
            ctx.server.stop()
            Log.red("Stopped SocketServer.")
            socketServerStopLatch.countDown()
        }

        Log.red("Stopping bot loops...")
        runningLatch.countDown()
        phaser.arriveAndAwaitAdvance()
        Log.red("All bot loops stopped.")

        socketServerStopLatch.await()
    }

    fun runningLatchAwait(timeout: Long, unit: TimeUnit) {
        try {
            runningLatch.await(timeout, unit)
        } catch (ignore: InterruptedException) {
        }
    }

    fun isRunning(): Boolean {
        return runningLatch.count > 0
    }

    fun task(task: Task) {
        task.run(this, ctx, settings)
    }

    fun checkForPlannedStop():Boolean {
        val timeDiff:Long = ChronoUnit.MINUTES.between(ctx.startTime, LocalDateTime.now())
        val pokemonCatched:Int = ctx.pokemonStats.first.get()
        val pokestopsVisited:Int = ctx.pokestops.get()
        //Log.red("time: ${timeDiff}, pokemon: ${pokemonCatched}, pokestops: ${pokestopsVisited}")
        if(settings.botTimeoutAfterMinutes <= timeDiff && settings.botTimeoutAfterMinutes != -1){
            Log.red("Bot timed out as declared in the settings (after ${settings.botTimeoutAfterMinutes} minutes)")
            return true
        } else if(settings.botTimeoutAfterCatchingPokemon <= pokemonCatched && settings.botTimeoutAfterCatchingPokemon != -1){
            Log.red("Bot timed out as declared in the settings (after catching ${settings.botTimeoutAfterCatchingPokemon} pokemon)")
            return true
        } else if(settings.botTimeoutAfterVisitingPokestops <= pokestopsVisited && settings.botTimeoutAfterVisitingPokestops != -1){
            Log.red("Bot timed out as declared in the settings (after visiting ${settings.botTimeoutAfterVisitingPokestops} pokestops)")
            return true
        }
        return false
    }

    fun terminateApplication(){
        phaser.forceTermination()
        ProgramController.stopAllApplications()
    }

}
