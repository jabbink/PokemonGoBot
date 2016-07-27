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

class ReleasePokemon : Task {
    override fun run(bot: Bot, ctx: Context, settings: Settings) {
        val groupedPokemon = ctx.api.inventories.pokebank.pokemons.groupBy { it.pokemonId }
        val ignoredPokemon = settings.ignoredPokemon
        val obligatoryTransfer = settings.obligatoryTransfer
        val minIVPercentage = settings.transferIVThreshold
        val minCP = settings.transferCPThreshold
        val sortByIV = settings.sortByIV

        // there is still a pokemon release process
        if (!ctx.releasing.compareAndSet(false, true)) {
            // Log.red("There is still pokemon transfer process. skipping..")
            return;
        }

        else {

            groupedPokemon.forEach {
                var sorted: List<Pokemon>
                if (sortByIV) {
                    sorted = it.value.sortedByDescending { it.getIv() }
                } else {
                    sorted = it.value.sortedByDescending { it.cp }
                }
                for ((index, pokemon) in sorted.withIndex()) {
                    // don't drop favourited or nicknamed pokemon
                    val isFavourite = pokemon.nickname.isNotBlank() || pokemon.favorite
                    if (!isFavourite) {
                        val ivPercentage = pokemon.getIvPercentage()
                        // never transfer highest rated Pokemon (except for obligatory transfer)
                        if (settings.obligatoryTransfer.contains(pokemon.pokemonId.name) || index >= settings.keepPokemonAmount) {
                            // stop releasing when pokemon is set in ignoredPokemon
                            if (!ignoredPokemon.contains(pokemon.pokemonId.name)) {
                                var shouldRelease = obligatoryTransfer.contains(pokemon.pokemonId.name)
                                var reason: String
                                if (shouldRelease) {
                                    reason = "Obligatory release"
                                } else {
                                    var ivTooLow = false
                                    var cpTooLow = false

                                    // never transfer > min IV percentage (unless set to -1)
                                    if (ivPercentage < minIVPercentage || minIVPercentage == -1) {
                                        ivTooLow = true
                                    }
                                    // never transfer > min CP  (unless set to -1)
                                    if (pokemon.cp < minCP || minCP == -1) {
                                        cpTooLow = true
                                    }
                                    reason = "CP < $minCP and IV < $minIVPercentage%"
                                    shouldRelease = ivTooLow && cpTooLow
                                }
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
        }

        ctx.releasing.getAndSet(false)
    }
}
