/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper.util.data

import POGOProtos.Enums.PokemonMoveOuterClass
import com.google.common.geometry.S2CellId
import com.google.common.geometry.S2LatLng
import ink.abb.pogo.api.cache.BagPokemon
import ink.abb.pogo.api.util.PokemonMoveMetaRegistry
import ink.abb.pogo.scraper.util.pokemon.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

data class PokemonData(
        var id: String? = null,
        var pokemonId: Int? = null,
        var name: String? = null,
        var nickname: String? = null,
        var pclass: String? = null,
        var type1: String? = null,
        var type2: String? = null,
        var cp: Int? = null,
        var iv: Int? = null,
        var stats: String? = null,
        var favorite: Boolean? = null,
        var cpMultiplier: Float? = null,
        var heightM: Float? = null,
        var weightKg: Float? = null,

        var individualStamina: Int? = null,
        var individualAttack: Int? = null,
        var individualDefense: Int? = null,
        var candy: Int? = null,
        var candiesToEvolve: Int? = null,
        var level: Float? = null,

        var move1: String? = null,
        var move1Type: String? = null,
        var move1Power: Int? = null,
        var move1Accuracy: Int? = null,
        var move1CritChance: Double? = null,
        var move1Time: Int? = null,
        var move1Energy: Int? = null,

        var move2: String? = null,
        var move2Type: String? = null,
        var move2Power: Int? = null,
        var move2Accuracy: Int? = null,
        var move2CritChance: Double? = null,
        var move2Time: Int? = null,
        var move2Energy: Int? = null,

        var deployedFortId: String? = null,
        var stamina: Int? = null,
        var maxStamina: Int? = null,
        var maxCp: Int? = null,
        var absMaxCp: Int? = null,
        var maxCpFullEvolveAndPowerup: Int? = null,

        var candyCostsForPowerup: Int? = null,
        var stardustCostsForPowerup: Int? = null,
        var creationTime: String? = null,
        var creationTimeMs: Long? = null,
        var creationLatDegrees: Double? = null,
        var creationLngDegrees: Double? = null,
        var baseCaptureRate: Double? = null,
        var baseFleeRate: Double? = null,
        var battlesAttacked: Int? = null,
        var battlesDefended: Int? = null,
        var isInjured: Boolean? = null,
        var isFainted: Boolean? = null,
        var cpAfterPowerup: Int? = null

) {
    fun buildFromPokemon(bagPokemon: BagPokemon): PokemonData {
        val pokemon = bagPokemon.pokemonData
        val latLng = S2LatLng(S2CellId(pokemon.capturedCellId).toPoint())
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

        val pmeta = pokemon.meta
        val pmmeta1 = PokemonMoveMetaRegistry.getMeta(PokemonMoveOuterClass.PokemonMove.forNumber(pokemon.move1.number))
        val pmmeta2 = PokemonMoveMetaRegistry.getMeta(PokemonMoveOuterClass.PokemonMove.forNumber(pokemon.move2.number))

        this.id = pokemon.id.toString()
        this.pokemonId = pokemon.pokemonId.number
        this.name = pokemon.pokemonId.name
        this.nickname = pokemon.nickname
        this.pclass = pmeta.pokemonClass.name
        this.type1 = pmeta.type1.name
        this.type2 = pmeta.type2.name
        this.cp = pokemon.cp
        this.iv = pokemon.getIvPercentage()
        this.stats = pokemon.getStatsFormatted()
        this.favorite = pokemon.favorite > 0
        this.cpMultiplier = pokemon.cpMultiplier
        this.heightM = pokemon.heightM
        this.weightKg = pokemon.weightKg

        this.individualStamina = pokemon.individualStamina
        this.individualAttack = pokemon.individualAttack
        this.individualDefense = pokemon.individualDefense
        this.candy = bagPokemon.poGoApi.inventory.candies.getOrPut(pmeta.family, { AtomicInteger(0) }).get()
        this.candiesToEvolve = pmeta.candyToEvolve
        this.level = pokemon.level

        this.move1 = pokemon.move1.name
        this.move1Type = pmmeta1.type.name
        this.move1Power = pmmeta1.power
        this.move1Accuracy = pmmeta1.accuracy
        this.move1CritChance = pmmeta1.critChance
        this.move1Time = pmmeta1.time
        this.move1Energy = pmmeta1.energy

        this.move2 = pokemon.move2.name
        this.move2Type = pmmeta2.type.name
        this.move2Power = pmmeta2.power
        this.move2Accuracy = pmmeta2.accuracy
        this.move2CritChance = pmmeta2.critChance
        this.move2Time = pmmeta2.time
        this.move2Energy = pmmeta2.energy

        this.deployedFortId = pokemon.deployedFortId
        this.stamina = pokemon.stamina
        this.maxStamina = pokemon.staminaMax
        this.maxCp = pokemon.maxCp
        this.absMaxCp = pokemon.absoluteMaxCp
        this.maxCpFullEvolveAndPowerup = pokemon.cpFullEvolveAndPowerup

        this.candyCostsForPowerup = pokemon.candyCostsForPowerup
        this.stardustCostsForPowerup = pokemon.stardustCostsForPowerup
        this.creationTime = dateFormatter.format(Date(pokemon.creationTimeMs))
        this.creationTimeMs = pokemon.creationTimeMs
        this.creationLatDegrees = latLng.latDegrees()
        this.creationLngDegrees = latLng.lngDegrees()
        this.baseCaptureRate = pmeta.baseCaptureRate
        this.baseFleeRate = pmeta.baseFleeRate
        this.battlesAttacked = pokemon.battlesAttacked
        this.battlesDefended = pokemon.battlesDefended
        this.isInjured = pokemon.injured
        this.isFainted = pokemon.fainted
        this.cpAfterPowerup = pokemon.cpAfterPowerup

        return this
    }
}
