/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper.util.data

import POGOProtos.Inventory.Item.ItemIdOuterClass

data class ItemData(
        var itemId: Int? = null,
        var itemName: String? = null,
        var count: Int? = null
) {
    fun buildFromItem(item: ItemIdOuterClass.ItemId, count: Int): ItemData {
        this.itemId = item.number
        this.itemName = item.name
        this.count = count
        return this
    }
}
