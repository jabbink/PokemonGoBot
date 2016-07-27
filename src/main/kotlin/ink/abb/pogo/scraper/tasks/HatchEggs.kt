/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper.tasks

import POGOProtos.Networking.Responses.UseItemEggIncubatorResponseOuterClass
import ink.abb.pogo.scraper.Bot
import ink.abb.pogo.scraper.Context
import ink.abb.pogo.scraper.Settings
import ink.abb.pogo.scraper.Task
import ink.abb.pogo.scraper.util.Log
import ink.abb.pogo.scraper.util.pokemon.getIvPercentage

class HatchEggs : Task {

    override fun run(bot: Bot, ctx: Context, settings: Settings) {
        val result = ctx.api.inventories.hatchery.queryHatchedEggs()
        if (result.isNotEmpty()) {
            result.forEachIndexed { index, it ->
                val newPokemon = ctx.api.inventories.pokebank.getPokemonById(it.id)
                val stats = "+${it.candy} candy; +${it.experience} XP; +${it.stardust} stardust"
                if (newPokemon == null) {
                    Log.cyan("Hatched pokemon; $stats")
                } else {
                    Log.cyan("Hatched ${newPokemon.pokemonId.name} with ${newPokemon.cp} CP " +
                            "and ${newPokemon.getIvPercentage()}% IV; $stats")
                }
            }
        }

        val freeIncubators = ctx.api.inventories.incubators.filter { !it.isInUse }
        val eggs = ctx.api.inventories.hatchery.eggs
                .filter { !it.isIncubate }
                .sortedByDescending { it.eggKmWalkedTarget }
        if (freeIncubators.isNotEmpty() && eggs.isNotEmpty() && settings.shouldAutoFillIncubators) {
            val result2 = eggs.first().incubate(freeIncubators.first())
            if (result2 == UseItemEggIncubatorResponseOuterClass.UseItemEggIncubatorResponse.Result.SUCCESS) {
                Log.cyan("Put egg of ${eggs.first().eggKmWalkedTarget}km in unused incubator")
            } else {
                Log.red("Failed to put egg in incubator; error: $result2")
            }
        }
    }
}
