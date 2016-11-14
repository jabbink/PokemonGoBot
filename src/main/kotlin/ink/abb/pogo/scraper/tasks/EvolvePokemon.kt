/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper.tasks

import POGOProtos.Inventory.Item.ItemIdOuterClass
import POGOProtos.Inventory.Item.ItemTypeOuterClass
import POGOProtos.Networking.Responses.EvolvePokemonResponseOuterClass
import ink.abb.pogo.api.request.EvolvePokemon
import ink.abb.pogo.api.request.UseItemXpBoost
import ink.abb.pogo.api.util.PokemonMetaRegistry
import ink.abb.pogo.scraper.Bot
import ink.abb.pogo.scraper.Context
import ink.abb.pogo.scraper.Settings
import ink.abb.pogo.scraper.Task
import ink.abb.pogo.scraper.util.Log
import ink.abb.pogo.scraper.util.pokemon.getIvPercentage
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
            val startingXP = ctx.api.inventory.playerStats.experience
            ctx.pauseWalking.set(true)
            if (settings.useLuckyEgg == 1) {
                Log.yellow("Starting stack evolve of $countEvolveStack pokemon using lucky egg")
                val activeEgg = bot.api.inventory.appliedItems.find { it.itemType == ItemTypeOuterClass.ItemType.ITEM_TYPE_XP_BOOST }
                if (activeEgg != null) {
                    Log.green("Already have an active egg")
                } else {
                    val luckyEgg = UseItemXpBoost().withItemId(ItemIdOuterClass.ItemId.ITEM_LUCKY_EGG)
                    val result = ctx.api.queueRequest(luckyEgg).toBlocking().first().response
                    Log.yellow("Result of using lucky egg: ${result.result.toString()}")
                }
            } else {
                Log.yellow("Starting stack evolve of $countEvolveStack pokemon without lucky egg")
            }
            var countEvolved = 0
            ctx.api.inventory.pokemon.forEach {
                if (settings.evolveBeforeTransfer.contains(it.value.pokemonData.pokemonId)) {
                    val pokemonMeta = PokemonMetaRegistry.getMeta(it.value.pokemonData.pokemonId)
                    if (bot.api.inventory.candies.getOrPut(pokemonMeta.family, { AtomicInteger(0) }).get() >= pokemonMeta.candyToEvolve) {
                        val pokemonData = it.value.pokemonData
                        Log.yellow("Evolving ${pokemonData.pokemonId.name} CP ${pokemonData.cp} IV ${pokemonData.getIvPercentage()}%")
                        val evolve = EvolvePokemon().withPokemonId(it.key)
                        val evolveResult = ctx.api.queueRequest(evolve).toBlocking().first().response
                        if (evolveResult.result == EvolvePokemonResponseOuterClass.EvolvePokemonResponse.Result.SUCCESS) {
                            countEvolved++
                            val evolvedPokemon = evolveResult.evolvedPokemonData
                            Log.yellow("Successfully evolved in ${evolvedPokemon.pokemonId.name} CP ${evolvedPokemon.cp} IV ${evolvedPokemon.getIvPercentage()}%")
                            ctx.server.releasePokemon(pokemonData.id)
                        } else {
                            Log.red("Evolve of ${pokemonData.pokemonId.name} CP ${pokemonData.cp} IV ${pokemonData.getIvPercentage()}% failed: ${evolveResult.result.toString()}")
                        }

                    } else {
                        Log.red("Not enough candy (${bot.api.inventory.candies.getOrPut(pokemonMeta.family, { AtomicInteger(0) }).get()}/${pokemonMeta.candyToEvolve}) to evolve ${it.value.pokemonData.pokemonId.name} CP ${it.value.pokemonData.cp} IV ${it.value.pokemonData.getIvPercentage()}%")
                    }
                }
            }
            val endXP = ctx.api.inventory.playerStats.experience
            ctx.pauseWalking.set(false)
            Log.yellow("Finished evolving $countEvolved pokemon; ${endXP - startingXP} xp gained")
        }
    }
}
