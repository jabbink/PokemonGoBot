/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper.util.map

import com.pokegoapi.api.map.Map
import com.pokegoapi.api.map.pokemon.CatchablePokemon

fun Map.getCatchablePokemon(blacklist: Set<Long>): List<CatchablePokemon> {
    return catchablePokemon.filter { !blacklist.contains(it.encounterId) }
}
