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
import ink.abb.pogo.scraper.util.pokemon.getIv
import ink.abb.pogo.scraper.util.pokemon.getIvPercentage
import ink.abb.pogo.scraper.util.pokemon.shouldTransfer

class ReleasePokemon : Task {
    override fun run(bot: Bot, ctx: Context, settings: Settings) {
        val groupedPokemon = ctx.api.inventories.pokebank.pokemons.groupBy { it.pokemonId }
        val sortByIV = settings.sortByIV
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

                            if (settings.obligatoryTransfer.contains(pokemon.pokemonId)) { //Obligatory = Evolve bcs I dont mind candies
                                Log.yellow("Going to evolve ${pokemon.pokemonId.name} with " +
                                        "CP ${pokemon.cp} and IV $ivPercentage%; reason: $reason")
                                val evolveResult = pokemon.evolve()
                                if (evolveResult.isSuccessful()) {
                                    var evolvedPkmn = evolveResult.getEvolvedPokemon();
                                    Log.yellow("${pokemon.pokemonId.name} has evolved to ${evolvedPkmn.pokemonId.name}")

                                    val (shouldReleaseEvolved, reasonshouldReleaseEvolved) = evolvedPkmn.shouldTransfer(settings, pokemonCounts)
                                    val ivPercentage = evolvedPkmn.getIvPercentage()


                                    if (shouldReleaseEvolved) {
                                        Log.yellow("Going to transfer evolved ${evolvedPkmn.pokemonId.name} with " +
                                                "CP ${evolvedPkmn.cp} and IV $ivPercentage%; reason: $reasonshouldReleaseEvolved")
                                        val result = evolvedPkmn.transferPokemon()
                                        if (result == Result.SUCCESS) {
                                            ctx.pokemonStats.second.andIncrement
                                            ctx.server.releasePokemon(evolvedPkmn.id)
                                            ctx.server.sendProfile()
                                        } else {
                                            Log.red("Failed to transfer evolved ${evolvedPkmn.pokemonId.name}: ${result.name}")
                                        }
                                    } else {
                                        Log.yellow("Going to keep evolved ${evolvedPkmn.pokemonId.name} with " +
                                                "CP ${evolvedPkmn.cp} and IV $ivPercentage%; reason: $reasonshouldReleaseEvolved")
                                    }

                                } else {
                                    Log.red("Cannot evolve ${pokemon.pokemonId.name}, going to transfer")

                                    val result = pokemon.transferPokemon()
                                    if (result == Result.SUCCESS) {
                                        ctx.pokemonStats.second.andIncrement
                                        ctx.server.releasePokemon(pokemon.id)
                                        ctx.server.sendProfile()
                                    } else {
                                        Log.red("Failed to transfer ${pokemon.pokemonId.name}: ${result.name}")
                                    }
                                }
                            } else {
                                val result = pokemon.transferPokemon()
                                if (result == Result.SUCCESS) {
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
}
