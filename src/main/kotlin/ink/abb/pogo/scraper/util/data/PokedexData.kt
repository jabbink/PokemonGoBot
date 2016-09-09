/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper.util.data

import POGOProtos.Data.PokedexEntryOuterClass

data class PokedexEntry(

        var timesEncountered: Int? = null,
        var timeCaptured: Int? = null,
        var pokemonName: String? = null,
        var pokemonNumber: Int? = null
) {
    fun buildFromEntry(entry: PokedexEntryOuterClass.PokedexEntry): PokedexEntry {

        this.timesEncountered = entry.timesEncountered
        this.timeCaptured = entry.timesCaptured
        this.pokemonName = entry.pokemonId.name
        this.pokemonNumber = entry.pokemonId.number

        return this
    }
}
