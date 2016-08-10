package ink.abb.pogo.scraper.util.data

import com.pokegoapi.api.inventory.Item

/**
 * Created by bertrand on 8/10/16.
 */
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