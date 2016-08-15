/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper.tasks

import POGOProtos.Networking.Responses.CatchPokemonResponseOuterClass.CatchPokemonResponse
import POGOProtos.Networking.Responses.EncounterResponseOuterClass.EncounterResponse.Status
import com.pokegoapi.api.inventory.Pokeball
import com.pokegoapi.api.map.pokemon.encounter.DiskEncounterResult
import ink.abb.pogo.scraper.Bot
import ink.abb.pogo.scraper.Context
import ink.abb.pogo.scraper.Settings
import ink.abb.pogo.scraper.Task
import ink.abb.pogo.scraper.util.Log
import ink.abb.pogo.scraper.util.cachedInventories
import ink.abb.pogo.scraper.util.inventory.hasPokeballs
import ink.abb.pogo.scraper.util.map.getCatchablePokemon
import ink.abb.pogo.scraper.util.pokemon.catch
import ink.abb.pogo.scraper.util.pokemon.getIvPercentage
import ink.abb.pogo.scraper.util.pokemon.getStatsFormatted
import ink.abb.pogo.scraper.util.pokemon.shouldTransfer

class SnipePokemon (val latitude: Double, val longitude: Double, val pokemonName: String): Task {
    override fun run(bot: Bot, ctx: Context, settings: Settings) {
        ctx.pauseWalking.set(true)
        ctx.pauseForSniping.set(true)
        val hasPokeballs = ctx.api.cachedInventories.itemBag.hasPokeballs()
        if (!hasPokeballs) {
            ctx.pauseWalking.set(false)
            return
        }

        var oldLatitude = ctx.lat.get()
        var oldLongitude = ctx.lng.get()

        ctx.lat.set(latitude); ctx.lng.set(longitude)
        ctx.api.setLocation(latitude, longitude, 0.0)
        bot.task(GetMapRandomDirection(isForSniping=true))

        val pokemon = ctx.api.map.getCatchablePokemon(ctx.blacklistedEncounters)

        Log.cyan("Sniper Found $pokemon at long/lat ${ctx.lng.get()}/${ctx.lat.get()}")
        val catchablePokemon = pokemon.find { it.pokemonId.toString().toLowerCase().equals(pokemonName.toLowerCase()) }

        Log.cyan(text="$catchablePokemon")
        if (null != catchablePokemon) {
            if (settings.obligatoryTransfer.contains(catchablePokemon.pokemonId) && settings.desiredCatchProbabilityUnwanted == -1.0) {
                ctx.blacklistedEncounters.add(catchablePokemon.encounterId)
                Log.normal("Found pokemon ${catchablePokemon.pokemonId}; blacklisting it because it's unwanted")
                ctx.pauseWalking.set(false)
                return
            }
            Log.green("Found pokemon ${catchablePokemon.pokemonId}")

            val encounterResult = catchablePokemon.encounterPokemon()
            val wasFromLure = encounterResult is DiskEncounterResult
            if (encounterResult.wasSuccessful()) {
                val pokemonData = encounterResult.pokemonData
                Log.green("Encountered pokemon ${catchablePokemon.pokemonId} " +
                        "with CP ${pokemonData.cp} and IV ${pokemonData.getIvPercentage()}%")

                ctx.lat.set(oldLatitude); ctx.lng.set(oldLongitude)
                ctx.api.setLocation(oldLatitude, oldLongitude, 0.0)

                bot.task(GetMapRandomDirection(isForSniping=true))

                val (shouldRelease, reason) = pokemonData.shouldTransfer(settings)
                val desiredCatchProbability = if (shouldRelease) {
                    Log.yellow("Using desired_catch_probability_unwanted because $reason")
                    settings.desiredCatchProbabilityUnwanted
                } else {
                    settings.desiredCatchProbability
                }
                if (desiredCatchProbability == -1.0) {
                    ctx.blacklistedEncounters.add(catchablePokemon.encounterId)
                    Log.normal("CP/IV of encountered pokemon ${catchablePokemon.pokemonId} is too low; blacklisting encounter")
                    ctx.pauseWalking.set(false)
                    return
                }

                val result = catchablePokemon.catch(
                        encounterResult.captureProbability,
                        ctx.api.cachedInventories.itemBag,
                        desiredCatchProbability,
                        settings.alwaysCurve,
                        !settings.neverUseBerries,
                        -1)

                if (result == null) {
                    ctx.blacklistedEncounters.add(catchablePokemon.encounterId)
                    Log.red("No Pokeballs in your inventory; blacklisting Pokemon")
                    ctx.pauseWalking.set(false)
                    return
                }

                ctx.blacklistedEncounters.add(catchablePokemon.encounterId)
                if (result.status == CatchPokemonResponse.CatchStatus.CATCH_SUCCESS) {
                    ctx.pokemonStats.first.andIncrement
                    if (wasFromLure) {
                        ctx.luredPokemonStats.andIncrement
                    }
                    val iv = (pokemonData.individualAttack + pokemonData.individualDefense + pokemonData.individualStamina) * 100 / 45
                    var message = "Caught a ${catchablePokemon.pokemonId} " +
                            "with CP ${pokemonData.cp} and IV $iv%"
                    message += "\r\n ${pokemonData.getStatsFormatted()}"
                    if (settings.displayIfPokemonFromLure) {
                        if (encounterResult is DiskEncounterResult)
                            message += " (lured pokemon) "
                        else
                            message += " (wild pokemon) "
                    }
                    if (settings.displayPokemonCatchRewards)
                        message += ": [${result.xpList.sum()}x XP, ${result.candyList.sum()}x " +
                                "Candy, ${result.stardustList.sum()}x Stardust]"
                    Log.cyan(message)

                    ctx.server.newPokemon(catchablePokemon.latitude, catchablePokemon.longitude, pokemonData)
                    ctx.server.sendProfile()
                } else {
                    Log.red("Capture of ${catchablePokemon.pokemonId} failed with status : ${result.status}")
                    if (result.status == CatchPokemonResponse.CatchStatus.CATCH_ERROR) {
                        Log.red("Blacklisting pokemon to prevent infinite loop")
                    }
                }
            } else {
                // We need to set this back to the old value.
                ctx.lat.set(oldLatitude); ctx.lng.set(oldLongitude)
                ctx.api.setLocation(oldLatitude, oldLongitude, 0.0)

                Log.red("Encounter failed with result: ${encounterResult.status}")
                if (encounterResult.status == Status.POKEMON_INVENTORY_FULL) {
                    Log.red("Disabling catching of Pokemon")
                    
                    ctx.pokemonInventoryFullStatus.second.set(true)
                    
                    settings.catchPokemon = false
                }
            }
        }
        ctx.pauseWalking.set(false)
        bot.task(GetMapRandomDirection(isForSniping=false))
        ctx.pauseForSniping.set(false)

    }
}
