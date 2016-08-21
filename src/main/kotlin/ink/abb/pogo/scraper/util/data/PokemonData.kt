/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper.util.data

import com.pokegoapi.api.pokemon.Pokemon
import ink.abb.pogo.scraper.util.pokemon.getIv
import ink.abb.pogo.scraper.util.pokemon.getIvPercentage
import ink.abb.pogo.scraper.util.pokemon.getStatsFormatted

data class PokemonData(
        var id: Long? = null,
        var pokemonId: Int? = null,
        var name: String? = null,
        var nickname: String? = null,
        var cp: Int? = null,
        var iv: Int? = null,
        var stats: String? = null,
        var favorite: Boolean? = null,

        // PR #968
        var individualStamina: Int? = null,
        var individualAttack: Int? = null,
        var individualDefense: Int? = null,
        var candy: Int? = null,
        var candiesToEvolve: Int? = null,
        var level: Float? = null,
        var move1: String? = null,
        var move2: String? = null,
        var deployedFortId: String? = null,
        var stamina: Int? = null,
        var maxStamina: Int? = null,
        var maxCp: Int? = null,
        var absMaxCp: Int? = null,
        var maxCpFullEvolveAndPowerup: Int? = null,
        var candyCostsForPowerup: Int? = null,
        var stardustCostsForPowerup: Int? = null,
		var.cpAfterPowerup: Int? = null,
        var.creationTimeMs: Int? = null,
        var.numerOfPowerupsDone: Int? = null,
		var.battlesAttacked: Int? = null,
		var.battlesDefended: Int? = null

) {
    fun buildFromPokemon(pokemon: Pokemon): PokemonData {
        this.id = pokemon.id
        this.pokemonId = pokemon.pokemonId.number
        this.name = pokemon.pokemonId.name
        this.nickname = pokemon.nickname
        this.cp = pokemon.cp
        this.iv = pokemon.getIvPercentage()
        this.stats = pokemon.getStatsFormatted()
        this.favorite = pokemon.isFavorite

        // PR #968
        this.individualStamina = pokemon.individualStamina
        this.individualAttack = pokemon.individualAttack
        this.individualDefense = pokemon.individualDefense
        this.candy = pokemon.candy
        this.candiesToEvolve = pokemon.candiesToEvolve
        this.level = pokemon.level
        this.move1 = pokemon.move1.name
        this.move2 = pokemon.move2.name
        this.deployedFortId = pokemon.deployedFortId
        this.stamina = pokemon.stamina
        this.maxStamina = pokemon.maxStamina
        this.maxCp = pokemon.maxCp
        this.absMaxCp = pokemon.absoluteMaxCp
        this.maxCpFullEvolveAndPowerup = pokemon.cpFullEvolveAndPowerup
        this.candyCostsForPowerup = pokemon.candyCostsForPowerup
        this.stardustCostsForPowerup = pokemon.stardustCostsForPowerup
		this.cpAfterPowerup = pokemon.cpAfterPowerup
        this.creationTimeMs = pokemon.creationTimeMs
        this.numerOfPowerupsDone = pokemon.numerOfPowerupsDone
		this.battlesDefended = pokemon.battlesDefended
		this.battlesAttacked = pokemon.battlesAttacked
        return this
    }

}