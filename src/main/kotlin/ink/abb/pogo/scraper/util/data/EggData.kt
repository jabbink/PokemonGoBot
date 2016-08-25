/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper.util.data

import ink.abb.pogo.api.cache.BagPokemon
import ink.abb.pogo.scraper.util.pokemon.eggKmWalked
import ink.abb.pogo.scraper.util.pokemon.incubated

data class EggData(
        var isIncubate: Boolean? = null,
        var kmWalked: Double? = null,
        var kmTarget: Double? = null
) {
    fun buildFromEggPokemon(egg: BagPokemon): EggData {
        this.isIncubate = egg.pokemonData.incubated
        this.kmWalked = egg.pokemonData.eggKmWalked(egg.poGoApi)
        this.kmTarget = egg.pokemonData.eggKmWalkedTarget

        return this
    }
}
