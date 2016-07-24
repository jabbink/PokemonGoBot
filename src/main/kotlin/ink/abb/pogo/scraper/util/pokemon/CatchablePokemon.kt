/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper.util.pokemon

import com.pokegoapi.api.inventory.Pokeball
import com.pokegoapi.api.map.pokemon.CatchResult
import com.pokegoapi.api.map.pokemon.CatchablePokemon

/**
 * Extension function to make the code more readable in the CatchOneNearbyPokemon task
 */
fun CatchablePokemon.catch(normalizedHitPosition: Double = 1.0,
                           normalizedReticleSize: Double = 1.95 + Math.random() * 0.05,
                           spinModifier: Double = 0.85 + Math.random() * 0.15,
                           ballType: Pokeball? = Pokeball.POKEBALL, amount: Int = -1): CatchResult? {
    return this.catchPokemon(normalizedHitPosition, normalizedReticleSize, spinModifier, ballType, amount)
}