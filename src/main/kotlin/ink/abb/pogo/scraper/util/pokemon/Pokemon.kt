/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper.util.pokemon

import POGOProtos.Data.PokemonDataOuterClass
import POGOProtos.Data.PokemonDataOuterClass.PokemonData
import com.pokegoapi.api.pokemon.Pokemon

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
