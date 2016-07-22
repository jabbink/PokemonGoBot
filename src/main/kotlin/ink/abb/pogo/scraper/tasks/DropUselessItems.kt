package ink.abb.pogo.scraper.tasks

import POGOProtos.Networking.Responses.RecycleInventoryItemResponseOuterClass
import ink.abb.pogo.scraper.Bot
import ink.abb.pogo.scraper.Context
import ink.abb.pogo.scraper.Settings
import ink.abb.pogo.scraper.Task

/**
 * @author Andrew Potter (apottere)
 */
class DropUselessItems : Task {
    override fun run(bot: Bot, ctx: Context, settings: Settings) {
        if(!settings.shouldDropItems) {
            return
        }

        settings.uselessItems.forEach {
            val item = ctx.api.bag.getItem(it.key)
            val count = item.count - it.value
            if (count > 0) {
                val result = ctx.api.bag.removeItem(it.key, count)
                if (result == RecycleInventoryItemResponseOuterClass.RecycleInventoryItemResponse.Result.SUCCESS) {
                    println("Dropped ${count}x ${it.key.name}")
                } else {
                    println("Failed to drop ${count}x ${it.key.name}: $result")
                }
            }
        }
    }
}
