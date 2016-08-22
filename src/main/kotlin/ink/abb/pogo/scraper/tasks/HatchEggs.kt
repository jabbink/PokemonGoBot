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
import ink.abb.pogo.scraper.util.cachedInventories
import ink.abb.pogo.scraper.util.pokemon.getIvPercentage

class HatchEggs : Task {

    override fun run(bot: Bot, ctx: Context, settings: Settings) {
        val result = ctx.api.cachedInventories.hatchery.queryHatchedEggs()
        if (result.isNotEmpty()) {
            ctx.api.inventories.updateInventories(true)
            result.forEachIndexed { index, it ->
                val newPokemon = ctx.api.cachedInventories.pokebank.getPokemonById(it.id)
                val stats = "+${it.candy} candy; +${it.experience} XP; +${it.stardust} stardust"
                if (newPokemon == null) {
                    Log.cyan("Hatched pokemon; $stats")
                } else {
                    Log.cyan("Hatched ${newPokemon.pokemonId.name} with ${newPokemon.cp} CP " +
                            "and ${newPokemon.getIvPercentage()}% IV; $stats")
                }
            }
        }

        val incubators = ctx.api.cachedInventories.incubators
        val eggs = ctx.api.cachedInventories.hatchery.eggs

        val freeIncubators = incubators
                .filter { !it.isInUse }
                .sortedByDescending { it.usesRemaining }
        val filteredEggs = eggs
                .filter { !it.isIncubate }
                .sortedByDescending { it.eggKmWalkedTarget }
        if (freeIncubators.isNotEmpty() && filteredEggs.isNotEmpty() && settings.autoFillIncubator) {
            var eggResult = filteredEggs.first() 
            if(freeIncubators.first().usesRemaining == 0) {
                eggResult = filteredEggs.last()
            }
            val incubateResult = eggResult.incubate(freeIncubators.first())
            if (incubateResult == UseItemEggIncubatorResponseOuterClass.UseItemEggIncubatorResponse.Result.SUCCESS) {
                Log.cyan("Put egg of ${eggResult.eggKmWalkedTarget}km in unused incubator")
            } else {
                Log.red("Failed to put egg in incubator; error: $incubateResult")
            }
        }
    }
}
