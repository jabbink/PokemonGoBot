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
import ink.abb.pogo.scraper.util.directions.getAltitude
import ink.abb.pogo.scraper.util.inventory.hasPokeballs
import ink.abb.pogo.scraper.util.map.getCatchablePokemon
import ink.abb.pogo.scraper.util.pokemon.catch
import ink.abb.pogo.scraper.util.pokemon.getIvPercentage
import ink.abb.pogo.scraper.util.pokemon.getStatsFormatted
import ink.abb.pogo.scraper.util.pokemon.shouldTransfer

class CatchOneNearbyPokemon : Task {
    override fun run(bot: Bot, ctx: Context, settings: Settings) {
        // STOP WALKING
        ctx.pauseWalking.set(true)
        val pokemon = ctx.api.map.getCatchablePokemon(ctx.blacklistedEncounters)

        val itemBag = ctx.api.cachedInventories.itemBag
        val hasPokeballs = itemBag.hasPokeballs()

        /*Pokeball.values().forEach {
            Log.yellow("${it.ballType}: ${ctx.api.cachedInventories.itemBag.getItem(it.ballType).count}")
        }*/

        if (!hasPokeballs) {
            ctx.pauseWalking.set(false)
            return
        }

        if (pokemon.isNotEmpty()) {
            val catchablePokemon = pokemon.first()
            if (settings.obligatoryTransfer.contains(catchablePokemon.pokemonId) && settings.desiredCatchProbabilityUnwanted == -1.0 || settings.neverCatchPokemon.contains(catchablePokemon.pokemonId)) {
                ctx.blacklistedEncounters.add(catchablePokemon.encounterId)
                Log.normal("Found pokemon ${catchablePokemon.pokemonId}; blacklisting because it's unwanted")
                ctx.pauseWalking.set(false)
                return
            }
            Log.green("Found pokemon ${catchablePokemon.pokemonId}")
            ctx.api.setLocation(ctx.lat.get(), ctx.lng.get(), getAltitude(ctx.lat.get(), ctx.lng.get(), ctx))

            val encounterResult = catchablePokemon.encounterPokemon()
            val wasFromLure = encounterResult is DiskEncounterResult
            if (encounterResult.wasSuccessful()) {
                val pokemonData = encounterResult.pokemonData
                Log.green("Encountered pokemon ${catchablePokemon.pokemonId} " +
                        "with CP ${pokemonData.cp} and IV ${pokemonData.getIvPercentage()}%")
                val (shouldRelease, reason) = pokemonData.shouldTransfer(settings)
                val desiredCatchProbability = if (shouldRelease) {
                    Log.yellow("Using desired_catch_probability_unwanted because $reason")
                    settings.desiredCatchProbabilityUnwanted
                } else {
                    settings.desiredCatchProbability
                }
                if (desiredCatchProbability == -1.0) {
                    ctx.blacklistedEncounters.add(catchablePokemon.encounterId)
                    Log.normal("CP/IV of encountered pokemon ${catchablePokemon.pokemonId} turns out to be too low; blacklisting encounter")
                    ctx.pauseWalking.set(false)
                    return
                }
                val isBallCurved = (Math.random() < settings.desiredCurveRate)
                //TODO: Give settings object to the catch function instead of the seperate values
                val result = catchablePokemon.catch(
                        encounterResult.captureProbability,
                        itemBag,
                        desiredCatchProbability,
                        isBallCurved,
                        !settings.neverUseBerries,
                        settings.randomBallThrows,
                        settings.waitBetweenThrows,
                        -1)

                if (result == null) {
                    // prevent trying it in the next iteration
                    ctx.blacklistedEncounters.add(catchablePokemon.encounterId)
                    Log.red("No Pokeballs in your inventory; blacklisting Pokemon")
                    ctx.pauseWalking.set(false)
                    return
                }

                // TODO: temp fix for server timing issues regarding GetMapObjects
                ctx.blacklistedEncounters.add(catchablePokemon.encounterId)
                if (result.status == CatchPokemonResponse.CatchStatus.CATCH_SUCCESS) {
                    ctx.pokemonStats.first.andIncrement
                    if (wasFromLure) {
                        ctx.luredPokemonStats.andIncrement
                    }
                    var message = "Caught a "
                    if (settings.displayIfPokemonFromLure) {
                        if (encounterResult is DiskEncounterResult)
                            message += "lured "
                        else
                            message += "wild "
                    }
                    message += "${catchablePokemon.pokemonId} with CP ${pokemonData.cp} and IV" +
                            " (${pokemonData.individualAttack}-${pokemonData.individualDefense}-${pokemonData.individualStamina}) ${pokemonData.getIvPercentage()}%"
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
                Log.red("Encounter failed with result: ${encounterResult.status}")
                if (encounterResult.status == Status.POKEMON_INVENTORY_FULL) {
                    if (settings.catchPokemon)
                        Log.red("Inventory fillup, disabling catching of pokemon")
                    ctx.pokemonInventoryFullStatus.set(true)
                }
            }
        }
        ctx.pauseWalking.set(false)
    }
}
