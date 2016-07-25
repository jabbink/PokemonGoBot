package ink.abb.pogo.scraper.util.inventory

import com.pokegoapi.api.inventory.ItemBag
import com.pokegoapi.api.inventory.Pokeball

fun ItemBag.hasPokeballs(): Boolean {
    var totalCount = 0
    Pokeball.values().forEach {
        totalCount += getItem(it.ballType).count
    }
    return totalCount > 0
}

