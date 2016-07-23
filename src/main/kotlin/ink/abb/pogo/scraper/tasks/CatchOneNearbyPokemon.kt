/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper.tasks

import POGOProtos.Inventory.ItemIdOuterClass
import POGOProtos.Networking.Responses.CatchPokemonResponseOuterClass
import com.pokegoapi.api.map.pokemon.CatchablePokemon
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
                println("Found pokemon ${catchablePokemon.pokemonId}")
                ctx.api.setLocation(ctx.lat.get(), ctx.lng.get(), 0.0)
                val encounterResult = catchablePokemon.encounterPokemon()
                if (encounterResult.wasSuccessful()) {
                    println("Encountered pokemon ${catchablePokemon.pokemonId}")
                    val result = catchablePokemon.catchPokemon(usedPokeball)

                    if (result.status == CatchPokemonResponseOuterClass.CatchPokemonResponse.CatchStatus.CATCH_SUCCESS)
                        println("Caught a ${catchablePokemon.pokemonId} using $ball")
                    else
                        println("Capture of ${catchablePokemon.pokemonId} failed with status : ${result.status}")
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