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
import com.pokegoapi.api.player.PlayerProfile
import ink.abb.pogo.scraper.gui.SocketServer
import ink.abb.pogo.scraper.gui.WebServer
import com.pokegoapi.api.pokemon.Pokemon
import ink.abb.pogo.scraper.tasks.*
import ink.abb.pogo.scraper.util.Log
import ink.abb.pogo.scraper.util.inventory.size
import ink.abb.pogo.scraper.util.pokemon.getIv
import ink.abb.pogo.scraper.util.pokemon.getIvPercentage
import ink.abb.pogo.scraper.util.pokemon.getStatsFormatted
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Phaser
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.thread

class Bot(val api: PokemonGo, val settings: Settings) {

    private var runningLatch = CountDownLatch(0)
    lateinit private var phaser: Phaser

    var ctx = Context(
            api,
            api.playerProfile,
            AtomicDouble(settings.startingLatitude),
            AtomicDouble(settings.startingLongitude),
            AtomicLong(api.playerProfile.stats.experience),
            Pair(AtomicInteger(0), AtomicInteger(0)),
            Pair(AtomicInteger(0), AtomicInteger(0)),
            mutableSetOf(),
            SocketServer()
    )

    @Synchronized
    fun start() {
        if (isRunning()) return

        Log.normal()
        Log.normal("Name: ${ctx.profile.username}")
        Log.normal("Team: ${ctx.profile.team}")
        Log.normal("Pokecoin: ${ctx.profile.currencies.get(PlayerProfile.Currency.POKECOIN)}")
        Log.normal("Stardust: ${ctx.profile.currencies.get(PlayerProfile.Currency.STARDUST)}")
        Log.normal("Level ${ctx.profile.stats.level}, Experience ${ctx.profile.stats.experience}")
        Log.normal("Pokebank ${ctx.api.inventories.pokebank.pokemons.size + ctx.api.inventories.hatchery.eggs.size}/${ctx.profile.pokemonStorage}")
        Log.normal("Inventory ${ctx.api.inventories.itemBag.size()}/${ctx.profile.itemStorage}")
        //Log.normal("Inventory bag ${ctx.api.bag}")

        val compareName = Comparator<Pokemon> { a, b ->
            a.pokemonId.name.compareTo(b.pokemonId.name)
        }
        val compareIv = Comparator<Pokemon> { a, b ->
            // compare b to a to get it descending
            if (settings.sortByIV) {
                b.getIv().compareTo(a.getIv())
            } else {
                b.cp.compareTo(a.cp)
            }
        }
        api.inventories.pokebank.pokemons.sortedWith(compareName.thenComparing(compareIv)).map {
            val IV = it.getIvPercentage()
            "Have ${it.pokemonId.name} (${it.nickname}) with ${it.cp} CP and IV $IV% \r\n ${it.getStatsFormatted()}"
        }.forEach { Log.normal(it) }

        val keepalive = GetMapRandomDirection()
        val drop = DropUselessItems()
        val profile = UpdateProfile()
        val catch = CatchOneNearbyPokemon()
        val release = ReleasePokemon()
        val hatchEggs = HatchEggs()

        task(keepalive)
        Log.normal("Getting initial pokestops...")

        val sleepTimeout = 10L
        var reply: MapObjects?
        do {
            reply = api.map.mapObjects
            Log.normal("Got ${reply.pokestops.size} pokestops")
            if (reply == null || reply.pokestops.size == 0) {
                Log.red("Retrying in $sleepTimeout seconds...")
                Thread.sleep(sleepTimeout * 1000)
            }
        } while (reply == null || reply.pokestops.size == 0)
        val process = ProcessPokestops(reply.pokestops)

        runningLatch = CountDownLatch(1)
        phaser = Phaser(1)

        runLoop(TimeUnit.SECONDS.toMillis(settings.profileUpdateTimer), "ProfileLoop") {
            task(profile)
            task(hatchEggs)
        }
        
        runLoop(TimeUnit.SECONDS.toMillis(5), "BotLoop") {
            task(keepalive)
            if (settings.shouldCatchPokemons)
                task(catch)
            if (settings.shouldDropItems)
                task(drop)
            if (settings.shouldAutoTransfer)
                task(release)

            task(process)
        }

        Log.setContext(ctx)

        if(settings.guiPort > 0){
            Log.normal("Running webserver on port ${settings.guiPort}")
            WebServer().start(settings.guiPort, settings.guiPortSocket)
            ctx.server.start(ctx, settings.guiPortSocket)
        }
    }

    fun runLoop(timeout: Long, name: String, block: (cancel: () -> Unit) -> Unit) {
        phaser.register()
        thread(name = name) {
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

                    if(cancelled) continue

                    val sleep = timeout - (api.currentTimeMillis() - start)

                    if (sleep > 0) {
                        try {
                            runningLatch.await(sleep, TimeUnit.MILLISECONDS)
                        } catch (ignore: InterruptedException) {
                        }
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

        Log.red("Stopping bot loops...")
        runningLatch.countDown()
        phaser.arriveAndAwaitAdvance()
        Log.red("All bot loops stopped.")
    }

    fun isRunning(): Boolean {
        return runningLatch.count > 0
    }

    fun task(task: Task) {
        Thread.sleep(300)
        task.run(this, ctx, settings)
    }
}
