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
import ink.abb.pogo.scraper.tasks.*
import ink.abb.pogo.scraper.util.pokemon.getIvPercentage
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.fixedRateTimer
import kotlin.concurrent.thread

class Bot(val api: PokemonGo, val settings: Settings) {

    var ctx = Context(
            api,
            api.playerProfile,
            AtomicDouble(settings.startingCoordinate.first()),
            AtomicDouble(settings.startingCoordinate.last())
    )

    fun run() {

        println()
        println("Name: ${ctx.profile.username}")
        println("Team: ${ctx.profile.team}")
        println("Pokecoin: ${ctx.profile.currencies.get(PlayerProfile.Currency.POKECOIN)}")
        println("Stardust: ${ctx.profile.currencies.get(PlayerProfile.Currency.STARDUST)}")
        println("Level ${ctx.profile.stats.level}, Experience ${ctx.profile.stats.experience}")
        println("Pokebank ${ctx.api.inventories.pokebank.pokemons.size}/${ctx.profile.pokemonStorage}")
        //println("Inventory bag ${ctx.api.bag}")

        api.inventories.pokebank.pokemons.map {
            val IV = it.getIvPercentage()
            "Have ${it.pokemonId.name} (${it.nickname}) with ${it.cp} CP and IV $IV%"
        }.forEach { println(it) }

        val keepalive = GetMapRandomDirection()
        val drop = DropUselessItems()
        val profile = UpdateProfile()
        val catch = CatchOneNearbyPokemon()
        val release = ReleasePokemon()

        task(keepalive)
        println("Getting initial pokestops...")
        // TODO: Figure out why pokestops are only showing up the first time api.map.mapObjects is called (???)
        val width = 9;
        val reply = api.map.getMapObjects(width)
        println("Found ${reply.pokestops.size} number of pokestops within width: ${width} ")
        val process = ProcessPokestops(reply.pokestops)

        fixedRateTimer("ProfileLoop", false, 0, 60000, action = {
            thread(block = {
                task(profile)
            })
        })

        fixedRateTimer("BotLoop", false, 0, 5000, action = {
            thread(block = {
                task(keepalive)
                task(catch)
                task(drop)
                task(process)
                task(release)
            })
        })
    }

    fun task(task: Task) {
        task.run(this, ctx, settings)
    }
}