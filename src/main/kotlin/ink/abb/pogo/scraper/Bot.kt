/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper

import com.google.common.util.concurrent.AtomicDouble
import com.pokegoapi.api.PokemonGo
import com.pokegoapi.api.map.MapObjects
import com.pokegoapi.api.map.fort.Pokestop
import com.pokegoapi.api.player.PlayerProfile
import com.pokegoapi.api.pokemon.Pokemon
import ink.abb.pogo.scraper.gui.SocketServer
import ink.abb.pogo.scraper.tasks.*
import ink.abb.pogo.scraper.util.Log
import ink.abb.pogo.scraper.util.cachedInventories
import ink.abb.pogo.scraper.util.inventory.size
import ink.abb.pogo.scraper.util.pokemon.getIv
import ink.abb.pogo.scraper.util.pokemon.getIvPercentage
import ink.abb.pogo.scraper.util.pokemon.getStatsFormatted
import java.time.LocalDateTime
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
            Pair(AtomicInteger(0), AtomicInteger(0)),
            mutableSetOf(),
            SocketServer(),
            Pair(AtomicBoolean(settings.catchPokemon), AtomicBoolean(false))
    )

    @Synchronized
    fun start() {
        if (isRunning()) return
        ctx.walking.set(false)

        Log.normal()
        Log.normal("Name: ${ctx.profile.playerData.username}")
        Log.normal("Team: ${ctx.profile.playerData.team.name}")
        Log.normal("Pokecoin: ${ctx.profile.currencies.get(PlayerProfile.Currency.POKECOIN)}")
        Log.normal("Stardust: ${ctx.profile.currencies.get(PlayerProfile.Currency.STARDUST)}")
        Log.normal("Level ${ctx.profile.stats.level}, Experience ${ctx.profile.stats.experience}")
        Log.normal("Pokebank ${ctx.api.cachedInventories.pokebank.pokemons.size + ctx.api.inventories.hatchery.eggs.size}/${ctx.profile.playerData.maxPokemonStorage}")
        Log.normal("Inventory ${ctx.api.cachedInventories.itemBag.size()}/${ctx.profile.playerData.maxItemStorage}")
        //Log.normal("Inventory bag ${ctx.api.bag}")

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
            val IV = it.getIvPercentage()
            "Have ${it.pokemonId.name} (${it.nickname}) with ${it.cp} CP and IV $IV% \r\n ${it.getStatsFormatted()}"
        }.forEach { Log.normal(it) }

        val keepalive = GetMapRandomDirection(isForSniping=false)
        val drop = DropUselessItems()
        val profile = UpdateProfile()
        val catch = CatchOneNearbyPokemon()
        val release = ReleasePokemon()
        val evolve = EvolvePokemon()
        val hatchEggs = HatchEggs()
        val export = Export()
        val sniper = SnipeListener()

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
            Log.red("Too high initialMapSize (${originalInitialMapSize}) found, " +
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
            if (settings.catchPokemon) {
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
        }

        task(sniper)

        Log.setContext(ctx)

        if (settings.guiPortSocket > 0) {
            Log.normal("Running socket server on port ${settings.guiPortSocket}")
            ctx.server.start(ctx, settings.guiPortSocket)
            /*var needPort = ""
            if (settings.guiPortSocket != 8001) {
                needPort = "#localhost:${settings.guiPortSocket}"
            }*/
            Log.green("Open the map on http://pogo.abb.ink/RocketTheme/")
        }

        if (settings.snipingPort > 0) {
            Log.normal("Listening for snipe info on port ${settings.snipingPort}")
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
}
