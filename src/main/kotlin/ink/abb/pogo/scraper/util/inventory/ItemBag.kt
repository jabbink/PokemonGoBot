/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

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

