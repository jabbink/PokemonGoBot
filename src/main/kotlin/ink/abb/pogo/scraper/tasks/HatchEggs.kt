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
        val freeIncubators = ctx.api.inventories.incubators.filter { !it.isInUse }
        val eggs = ctx.api.inventories.hatchery.eggs
                .filter { it.eggIncubatorId == null || it.eggIncubatorId.isBlank() }
                .sortedByDescending { it.eggKmWalkedTarget }
        if (freeIncubators.isNotEmpty() && eggs.isNotEmpty()) {
            val result = freeIncubators.first().hatchEgg(eggs.first())
            if (result == UseItemEggIncubatorResponseOuterClass.UseItemEggIncubatorResponse.Result.SUCCESS) {
                Log.green("Put egg ${eggs.first().id} in unused incubator")
            } else {
                Log.red("Failed to put egg in incubator; error: $result")
            }
        } else {
            val result = ctx.api.inventories.hatchery.queryHatchedEggs()
            if (result.isNotEmpty()) {
                result.forEachIndexed { index, it ->
                    // TODO: That proto is probably wrong and this fails.
                    val newPokemon = ctx.api.inventories.pokebank.getPokemonById(it.id)
                    val stats = "+${it.candy} candy; +${it.experience} XP; +${it.stardust} stardust"
                    if (newPokemon == null) {
                        Log.green("Hatched pokemon; $stats")
                    } else {
                        Log.green("Hatched ${newPokemon.pokemonId.name} with ${newPokemon.cp} CP " +
                                "and ${newPokemon.getIvPercentage()}% IV; $stats")
                    }
                }
            }
        }
    }
}
