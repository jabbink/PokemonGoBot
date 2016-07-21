package ink.abb.pogo.scraper.tasks

import ink.abb.pogo.scraper.Context
import ink.abb.pogo.scraper.Task

/**
 * Created by TimD on 7/21/2016.
 */
class Release : Task {
    override fun run(context: Context?) {
        val groupedPokemon = context!!.api.pokebank.pokemons.groupBy { it.pokemonId }
        val ignoredPokemon = context.ignoredPokemon
        val obligatoryTransfer = context.obligatoryTransfer
        val maxCP = context.maxCP
        groupedPokemon.forEach {
            val sorted = it.value.sortedByDescending { it.cp }
            for ((index, pokemon) in sorted.withIndex()) {
                if (index > 0 &&
                        (pokemon.cp < maxCP || (if(!obligatoryTransfer.isEmpty()) (obligatoryTransfer.contains(pokemon.pokemonId.name)) else (false))) &&
                        (if(!ignoredPokemon.isEmpty()) (!ignoredPokemon.contains(pokemon.pokemonId.name)) else (true))) {
                    println("Going to transfer ${pokemon.pokemonId.name} with CP ${pokemon.cp}")
                    pokemon.transferPokemon()
                }
            }
        }
    }
}