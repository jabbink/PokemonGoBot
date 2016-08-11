package ink.abb.pogo.scraper.util.data

import POGOProtos.Data.PokedexEntryOuterClass
/**
 * Created by Peyphour on 8/11/16.
 */

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