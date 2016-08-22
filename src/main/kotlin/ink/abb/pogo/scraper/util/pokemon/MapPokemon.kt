/*
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper.util.pokemon

import POGOProtos.Data.Capture.CaptureProbabilityOuterClass
import POGOProtos.Inventory.Item.ItemIdOuterClass
import com.google.common.geometry.S2LatLng
import ink.abb.pogo.api.cache.MapPokemon
import ink.abb.pogo.api.request.Encounter
import rx.Observable
import java.util.concurrent.atomic.AtomicInteger

fun MapPokemon.encounter(): Observable<Encounter> {
    val encounter = Encounter().withEncounterId(encounterId).withSpawnPointId(spawnPointId)
    return poGoApi.queueRequest(encounter)
}

val MapPokemon.inRange: Boolean
    get() {
        return distance < poGoApi.mapSettings.encounterRangeMeters
    }


val MapPokemon.distance: Double
    get() {
        val playerLocation = S2LatLng.fromDegrees(poGoApi.latitude, poGoApi.longitude)
        val fortLocation = S2LatLng.fromDegrees(latitude, longitude)
        return playerLocation.getEarthDistance(fortLocation)
    }