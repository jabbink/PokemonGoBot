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
import ink.abb.pogo.api.PoGoApi
import ink.abb.pogo.api.cache.BagPokemon
import ink.abb.pogo.api.cache.Pokestop
import ink.abb.pogo.scraper.gui.SocketServer
import ink.abb.pogo.scraper.tasks.*
import ink.abb.pogo.scraper.util.Log
import ink.abb.pogo.scraper.util.directions.RouteProviderEnum
import ink.abb.pogo.scraper.util.directions.getAltitude
import ink.abb.pogo.scraper.util.io.SettingsJSONWriter
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

class Bot(val api: PoGoApi, val settings: Settings) {

    private var runningLatch = CountDownLatch(0)
    var prepareWalkBack = AtomicBoolean(false)
    var walkBackLock = AtomicBoolean(true)
    var altitudeCache: MutableMap<String, Double> =
            try {
                @Suppress("UNCHECKED_CAST")
                (ObjectMapper().readValue(File("altitude_cache.json").readText(), MutableMap::class.java) as MutableMap<String, Double>)
            } catch (ex: Exception) {
                mutableMapOf()
            }

    lateinit private var phaser: Phaser
    var ctx = Context(
            api,
            AtomicDouble(settings.latitude),
            AtomicDouble(settings.longitude),
            AtomicLong(api.inventory.playerStats.experience),
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

        if (settings.saveLocationOnShutdown && settings.savedLatitude != 0.0 && settings.savedLongitude != 0.0) {
            ctx.lat.set(settings.savedLatitude)
            ctx.lng.set(settings.savedLongitude)
        }

        ctx.walking.set(false)

        Log.normal("Name: ${api.playerData.username} - Team: ${api.playerData.team.name}")
        Log.normal("Level ${api.inventory.playerStats.level} - " +
                "Experience ${api.inventory.playerStats.experience}; " +
                "Pokecoin: ${api.inventory.currencies.getOrPut("POKECOIN", { AtomicInteger(0) }).get()}")
        Log.normal("Pokebank ${api.inventory.pokemon.size + api.inventory.eggs.size}/${api.playerData.maxPokemonStorage}; " +
                "Stardust: ${api.inventory.currencies.getOrPut("STARDUST", { AtomicInteger(0) }).get()}; " +
                "Inventory ${api.inventory.size}/${api.playerData.maxItemStorage}")

        val compareName = Comparator<BagPokemon> { a, b ->
            a.pokemonData.pokemonId.name.compareTo(b.pokemonData.pokemonId.name)
        }
        val compareIv = Comparator<BagPokemon> { a, b ->
            // compare b to a to get it descending
            if (settings.sortByIv) {
                b.pokemonData.getIv().compareTo(a.pokemonData.getIv())
            } else {
                b.pokemonData.cp.compareTo(a.pokemonData.cp)
            }
        }
        api.inventory.pokemon.map { it.value }.sortedWith(compareName.thenComparing(compareIv)).map {
            val pnickname = if (!it.pokemonData.nickname.isEmpty()) " (${it.pokemonData.nickname})" else ""
            "Have ${it.pokemonData.pokemonId.name}$pnickname with ${it.pokemonData.cp} CP and IV  (${it.pokemonData.individualAttack}-${it.pokemonData.individualDefense}-${it.pokemonData.individualStamina}) ${it.pokemonData.getIvPercentage()}%"
        }.forEach { Log.normal(it) }

        val keepalive = GetMapRandomDirection()
        val drop = DropUselessItems()
        val profile = UpdateProfile()
        val catch = CatchOneNearbyPokemon()
        val buddy = SetBuddyPokemon()
        val release = ReleasePokemon()
        val evolve = EvolvePokemon()
        val hatchEggs = HatchEggs()
        val export = Export()

        if (settings.export.length > 0)
            task(export)

        task(keepalive)
        Log.normal("Getting initial pokestops...")

        while (api.map.getPokestops(api.latitude, api.longitude, settings.initialMapSize).size == 0) {
            Thread.sleep(1000)
        }

        val process = ProcessPokestops(api.map.getPokestops(api.latitude, api.longitude, settings.initialMapSize))

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
            if (settings.buddyPokemon.isNotBlank()) {
                task(buddy)
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
            if (checkForPlannedStop()) {
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

        val settingsJSONWriter = SettingsJSONWriter()
        settingsJSONWriter.save(settings)

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

    fun checkForPlannedStop(): Boolean {
        val timeDiff: Long = ChronoUnit.MINUTES.between(ctx.startTime, LocalDateTime.now())
        val pokemonCatched: Int = ctx.pokemonStats.first.get()
        val pokestopsVisited: Int = ctx.pokestops.get()
        //Log.red("time: ${timeDiff}, pokemon: ${pokemonCatched}, pokestops: ${pokestopsVisited}")
        if (settings.botTimeoutAfterMinutes <= timeDiff && settings.botTimeoutAfterMinutes != -1) {
            Log.red("Bot timed out as declared in the settings (after ${settings.botTimeoutAfterMinutes} minutes)")
            return true
        } else if (settings.botTimeoutAfterCatchingPokemon <= pokemonCatched && settings.botTimeoutAfterCatchingPokemon != -1) {
            Log.red("Bot timed out as declared in the settings (after catching ${settings.botTimeoutAfterCatchingPokemon} pokemon)")
            return true
        } else if (settings.botTimeoutAfterVisitingPokestops <= pokestopsVisited && settings.botTimeoutAfterVisitingPokestops != -1) {
            Log.red("Bot timed out as declared in the settings (after visiting ${settings.botTimeoutAfterVisitingPokestops} pokestops)")
            return true
        }
        return false
    }
}
