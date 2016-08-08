/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper.util.map

import com.pokegoapi.api.PokemonGo
import com.pokegoapi.api.map.fort.Pokestop

fun Pokestop.canLoot(ignoreDistance: Boolean = false, lootTimeouts: Map<String, Long>, api: PokemonGo): Boolean {
    val canLoot = lootTimeouts.getOrElse(id, { cooldownCompleteTimestampMs }) < api.currentTimeMillis()
    return (ignoreDistance || inRange()) && canLoot
}

fun Pokestop.inRange(api: PokemonGo, maxDistance: Double): Boolean {
    return distance < maxDistance
}