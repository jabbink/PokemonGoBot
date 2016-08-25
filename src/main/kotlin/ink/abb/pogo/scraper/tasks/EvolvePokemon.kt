/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper.tasks

import com.pokegoapi.api.pokemon.PokemonMetaRegistry
import ink.abb.pogo.scraper.Bot
import ink.abb.pogo.scraper.Context
import ink.abb.pogo.scraper.Settings
import ink.abb.pogo.scraper.Task
import ink.abb.pogo.scraper.util.Log
import ink.abb.pogo.scraper.util.cachedInventories
import ink.abb.pogo.scraper.util.pokemon.getIvPercentage

class EvolvePokemon : Task {
    override fun run(bot: Bot, ctx: Context, settings: Settings) {
        //count the current stack of possible evolves
        var countEvolveStack = 0
        val groupedPokemonForCount = ctx.api.inventories.pokebank.pokemons.groupBy { it.pokemonId }
        groupedPokemonForCount.forEach {
            if (settings.evolveBeforeTransfer.contains(it.key)) {
                // Get pokemonFamily meta information
                val pokemonMeta = PokemonMetaRegistry.getMeta(it.key)
                var maxPossibleEvolves: Int = 0

                if (pokemonMeta.candyToEvolve > 0) {
                    maxPossibleEvolves = bot.api.inventories.candyjar.getCandies(pokemonMeta.family) / pokemonMeta.candyToEvolve
                } else {
                    Log.red("${it.key} is in evolve list but is unevolvable")
                }

                // Add the minimum value, depending on which is the bottleneck, amount of candy, or pokemon of this type in pokebank:
                countEvolveStack += Math.min(maxPossibleEvolves, it.value.count())
            }
        }
        Log.yellow("Stack of pokemon ready to evolve: $countEvolveStack/${settings.evolveStackLimit}")

        // use lucky egg if above evolve stack limit and evolve the whole stack
        if (countEvolveStack >= settings.evolveStackLimit) {
            Log.yellow("Starting stack evolve of $countEvolveStack pokemon using lucky egg")
            ctx.pauseWalking.set(true)
            val startingXP = ctx.api.playerProfile.stats.experience
            if (settings.useLuckyEgg == 1) {
                try {
                    val resultLuckyEgg = ctx.api.cachedInventories.itemBag.useLuckyEgg()
                    Log.yellow("Result of using lucky egg: ${resultLuckyEgg.result.toString()}")
                } catch (exc: Exception) {
                    Log.red("Lucky egg usage failed! Will continue evolving stack without one.")
                }
            } else {
                Log.yellow("Starting stack evolve of $countEvolveStack pokemon without lucky egg")
            }
            var countEvolved = 0
            ctx.api.inventories.pokebank.pokemons.forEach {
                if (settings.evolveBeforeTransfer.contains(it.pokemonId)) {
                    val pokemonMeta = PokemonMetaRegistry.getMeta(it.pokemonId)
                    if (bot.api.inventories.candyjar.getCandies(pokemonMeta.family) >= pokemonMeta.candyToEvolve) {
                        Log.yellow("Evolving ${it.pokemonId.name} CP ${it.cp} IV ${it.getIvPercentage()}%")
                        val evolveResult = it.evolve()
                        if( settings.evolveTimeDelay > 300){
                            Thread.sleep(settings.evolveTimeDelay/2 + (Math.random()*settings.evolveTimeDelay).toLong())
                        } else {
                            Thread.sleep(300)
                        }
                        if (evolveResult.isSuccessful) {
                            countEvolved++
                            val evolvedpokemon = evolveResult.evolvedPokemon
                            Log.yellow("Successfully evolved in ${evolvedpokemon.pokemonId.name} CP ${evolvedpokemon.cp} IV ${evolvedpokemon.getIvPercentage()}%")
                            ctx.server.releasePokemon(it.id)
                            //TODO: comunicate to the sockserver the new got pokemon
                            //ctx.server.newPokemon(0.0, 0.0, PokemonDataOuterClass.PokemonData.buildFromPokemon(evolvedpokemon))
                            ctx.api.inventories.updateInventories(true)
                            Thread.sleep(300)
                        } else {
                            Log.red("Evolve of ${it.pokemonId.name} CP ${it.cp} IV ${it.getIvPercentage()}% failed: ${evolveResult.result.toString()}")
                        }
                    } else {
                        Log.red("Not enough candy (${bot.api.inventories.candyjar.getCandies(pokemonMeta.family)}/${pokemonMeta.candyToEvolve}) to evolve ${it.pokemonId.name} CP ${it.cp} IV ${it.getIvPercentage()}%")
                    }
                }
            }
            val endXP = ctx.api.playerProfile.stats.experience
            ctx.pauseWalking.set(false)
            Log.yellow("Finished evolving $countEvolved pokemon; ${endXP - startingXP} xp gained")
        }
    }
}