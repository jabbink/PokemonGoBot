/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper.tasks

import POGOProtos.Inventory.Item.ItemIdOuterClass.ItemId
import POGOProtos.Networking.Responses.RecycleInventoryItemResponseOuterClass
import ink.abb.pogo.scraper.Bot
import ink.abb.pogo.scraper.Context
import ink.abb.pogo.scraper.Settings
import ink.abb.pogo.scraper.Task
import ink.abb.pogo.scraper.util.Log
import ink.abb.pogo.scraper.util.cachedInventories

class DropUselessItems : Task {
    override fun run(bot: Bot, ctx: Context, settings: Settings) {
        // ignores the items that have -1
        val itemsToDrop = settings.uselessItems.filter { it.value != -1 }
        if (settings.groupItemsByType) dropGroupedItems(ctx, itemsToDrop, settings) else dropItems(ctx, itemsToDrop, settings)
    }

    /**
     * Drops the excess items by group
     */
    fun dropGroupedItems(ctx: Context, items: Map<ItemId, Int>, settings: Settings) {
        // map with what items to keep in what amounts
        val itemsToDrop = mutableMapOf<ItemId, Int>()
        // adds not groupable items on map
        itemsToDrop.putAll(items.filter { singlesFilter.contains(it.key) })
        // groups items
        val groupedItems = groupItems(items)
        // adds new items to the map
        val itemBag = ctx.api.cachedInventories.itemBag
        groupedItems.forEach groupedItems@ {
            var groupCount = 0
            it.key.forEach { groupCount += itemBag.getItem(it).count }
            var neededToDrop = groupCount - it.value
            if (neededToDrop > 0)
                it.key.forEach {
                    val item = itemBag.getItem(it)
                    if (neededToDrop <= item.count) {
                        itemsToDrop.put(it, item.count - neededToDrop)
                        return@groupedItems
                    } else {
                        neededToDrop -= item.count
                        itemsToDrop.put(it, 0)
                    }
                }
        }
        // drops excess items
        dropItems(ctx, itemsToDrop, settings)
    }

    /**
     * Groups the items using the groupFilters
     * Each group contains the list of itemIds of the group and sum of all its number
     */
    fun groupItems(items: Map<ItemId, Int>): Map<Array<ItemId>, Int> {
        val groupedItems = mutableMapOf<Array<ItemId>, Int>()
        groupFilters.forEach {
            val filter = it
            val filteredItems = items.filter { filter.contains(it.key) }
            groupedItems.put(filteredItems.keys.toTypedArray(), filteredItems.values.sum())
        }
        return groupedItems
    }

    // Items that can be grouped
    val groupFilters = arrayOf(
            arrayOf(ItemId.ITEM_REVIVE, ItemId.ITEM_MAX_REVIVE),
            arrayOf(ItemId.ITEM_POTION, ItemId.ITEM_SUPER_POTION, ItemId.ITEM_HYPER_POTION, ItemId.ITEM_MAX_POTION),
            arrayOf(ItemId.ITEM_POKE_BALL, ItemId.ITEM_GREAT_BALL, ItemId.ITEM_ULTRA_BALL, ItemId.ITEM_MASTER_BALL)
    )

    // Items that cant be grouped
    val singlesFilter = arrayOf(ItemId.ITEM_RAZZ_BERRY, ItemId.ITEM_LUCKY_EGG, ItemId.ITEM_INCENSE_ORDINARY, ItemId.ITEM_TROY_DISK)

    /**
     * Drops the excess items by item
     */
    fun dropItems(ctx: Context, items: Map<ItemId, Int>, settings: Settings) {
        val itemBag = ctx.api.cachedInventories.itemBag
        items.forEach {
            val item = itemBag.getItem(it.key)
            val count = item.count - it.value
            if (count > 0) {
                val result = itemBag.removeItem(it.key, count)
                if (result == RecycleInventoryItemResponseOuterClass.RecycleInventoryItemResponse.Result.SUCCESS) {
                    ctx.itemStats.second.getAndAdd(count)
                    Log.yellow("Dropped ${count}x ${it.key.name}")
                    ctx.server.sendProfile()
                } else {
                    Log.red("Failed to drop ${count}x ${it.key.name}: $result")
                }
                if(settings.itemDropDelay != (-1).toLong()){
                    val itemDropDelay = settings.itemDropDelay/2 + (Math.random()*settings.itemDropDelay).toLong()
                    Thread.sleep(itemDropDelay)
                }
            }
        }
    }
}
