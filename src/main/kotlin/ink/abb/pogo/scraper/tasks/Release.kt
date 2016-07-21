package ink.abb.pogo.scraper.tasks

import ink.abb.pogo.scraper.Context
import ink.abb.pogo.scraper.Task

/**
 * Created by TimD on 7/21/2016.
 */
class Release : Task {
    override fun run(context: Context?) {
        val groupedPokemon = context!!.api.pokebank.pokemons.groupBy { it.pokemonId }
        groupedPokemon.forEach {
            val sorted = it.value.sortedByDescending { it.cp }
            for ((index, pokemon) in sorted.withIndex()) {
                if (index > 0 && pokemon.cp < 400) {
                    println("Going to transfer ${pokemon.pokemonId.name} with CP ${pokemon.cp}")
                    pokemon.transferPokemon()
                }
            }
        }
    }
}