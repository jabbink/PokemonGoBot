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
import com.pokegoapi.api.player.PlayerProfile
import com.pokegoapi.api.pokemon.Pokemon
import ink.abb.pogo.scraper.*
import ink.abb.pogo.scraper.tasks.*
import ink.abb.pogo.scraper.util.Log
import ink.abb.pogo.scraper.util.Helper
import ink.abb.pogo.scraper.util.inventory.size
import ink.abb.pogo.scraper.util.pokemon.getIv
import ink.abb.pogo.scraper.util.pokemon.getIvPercentage
import ink.abb.pogo.scraper.util.pokemon.getStatsFormatted
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.fixedRateTimer
import kotlin.concurrent.thread
import java.util.concurrent.TimeUnit
import com.pokegoapi.exceptions.LoginFailedException

class Bot(var api: PokemonGo, val settings: Settings) {

    var ctx = Context(
            api,
            api.playerProfile,
            AtomicDouble(settings.startingLatitude),
            AtomicDouble(settings.startingLongitude),
            AtomicLong(api.playerProfile.stats.experience),
            Pair(AtomicInteger(0), AtomicInteger(0)),
            Pair(AtomicInteger(0), AtomicInteger(0)),
            mutableSetOf()            
    )

    @Synchronized
    fun start() {

        Log.normal();
        Log.normal("Name: ${ctx.profile.username}")
        Log.normal("Team: ${ctx.profile.team}")
        Log.normal("Pokecoin: ${ctx.profile.currencies.get(PlayerProfile.Currency.POKECOIN)}")
        Log.normal("Stardust: ${ctx.profile.currencies.get(PlayerProfile.Currency.STARDUST)}")
        Log.normal("Level ${ctx.profile.stats.level}, Experience ${ctx.profile.stats.experience}")
        Log.normal("Pokebank ${ctx.api.inventories.pokebank.pokemons.size}/${ctx.profile.pokemonStorage}")
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
        // TODO: Figure out why pokestops are only showing up the first time api.map.mapObjects is called (???)
        val reply = api.map.mapObjects
        Log.normal("Got ${reply.pokestops.size} pokestops")
        val process = ProcessPokestops(reply.pokestops)


        // BotLoop 1
        thread(true, false, null, "BotLoop1", 1, block = {
            var threadRun = true

            while(threadRun) {

                // keepalive
                task(keepalive)

                // process
                task(process)

                TimeUnit.SECONDS.sleep(Helper.getRandomNumber(4,7).toLong())
            }
        })

        // BotLoop 2
        thread(true, false, null, "BotLoop2", 1, block = {
            var threadRun = true

            while(threadRun) {

                synctask(profile)
                synctask(hatchEggs)

                TimeUnit.SECONDS.sleep(Helper.getRandomNumber(50,300).toLong())
            }
        })

        // BotLoop 3
        thread(true, false, null, "BotLoop3", 1, block = {
            var threadRun = true

            while(threadRun) {
                // catch pokemon
                if (settings.shouldCatchPokemons) {
                    synctask(catch)
                }

                // transfer pokemon
                if (settings.shouldAutoTransfer) {                            
                    synctask(release)
                }

                // drop items
                if (settings.shouldDropItems) {
                    synctask(drop)
                }                                

                TimeUnit.SECONDS.sleep(Helper.getRandomNumber(3,10).toLong())
            }

        })

    }

    @Suppress("UNUSED_VARIABLE")
    fun synctask(task: Task) {
        synchronized(ctx) {
            synchronized(settings) {

                try {            
                    task.run(this, ctx, settings)
                } catch (lfe: LoginFailedException) {

                    lfe.printStackTrace()

                    val (api2, auth) = login()

                    synchronized(ctx) {
                        ctx.api = api2
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    @Suppress("UNUSED_VARIABLE")
    fun task(task: Task) {
        try {
            task.run(this, ctx, settings)
        } catch (lfe: LoginFailedException) {

            lfe.printStackTrace()

            val (api2, auth) = login()

            synchronized(ctx) {
                ctx.api = api2
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }       
    }

    @Synchronized
    fun stop() {
        // do something

        Log.red("Stopping bot loops...")
        Log.red("All bot loops stopped.")
    }
}