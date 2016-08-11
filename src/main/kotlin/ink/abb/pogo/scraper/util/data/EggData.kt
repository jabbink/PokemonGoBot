package ink.abb.pogo.scraper.util.data

import com.pokegoapi.api.pokemon.EggPokemon

/**
 * Created by Peyphour on 8/11/16.
 */
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