/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper.tasks

import POGOProtos.Networking.Responses.ReleasePokemonResponseOuterClass.ReleasePokemonResponse.Result
import ink.abb.pogo.scraper.Bot
import ink.abb.pogo.scraper.Context
import ink.abb.pogo.scraper.Settings
import ink.abb.pogo.scraper.Task
import ink.abb.pogo.scraper.util.Log
import ink.abb.pogo.scraper.util.cachedInventories
import ink.abb.pogo.scraper.util.pokemon.getIv
import ink.abb.pogo.scraper.util.pokemon.getIvPercentage
import ink.abb.pogo.scraper.util.pokemon.shouldTransfer

class ReleasePokemon : Task {
    override fun run(bot: Bot, ctx: Context, settings: Settings) {
        val pokemons = ctx.api.cachedInventories.pokebank.pokemons ?: return
        // prevent concurrent modification exception
        val groupedPokemon = pokemons.groupBy { it.pokemonId }
        val sortByIV = settings.sortByIv
        val pokemonCounts = hashMapOf<String, Int>()

        groupedPokemon.forEach {
            val sorted = if (sortByIV) {
                it.value.sortedByDescending { it.getIv() }
            } else {
                it.value.sortedByDescending { it.cp }
            }
            for ((index, pokemon) in sorted.withIndex()) {
                // don't drop favorited, deployed, or nicknamed pokemon
                val isFavourite = pokemon.nickname.isNotBlank() || pokemon.isFavorite || !pokemon.deployedFortId.isEmpty()
                if (!isFavourite) {
                    val ivPercentage = pokemon.getIvPercentage()
                    // never transfer highest rated Pokemon (except for obligatory transfer)
                    if (settings.obligatoryTransfer.contains(pokemon.pokemonId) || index >= settings.keepPokemonAmount) {
                        val (shouldRelease, reason) = pokemon.shouldTransfer(settings, pokemonCounts)

                        if (shouldRelease) {
                            Log.yellow("Going to transfer ${pokemon.pokemonId.name} with " +
                                    "CP ${pokemon.cp} and IV $ivPercentage%; reason: $reason")
                            val result = pokemon.transferPokemon()

                            if(settings.autotransferTimeDelay != (-1).toLong()){
                                val transferWaitTime = settings.autotransferTimeDelay/2 + (Math.random()*settings.autotransferTimeDelay).toLong()
                                Thread.sleep(transferWaitTime)
                            }


                            if (result == Result.SUCCESS) {
                                if (ctx.pokemonInventoryFullStatus.get()) {
                                    // Just released a pokemon so the inventory is not full anymore
                                    ctx.pokemonInventoryFullStatus.set(false)
                                    if (settings.catchPokemon)
                                        Log.green("Inventory freed, enabling catching of pokemon")
                                }
                                ctx.pokemonStats.second.andIncrement
                                ctx.server.releasePokemon(pokemon.id)
                                ctx.server.sendProfile()
                            } else {
                                Log.red("Failed to transfer ${pokemon.pokemonId.name}: ${result.name}")
                            }
                        }
                    }
                }
            }
        }
    }
}
