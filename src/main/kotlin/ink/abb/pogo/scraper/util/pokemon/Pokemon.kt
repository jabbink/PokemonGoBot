/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper.util.pokemon

import POGOProtos.Data.PokemonDataOuterClass.PokemonData
import com.pokegoapi.api.pokemon.Pokemon
import ink.abb.pogo.scraper.Settings

fun Pokemon.getIv(): Int {
    val iv = individualAttack + individualDefense + individualStamina
    return iv
}

fun Pokemon.getIvPercentage(): Int {
    val iv = getIv()
    val ivPercentage = (iv * 100) / 45
    return ivPercentage
}

fun PokemonData.getIv(): Int {
    val iv = individualAttack + individualDefense + individualStamina
    return iv
}

fun PokemonData.getIvPercentage(): Int {
    val iv = getIv()
    val ivPercentage = (iv * 100) / 45
    return ivPercentage
}

fun PokemonData.getStatsFormatted(): String {
    val details = "Stamina: $individualStamina | Attack: $individualAttack | Defense: $individualDefense"
    return details + " | IV: ${getIv()} (${(getIvPercentage())}%)"
}

fun Pokemon.getStatsFormatted(): String {
    val details = "Stamina: $individualStamina | Attack: $individualAttack | Defense: $individualDefense"
    return details + " | IV: ${getIv()} (${(getIvPercentage())}%)"
}

fun Pokemon.shouldTransfer(settings: Settings): Pair<Boolean, String> {
    val obligatoryTransfer = settings.obligatoryTransfer
    val ignoredPokemon = settings.ignoredPokemon
    val ivPercentage = getIvPercentage()
    val minIVPercentage = settings.transferIVThreshold
    val minCP = settings.transferCPThreshold

    var shouldRelease = obligatoryTransfer.contains(this.pokemonId.name)
    var reason: String = "Obligatory transfer"
    if (!ignoredPokemon.contains(this.pokemonId.name)) {
        if (!shouldRelease) {
            var ivTooLow = false
            var cpTooLow = false

            // never transfer > min IV percentage (unless set to -1)
            if (ivPercentage < minIVPercentage || minIVPercentage == -1) {
                ivTooLow = true
            }
            // never transfer > min CP  (unless set to -1)
            if (this.cp < minCP || minCP == -1) {
                cpTooLow = true
            }
            reason = "CP < $minCP and IV < $minIVPercentage%"
            shouldRelease = ivTooLow && cpTooLow
        }
    }
    return Pair(shouldRelease, reason);
}

// TODO: Deduplicate this
fun PokemonData.shouldTransfer(settings: Settings): Pair<Boolean, String> {
    val obligatoryTransfer = settings.obligatoryTransfer
    val ignoredPokemon = settings.ignoredPokemon
    val ivPercentage = getIvPercentage()
    val minIVPercentage = settings.transferIVThreshold
    val minCP = settings.transferCPThreshold

    var shouldRelease = obligatoryTransfer.contains(this.pokemonId.name)
    var reason: String = "Obligatory transfer"
    if (!ignoredPokemon.contains(this.pokemonId.name)) {
        if (!shouldRelease) {
            var ivTooLow = false
            var cpTooLow = false

            // never transfer > min IV percentage (unless set to -1)
            if (ivPercentage < minIVPercentage || minIVPercentage == -1) {
                ivTooLow = true
            }
            // never transfer > min CP  (unless set to -1)
            if (this.cp < minCP || minCP == -1) {
                cpTooLow = true
            }
            reason = "CP < $minCP and IV < $minIVPercentage%"
            shouldRelease = ivTooLow && cpTooLow
        }
    }
    return Pair(shouldRelease, reason);
}