/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper.tasks

import ink.abb.pogo.scraper.util.Log
import POGOProtos.Networking.Responses.UseItemEggIncubatorResponseOuterClass
import ink.abb.pogo.scraper.Bot
import ink.abb.pogo.scraper.Context
import ink.abb.pogo.scraper.Settings
import ink.abb.pogo.scraper.Task

class HatchEggs : Task {

    override fun run(bot: Bot, ctx: Context, settings: Settings) {
        val freeIncubators = ctx.api.inventories.incubators.filter { !it.isInUse }
        val eggs = ctx.api.inventories.hatchery.eggs.filter { it.eggIncubatorId == null || it.eggIncubatorId.isBlank() }
        if (freeIncubators.isNotEmpty() && eggs.isNotEmpty()) {
            val result = freeIncubators.first().hatchEgg(eggs.first())
            if (result == UseItemEggIncubatorResponseOuterClass.UseItemEggIncubatorResponse.Result.SUCCESS) {
                Log.green("Put egg ${eggs.first().id} in unused incubator")
            } else {
                Log.red("Failed to put egg in incubator; error: $result")
            }
        }
    }
}
