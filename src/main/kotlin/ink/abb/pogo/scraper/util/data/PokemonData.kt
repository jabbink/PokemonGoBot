package ink.abb.pogo.scraper.util.data

import com.pokegoapi.api.pokemon.Pokemon
import ink.abb.pogo.scraper.util.pokemon.getIv
import ink.abb.pogo.scraper.util.pokemon.getIvPercentage
import ink.abb.pogo.scraper.util.pokemon.getStatsFormatted

/**
 * Created by bertrand on 8/10/16.
 */

data class PokemonData(
        var id: Long? = null,
        var pokemonId: Int? = null,
        var name: String? = null,
        var cp: Int? = null,
        var iv: Int? = null,
        var stats: String? = null
) {
    fun buildFromPokemon(pokemon: Pokemon): PokemonData {
        this.id = pokemon.id
        this.pokemonId = pokemon.pokemonId.number
        this.name = pokemon.pokemonId.name
        this.cp = pokemon.cp
        this.iv = pokemon.getIvPercentage()
        this.stats = pokemon.getStatsFormatted()

        return this
    }
}