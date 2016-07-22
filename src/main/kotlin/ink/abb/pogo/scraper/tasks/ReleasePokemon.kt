/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper.tasks

import ink.abb.pogo.scraper.Bot
import ink.abb.pogo.scraper.Context
import ink.abb.pogo.scraper.Settings
import ink.abb.pogo.scraper.Task

class ReleasePokemon : Task {
    override fun run(bot: Bot, ctx: Context, settings: Settings) {
        if (!settings.shouldAutoTransfer) {
            return
        }
        val groupedPokemon = ctx.api.pokebank.pokemons.groupBy { it.pokemonId }
        val ignoredPokemon = settings.ignoredPokemon
        val obligatoryTransfer = settings.obligatoryTransfer
        val maxCP = settings.transferCPThreshold

        groupedPokemon.forEach {
            val sorted = it.value.sortedByDescending { it.cp }
            for ((index, pokemon) in sorted.withIndex()) {
                // never transfer highest rated Pokemon
                // never transfer > maxCP, unless set in obligatoryTransfer
                // stop releasing when pokemon is set in ignoredPokemon
                if (index > 0 && (pokemon.cp < maxCP || obligatoryTransfer.contains(pokemon.pokemonId.name)) &&
                        (!ignoredPokemon.contains(pokemon.pokemonId.name))) {
                    println("Going to transfer ${pokemon.pokemonId.name} with CP ${pokemon.cp}")
                    pokemon.transferPokemon()
                }
            }
        }
    }
}