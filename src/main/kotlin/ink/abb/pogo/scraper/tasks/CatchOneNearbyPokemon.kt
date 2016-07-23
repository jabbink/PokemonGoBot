/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper.tasks

import ink.abb.pogo.scraper.util.Log
import POGOProtos.Inventory.Item.ItemIdOuterClass.ItemId
import POGOProtos.Networking.Responses.CatchPokemonResponseOuterClass.CatchPokemonResponse
import ink.abb.pogo.scraper.Bot
import ink.abb.pogo.scraper.Context
import ink.abb.pogo.scraper.Settings
import ink.abb.pogo.scraper.Task

class CatchOneNearbyPokemon : Task {
    override fun run(bot: Bot, ctx: Context, settings: Settings) {
        val pokemon = ctx.api.map.catchablePokemon

        if (pokemon.isNotEmpty()) {
            val catchablePokemon = pokemon.first()
            var ball: ItemId? = null
            try {
                val preferred_ball = settings.preferredBall
                var item = ctx.api.inventories.itemBag.getItem(preferred_ball)

                // if we dont have our prefered pokeball, try fallback to other
                if (item == null || item.count == 0)
                    for (other in settings.pokeballItems) {
                        if (preferred_ball == other) continue

                        item = ctx.api.inventories.itemBag.getItem(other.key);
                        if (item != null && item.count > 0)
                            ball = other.key
                    }
                else
                    ball = preferred_ball
            } catch (e: Exception) {
                throw e
            }

            if (ball != null) {
                val usedPokeball = settings.pokeballItems[ball]
                Log.green("Found pokemon ${catchablePokemon.pokemonId}")
                ctx.api.setLocation(ctx.lat.get(), ctx.lng.get(), 0.0)
                val encounterResult = catchablePokemon.encounterPokemon()
                if (encounterResult.wasSuccessful()) {
                    Log.green("Encountered pokemon ${catchablePokemon.pokemonId} with CP ${encounterResult.wildPokemon.pokemonData.cp}")
                    val result = catchablePokemon.catchPokemon(usedPokeball)

                    if (result.status == CatchPokemonResponse.CatchStatus.CATCH_SUCCESS) {
                        ctx.pokemonStats.first.andIncrement
                        var message = "Caught a ${catchablePokemon.pokemonId} with CP ${encounterResult.wildPokemon.pokemonData.cp} using $ball"

                        if (settings.shouldDisplayPokemonCatchRewards)
                            message += ": [${result.xpList.sum()}x XP, ${result.candyList.sum()}x Candy, ${result.stardustList.sum()}x Stardust]"
                        Log.green(message)

                    } else
                        Log.red("Capture of ${catchablePokemon.pokemonId} failed with status : ${result.status}")
                } else {
                    Log.red("Encounter failed with result: ${encounterResult.getStatus()}")
                } 
            }

        }
    }
}
