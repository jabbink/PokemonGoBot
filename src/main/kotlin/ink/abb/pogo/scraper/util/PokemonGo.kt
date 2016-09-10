/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper.util

import com.pokegoapi.api.PokemonGo
import com.pokegoapi.api.inventory.Inventories
import java.util.*

private var _inventories: Inventories? = null

var PokemonGo.cachedInventories: Inventories
    get() {
        val inventories = inventories
        val copyInventories = _inventories
        if (copyInventories == null) {
            _inventories = inventories
        } else {
            if (inventories.incubators.isEmpty()) {
                // apparently currently fetching; return cache; always contains infinite incubator
                return copyInventories
            } else {
                _inventories = inventories
            }
        }
        return _inventories!!

    }
    set(inventories) {
        _inventories = inventories
    }
