/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper.evolve

import POGOProtos.Networking.Responses.ReleasePokemonResponseOuterClass
import com.pokegoapi.api.pokemon.Pokemon
import com.pokegoapi.api.pokemon.PokemonMetaRegistry
import ink.abb.pogo.scraper.Bot
import ink.abb.pogo.scraper.Context
import ink.abb.pogo.scraper.Settings
import ink.abb.pogo.scraper.Task
import ink.abb.pogo.scraper.util.Log
import ink.abb.pogo.scraper.util.cachedInventories
import ink.abb.pogo.scraper.util.pokemon.getIv
import ink.abb.pogo.scraper.util.pokemon.getIvPercentage

class XpBatchStrategy : EvolutionStrategy {
    override fun evolve(bot: Bot, ctx: Context, settings: Settings) {
        //count the current stack of possible evolves
        var countEvolveStack = 0
        val groupedPokemonForCount = ctx.api.inventories.pokebank.pokemons.groupBy { it.pokemonId }
        groupedPokemonForCount.forEach {
            if (settings.evolveBeforeTransfer.contains(it.key)){
                // Get pokemonFamily meta information
                val pokemonMeta = PokemonMetaRegistry.getMeta(it.key)
                var maxPossibleEvolves = bot.api.inventories.candyjar.getCandies(pokemonMeta.family) / pokemonMeta.candyToEvolve
                // Add the minimum value, depending on which is the bottleneck, amount of candy, or pokemon of this type in pokebank:
                countEvolveStack +=  Math.min(maxPossibleEvolves,it.value.count())

                // Use Iv sorting if this is configured:
                val sorted = if (settings.sortByIv) {
                    it.value.sortedByDescending { it.getIv() }
                } else {
                    it.value.sortedByDescending { it.cp }
                }
                // Release pokemon we wont be able to evolve anyway, because of lack of candy:
                sorted.forEach {
                    maxPossibleEvolves--
                    if (maxPossibleEvolves < 0) {
                        releasePokemon(it,"Not enough candy to save for evolve",ctx)
                    }
                }

            }
        }
        Log.yellow("Stack of pokemon ready to evolve: $countEvolveStack pokemons")

        // use lucky egg if above evolve stack limit and evolve the whole stack
        if (countEvolveStack >= settings.evolveStackLimit) {
            val startingXP = ctx.api.playerProfile.stats.experience
            if (settings.useLuckyEgg == 1) {
                Log.yellow("Using Lucky Egg before evolving stack of $countEvolveStack pokemons")
                try {
                    var resultLuckyEgg = ctx.api.cachedInventories.itemBag.useLuckyEgg()
                    Log.yellow("Result of using lucky egg: ${resultLuckyEgg.result.toString()}")
                } catch (exc: Exception) {
                    Log.red("Lucky egg usage failed! Will continue evolving stack without one.")
                }
            }
            else {
                Log.yellow("Not using lucky egg")
            }
            var countEvolved = 0
            ctx.api.inventories.pokebank.pokemons.forEach {
                if (settings.evolveBeforeTransfer.contains(it.pokemonId)) {
                    Log.yellow("Evolving ${it.pokemonId.name} before transfer")
                    val evolveResult = it.evolve()
                    Thread.sleep(300)
                    if (evolveResult.isSuccessful()) {
                        countEvolved++
                        releasePokemon(evolveResult.getEvolvedPokemon(),"Evolved for XP only",ctx)
                        Thread.sleep(300)
                    }
                }
            }
            val endXP = ctx.api.playerProfile.stats.experience
            Log.yellow("Finished evolving $countEvolved pokemon; ${endXP-startingXP} xp gained")
        }
        //
    }

    fun releasePokemon(pokemon: Pokemon, reason: String, ctx: Context) {
        Log.yellow("Going to transfer ${pokemon.pokemonId.name} with " +
                "CP ${pokemon.cp} and IV ${pokemon.getIvPercentage()}%; reason: $reason")
        val result = pokemon.transferPokemon()
        if (result == ReleasePokemonResponseOuterClass.ReleasePokemonResponse.Result.SUCCESS) {
            ctx.pokemonStats.second.andIncrement
            ctx.server.releasePokemon(pokemon.id)
            ctx.server.sendProfile()
        } else {
            Log.red("Failed to transfer ${pokemon.pokemonId.name}: ${result.name}")
        }
    }
}