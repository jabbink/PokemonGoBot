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
import kotlin.concurrent.fixedRateTimer
import kotlin.concurrent.thread

class Bot(val api: PokemonGo, val settings: Settings) {

    var ctx = Context(
            api,
            api.playerProfile,
            AtomicDouble(settings.startingLatitude),
            AtomicDouble(settings.startingLongitude)
    )

    fun run() {

        println()
        println("Name: ${ctx.profile.username}")
        println("Team: ${ctx.profile.team}")
        println("Pokecoin: ${ctx.profile.currencies.get(PlayerProfile.Currency.POKECOIN)}")
        println("Stardust: ${ctx.profile.currencies.get(PlayerProfile.Currency.STARDUST)}")
        println("Level ${ctx.profile.stats.level}, Experience ${ctx.profile.stats.experience}")
        println()

        api.pokebank.pokemons.map { "Have ${it.pokemonId.name} (${it.nickname}) with ${it.cp} CP" }.forEach { println(it) }

        val keepalive = GetMapRandomDirection()
        val drop = DropUselessItems()
        val profile = UpdateProfile()
        val catch = CatchOneNearbyPokemon()
        val release = ReleasePokemon()

        task(keepalive)
        println("Getting initial pokestops...")
        // TODO: Figure out why pokestops are only showing up the first time api.map.mapObjects is called (???)
        val reply = api.map.mapObjects
        val process = ProcessPokestops(reply.pokestops)

        fixedRateTimer("BotLoop", false, 0, 5000, action = {
            thread(block = {
                task(keepalive)
                task(catch)
                task(drop)
                task(process)
                task(release)
                task(profile)
            })
        })
    }

    fun task(task: Task) {
        task.run(this, ctx, settings)
    }
}