/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper.tasks

import POGOProtos.Networking.Responses.ReleasePokemonResponseOuterClass.ReleasePokemonResponse.Result
import com.pokegoapi.api.pokemon.Pokemon
import ink.abb.pogo.scraper.Bot
import ink.abb.pogo.scraper.Context
import ink.abb.pogo.scraper.Settings
import ink.abb.pogo.scraper.Task
import ink.abb.pogo.scraper.util.Log
import ink.abb.pogo.scraper.util.Helper
import ink.abb.pogo.scraper.util.pokemon.getIv
import ink.abb.pogo.scraper.util.pokemon.getIvPercentage
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean
import ink.abb.pogo.scraper.util.pokemon.shouldTransfer

class ReleasePokemon : Task {
    override fun run(bot: Bot, ctx: Context, settings: Settings) {
        val groupedPokemon = ctx.api.inventories.pokebank.pokemons.groupBy { it.pokemonId }
        val sortByIV = settings.sortByIV

        // there is still a pokemon release process
        if (!ctx.releasing.compareAndSet(false, true)) {
            // Log.red("There is still pokemon transfer process. skipping..")
            return;
        }

        else {

            groupedPokemon.forEach {
                val sorted = if (sortByIV) {
                    it.value.sortedByDescending { it.getIv() }
                } else {
                    it.value.sortedByDescending { it.cp }
                }

                for ((index, pokemon) in sorted.withIndex()) {
                    // don't drop favourited or nicknamed pokemon
                    val isFavourite = pokemon.nickname.isNotBlank() || pokemon.favorite || !pokemon.deployedFortId.isEmpty()
                    if (!isFavourite) {
                        val ivPercentage = pokemon.getIvPercentage()
                        // never transfer highest rated Pokemon (except for obligatory transfer)
                        if (settings.obligatoryTransfer.contains(pokemon.pokemonId.name) || index >= settings.keepPokemonAmount) {

                                val (shouldRelease, reason) = pokemon.shouldTransfer(settings)

                                if (shouldRelease) {                                

                                    ctx.releasing.getAndSet(true)
                                    
                                    Log.yellow("Going to transfer ${pokemon.pokemonId.name} with " +
                                            "CP ${pokemon.cp} and IV $ivPercentage%; reason: $reason")

                                    // wait for random seconds
                                    val sleeptime = Helper.getRandomNumber(10,30)
                                    TimeUnit.SECONDS.sleep(sleeptime.toLong())
                                    Log.normal("Waited for $sleeptime seconds before transfering pokemon.")

                                    val result = pokemon.transferPokemon()
                                    if (result == Result.SUCCESS) {
                                        ctx.pokemonStats.second.andIncrement
                                        Log.green("[ReleasePokemon] Pokemon ${pokemon.pokemonId.name} successfully transfered!" +
                                                " -- (CP ${pokemon.cp} and IV $ivPercentage%)")
                                    } else {
                                        Log.red("Failed to transfer ${pokemon.pokemonId.name}: ${result.name}")
                                    }

                                    ctx.releasing.getAndSet(false)
                                }                            
                        }
                    }
                }                
            }
        }

        ctx.releasing.getAndSet(false)
    }
}
