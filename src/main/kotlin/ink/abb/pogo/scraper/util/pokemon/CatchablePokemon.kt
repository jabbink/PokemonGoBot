/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper.util.pokemon

import POGOProtos.Data.Capture.CaptureProbabilityOuterClass.CaptureProbability
import POGOProtos.Inventory.Item.ItemIdOuterClass.ItemId
import POGOProtos.Networking.Responses.CatchPokemonResponseOuterClass
import POGOProtos.Networking.Responses.CatchPokemonResponseOuterClass.CatchPokemonResponse.CatchStatus
import com.pokegoapi.api.inventory.ItemBag
import com.pokegoapi.api.inventory.Pokeball
import com.pokegoapi.api.map.pokemon.CatchResult
import com.pokegoapi.api.map.pokemon.CatchablePokemon
import ink.abb.pogo.scraper.util.Log

/**
 * Extension function to make the code more readable in the CatchOneNearbyPokemon task
 */
fun CatchablePokemon.catch(normalizedHitPosition: Double = 1.0,
                           normalizedReticleSize: Double = 1.95 + Math.random() * 0.05,
                           spinModifier: Double = 0.85 + Math.random() * 0.15,
                           ballType: Pokeball? = Pokeball.POKEBALL, amount: Int = -1, razzBerryAmount: Int = -1): CatchResult? {
    return this.catchPokemon(normalizedHitPosition, normalizedReticleSize, spinModifier, ballType, amount, razzBerryAmount)
}

// unfortunately necessary because of the shitty `Pokeball` class...
val itemToPokeball = mapOf(
        Pair(ItemId.ITEM_POKE_BALL, Pokeball.POKEBALL),
        Pair(ItemId.ITEM_GREAT_BALL, Pokeball.GREATBALL),
        Pair(ItemId.ITEM_ULTRA_BALL, Pokeball.ULTRABALL),
        Pair(ItemId.ITEM_MASTER_BALL, Pokeball.MASTERBALL)
)

fun CatchablePokemon.catch(captureProbability: CaptureProbability, itemBag: ItemBag, desiredCatchProbability: Double, amount: Int): CatchResult? {
    var result: CatchResult?
    var numThrows = 0
    do {
        result = catch(captureProbability, itemBag, desiredCatchProbability)

        if (result != null && result.getStatus() != CatchStatus.CATCH_ESCAPE && result.getStatus() != CatchStatus.CATCH_MISSED) {
            break
        }
        numThrows++
    } while (amount < 0 || numThrows < amount)

    return result
}

fun CatchablePokemon.catch(captureProbability: CaptureProbability, itemBag: ItemBag, desiredCatchProbability: Double): CatchResult? {
    val ballTypes = captureProbability.pokeballTypeList
    val probabilities = captureProbability.captureProbabilityList
    var ball: ItemId? = null
    var needCurve = false
    var needRazzBerry = false
    var highestAvailable: ItemId? = null
    for ((index, ballType) in ballTypes.withIndex()) {
        val probability = probabilities.get(index)
        val ballAmount = itemBag.getItem(ballType).count
        if (ballAmount == 0) {
            continue;
        } else {
            highestAvailable = ballType
        }
        if (probability >= desiredCatchProbability) {
            ball = ballType
            break
        } else if (probability >= desiredCatchProbability - 0.1) {
            ball = ballType
            needCurve = true
            break
        } else if (probability >= desiredCatchProbability - 0.2) {
            ball = ballType
            needCurve = true
            needRazzBerry = true
            break
        }
    }

    if (highestAvailable == null) {
        Log.red("No balls available")
        return null
    }

    if (ball == null) {
        ball = highestAvailable
        needCurve = true
        needRazzBerry = true
    }

    var logMessage = "Using ${ball.name}"
    itemBag.getItem(ball).count--

    val razzBerryCount = itemBag.getItem(ItemId.ITEM_RAZZ_BERRY).count
    if (razzBerryCount > 0 && needRazzBerry) {
        logMessage += "; Using Razz Berry"
        useItem(ItemId.ITEM_RAZZ_BERRY)
        itemBag.getItem(ItemId.ITEM_RAZZ_BERRY).count--
    }
    if (needCurve) {
        logMessage += "; Using curve"
    }
    Log.yellow(logMessage)
    return catch(
            normalizedHitPosition = 1.0,
            normalizedReticleSize = 1.95 + Math.random() * 0.05,
            spinModifier = if (needCurve) 0.85 + Math.random() * 0.15 else Math.random() * 0.10,
            ballType = itemToPokeball.get(ball),
            amount = 0,
            razzBerryAmount = 0
    )
}
