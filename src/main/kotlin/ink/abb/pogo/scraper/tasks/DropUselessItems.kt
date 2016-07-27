/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper.tasks

import POGOProtos.Networking.Responses.RecycleInventoryItemResponseOuterClass
import ink.abb.pogo.scraper.Bot
import ink.abb.pogo.scraper.Context
import ink.abb.pogo.scraper.Settings
import ink.abb.pogo.scraper.Task
import ink.abb.pogo.scraper.util.Log

class DropUselessItems : Task {
    override fun run(bot: Bot, ctx: Context, settings: Settings) {

        settings.uselessItems.forEach {
            val item = ctx.api.inventories.itemBag.getItem(it.key)
            val count = item.count - it.value
            if (it.value != -1 && count > 0) {
                val result = ctx.api.inventories.itemBag.removeItem(it.key, count)
                if (result == RecycleInventoryItemResponseOuterClass.RecycleInventoryItemResponse.Result.SUCCESS) {
                    ctx.itemStats.second.getAndAdd(count)
                    Log.yellow("Dropped ${count}x ${it.key.name}")
                } else {
                    Log.red("Failed to drop ${count}x ${it.key.name}: $result")
                }
            }
        }
    }
}
