/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper.util.data

import com.pokegoapi.api.pokemon.EggPokemon

data class EggData (
        var isIncubate: Boolean? = null,
        var kmWalked: Double? = null,
        var kmTarget: Double? = null
) {
    fun buildFromEggPokemon(egg: EggPokemon): EggData {

        this.isIncubate = egg.isIncubate
        this.kmWalked = egg.eggKmWalked
        this.kmTarget = egg.eggKmWalkedTarget

        return this
    }
}
