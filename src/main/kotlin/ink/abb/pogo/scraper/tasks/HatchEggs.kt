/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper.tasks

import POGOProtos.Networking.Responses.UseItemEggIncubatorResponseOuterClass
import ink.abb.pogo.api.request.GetHatchedEggs
import ink.abb.pogo.api.request.UseItemEggIncubator
import ink.abb.pogo.scraper.Bot
import ink.abb.pogo.scraper.Context
import ink.abb.pogo.scraper.Settings
import ink.abb.pogo.scraper.Task
import ink.abb.pogo.scraper.util.Log
import ink.abb.pogo.scraper.util.pokemon.getIvPercentage
import ink.abb.pogo.scraper.util.pokemon.incubated

class HatchEggs : Task {

    override fun run(bot: Bot, ctx: Context, settings: Settings) {
        // not necessary, update profile is executed before this already in the tasks
        //bot.api.queueRequest(GetInventory().withLastTimestampMs(0))
        bot.api.queueRequest(GetHatchedEggs()).subscribe {
            val response = it.response
            response.pokemonIdList.forEachIndexed { index, it ->
                val newPokemon = ctx.api.inventory.pokemon[it]
                val candy = response.candyAwardedList[index]
                val experience = response.experienceAwardedList[index]
                val stardust = response.stardustAwardedList[index]
                val stats = "+${candy} candy; +${experience} XP; +${stardust} stardust"
                if (newPokemon == null) {
                    Log.cyan("Hatched pokemon; $stats")
                } else {
                    Log.cyan("Hatched ${newPokemon.pokemonData.pokemonId.name} with ${newPokemon.pokemonData.cp} CP " +
                            "and ${newPokemon.pokemonData.getIvPercentage()}% IV; $stats")
                }
            }
        }

        val incubators = ctx.api.inventory.eggIncubators
        val eggs = ctx.api.inventory.eggs

        val freeIncubators = incubators.map { it.value }
                .filter { it.targetKmWalked < bot.api.inventory.playerStats.kmWalked }
                .sortedByDescending { it.usesRemaining }
        val filteredEggs = eggs.map { it.value }
                .filter { !it.pokemonData.incubated }
                .sortedByDescending { it.pokemonData.eggKmWalkedTarget }
        if (freeIncubators.isNotEmpty() && filteredEggs.isNotEmpty() && settings.autoFillIncubator) {
            var eggResult = filteredEggs.first()
            if (freeIncubators.first().usesRemaining == 0) {
                eggResult = filteredEggs.last()
            }
            val use = UseItemEggIncubator().withPokemonId(eggResult.pokemonData.id).withItemId(freeIncubators.first().id)
            bot.api.queueRequest(use).subscribe {
                val response = it.response

                if (response.result == UseItemEggIncubatorResponseOuterClass.UseItemEggIncubatorResponse.Result.SUCCESS) {
                    Log.cyan("Put egg of ${eggResult.pokemonData.eggKmWalkedTarget}km in unused incubator")
                } else {
                    Log.red("Failed to put egg in incubator; error: ${response.result}")
                }
            }
        }
    }
}
