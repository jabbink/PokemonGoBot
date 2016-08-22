/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper.tasks

import POGOProtos.Data.PokemonDataOuterClass
import POGOProtos.Inventory.Item.ItemIdOuterClass
import POGOProtos.Networking.Responses.EvolvePokemonResponseOuterClass
import POGOProtos.Networking.Responses.ReleasePokemonResponseOuterClass
import ink.abb.pogo.api.request.EvolvePokemon
import ink.abb.pogo.api.request.ReleasePokemon
import ink.abb.pogo.api.request.UseItemXpBoost
import ink.abb.pogo.api.util.PokemonMetaRegistry
import ink.abb.pogo.scraper.Bot
import ink.abb.pogo.scraper.Context
import ink.abb.pogo.scraper.Settings
import ink.abb.pogo.scraper.Task
import ink.abb.pogo.scraper.util.Log
import ink.abb.pogo.scraper.util.pokemon.getIv
import ink.abb.pogo.scraper.util.pokemon.getIvPercentage
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger

class EvolvePokemon : Task {
    override fun run(bot: Bot, ctx: Context, settings: Settings) {
        //count the current stack of possible evolves
        var countEvolveStack = 0
        val groupedPokemonForCount = ctx.api.inventory.pokemon.map { it.value }.groupBy { it.pokemonData.pokemonId }
        groupedPokemonForCount.forEach {
            if (settings.evolveBeforeTransfer.contains(it.key)) {
                // Get pokemonFamily meta information
                val pokemonMeta = PokemonMetaRegistry.getMeta(it.key)
                var maxPossibleEvolves: Int = 0

                if (pokemonMeta.candyToEvolve > 0) {
                    maxPossibleEvolves = bot.api.inventory.candies.getOrPut(pokemonMeta.family, { AtomicInteger(0) }).get() / pokemonMeta.candyToEvolve
                }

                // Add the minimum value, depending on which is the bottleneck, amount of candy, or pokemon of this type in pokebank:
                countEvolveStack += Math.min(maxPossibleEvolves, it.value.count())

                // Use Iv sorting if this is configured:
                val sorted = if (settings.sortByIv) {
                    it.value.sortedByDescending { it.pokemonData.getIv() }
                } else {
                    it.value.sortedByDescending { it.pokemonData.cp }
                }
                // Release pokemon we wont be able to evolve anyway, because of lack of candy:
                sorted.forEach {
                    maxPossibleEvolves--
                    if (maxPossibleEvolves < 0) {
                        releasePokemon(it.pokemonData, "Not enough candy to save for evolve", ctx)
                    }
                }

            }
        }
        Log.yellow("Stack of pokemon ready to evolve: $countEvolveStack pokemons")

        // use lucky egg if above evolve stack limit and evolve the whole stack
        if (countEvolveStack >= settings.evolveStackLimit) {
            val startingXP = ctx.api.inventory.playerStats.experience
            if (settings.useLuckyEgg == 1) {
                Log.yellow("Using Lucky Egg before evolving stack of $countEvolveStack pokemons")
                try {
                    val countDown = CountDownLatch(1)
                    val luckyEgg = UseItemXpBoost().withItemId(ItemIdOuterClass.ItemId.ITEM_LUCKY_EGG)
                    ctx.api.queueRequest(luckyEgg).subscribe {
                        countDown.countDown()
                        val result = it.response
                        Log.yellow("Result of using lucky egg: ${result.result.toString()}")
                    }
                    countDown.await()
                } catch (exc: Exception) {
                    Log.red("Lucky egg usage failed! Will continue evolving stack without one.")
                }
            } else {
                Log.yellow("Not using lucky egg")
            }
            var countEvolved = 0
            ctx.api.inventory.pokemon.forEach {
                if (settings.evolveBeforeTransfer.contains(it.value.pokemonData.pokemonId)) {
                    Log.yellow("Evolving ${it.value.pokemonData.pokemonId.name} before transfer")
                    val evolve = EvolvePokemon().withPokemonId(it.key)
                    val countDown = CountDownLatch(2)
                    ctx.api.queueRequest(evolve).subscribe {
                        countDown.countDown()
                        val evolveResult = it.response
                        if (evolveResult.result == EvolvePokemonResponseOuterClass.EvolvePokemonResponse.Result.SUCCESS) {
                            countEvolved++
                            releasePokemon(evolveResult.evolvedPokemonData, "Evolved for XP only", ctx)
                            countDown.countDown()
                        } else {
                            countDown.countDown()
                        }
                    }
                    countDown.await()
                }
            }
            val endXP = ctx.api.inventory.playerStats.experience
            Log.yellow("Finished evolving $countEvolved pokemon; ${endXP - startingXP} xp gained")
        }
        //
    }

    fun releasePokemon(pokemon: PokemonDataOuterClass.PokemonData, reason: String, ctx: Context) {
        val countDown = CountDownLatch(1)
        Log.yellow("Going to transfer ${pokemon.pokemonId.name} with " +
                "CP ${pokemon.cp} and IV ${pokemon.getIvPercentage()}%; reason: $reason")
        val transfer = ReleasePokemon().withPokemonId(pokemon.id)
        ctx.api.queueRequest(transfer).subscribe {
            countDown.countDown()
            val result = it.response
            if (result.result == ReleasePokemonResponseOuterClass.ReleasePokemonResponse.Result.SUCCESS) {
                ctx.pokemonStats.second.andIncrement
                ctx.server.releasePokemon(pokemon.id)
                ctx.server.sendProfile()
            } else {
                Log.red("Failed to transfer ${pokemon.pokemonId.name}: ${result.result}")
            }
        }
        countDown.await()
    }
}