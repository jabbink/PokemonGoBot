/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper

import POGOProtos.Data.PokemonDataOuterClass
import com.google.common.util.concurrent.AtomicDouble
import ink.abb.pogo.api.PoGoApi
import ink.abb.pogo.api.cache.BagPokemon
import ink.abb.pogo.api.cache.Pokestop
import ink.abb.pogo.scraper.gui.SocketServer
import ink.abb.pogo.scraper.tasks.*
import ink.abb.pogo.scraper.util.Log
import ink.abb.pogo.scraper.util.cachedInventories
import ink.abb.pogo.scraper.util.pokemon.getIv
import ink.abb.pogo.scraper.util.pokemon.getIvPercentage
import ink.abb.pogo.scraper.util.pokemon.getStatsFormatted
import java.io.File
import java.time.LocalDateTime
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

    lateinit private var phaser: Phaser

    var ctx = Context(
            api,
            AtomicDouble(settings.latitude),
            AtomicDouble(settings.longitude),
            AtomicLong(api.inventory.playerStats.experience),
            LocalDateTime.now(),
            Pair(AtomicInteger(0), AtomicInteger(0)),
            AtomicInteger(0),
            Pair(AtomicInteger(0), AtomicInteger(0)),
            mutableSetOf(),
            SocketServer(),
            Pair(AtomicBoolean(settings.catchPokemon), AtomicBoolean(false)),
            settings.restApiPassword
    )

    @Synchronized
    fun start() {
        if (isRunning()) return
        ctx.walking.set(false)

        Log.normal()
        Log.normal("Name: ${api.playerData.username}")
        Log.normal("Team: ${api.playerData.team.name}")
        Log.normal("Pokecoin: ${api.inventory.currencies.get("POKECOIN")}")
        Log.normal("Stardust: ${api.inventory.currencies.get("STARDUST")}")
        Log.normal("Level ${api.inventory.playerStats.level}, Experience ${api.inventory.playerStats.level}")
        Log.normal("Pokebank ${api.inventory.pokemon.size + api.inventory.eggs.size}/${api.playerData.maxPokemonStorage}")
        Log.normal("Inventory ${api.inventory.items.size}/${api.playerData.maxItemStorage}")
        //Log.normal("Inventory bag ${ctx.api.bag}")

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
            val IV = it.pokemonData.getIvPercentage()
            "Have ${it.pokemonData.pokemonId.name} (${it.pokemonData.nickname}) with ${it.pokemonData.cp} CP and IV $IV% \r\n ${it.pokemonData.getStatsFormatted()}"
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

        if(settings.saveLocationOnShutdown) {
            Log.normal("Saving last location ...")
            settings.longitude = ctx.lng.get()
            settings.latitude = ctx.lat.get()
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
}
