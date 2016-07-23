/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper.tasks

import Log
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
            var ball = getPreferredBall(catchablePokemon, ctx, settings)

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
                }
            }
        }
    }

    fun getPreferredBall(pokemon: CatchablePokemon, ctx: Context, settings: Settings) : ItemIdOuterClass.ItemId? {
        var pokemonName = pokemon.pokemonId.name
        var preferred = settings.preferredBall;

        if(settings.masterBallPrefOverride.contains(pokemonName))
            preferred = ItemIdOuterClass.ItemId.ITEM_MASTER_BALL
        else if (settings.ultraBallPrefOverride.contains(pokemonName))
            preferred = ItemIdOuterClass.ItemId.ITEM_ULTRA_BALL
        else if (settings.greatBallPrefOverride.contains(pokemonName))
            preferred = ItemIdOuterClass.ItemId.ITEM_GREAT_BALL

        return nextBallInOrder(preferred, ctx, settings)
    }

    fun nextBallInOrder(preferred: ItemIdOuterClass.ItemId, ctx: Context, settings: Settings) : ItemIdOuterClass.ItemId? {
        var ballSize = settings.pokeballItems.keys.size
        var preferredIdx = settings.pokeballItems.keys.indexOf(preferred)
        var currentIdx = preferredIdx;

        do {
            var item = ctx.api.bag.getItem(settings.pokeballItems.keys.elementAt(currentIdx))
            if (item != null && item.count > 0)
                return item.itemId;

            //Wrap around if we can't find what we need in the order of priority
            currentIdx = (++currentIdx % ballSize);
        } while(currentIdx != preferredIdx)

        return null;
    }
}