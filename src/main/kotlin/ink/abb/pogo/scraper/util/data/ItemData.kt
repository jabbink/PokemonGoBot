/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper.util.data

import com.pokegoapi.api.inventory.Item

data class ItemData(
    var itemId: Int? = null,
    var itemName: String? = null,
    var count: Int? = null
) {
    fun buildFromItem(item: Item): ItemData {
        this.itemId = item.itemId.number
        this.itemName = item.itemId.name
        this.count = item.count
        return this
    }
}
