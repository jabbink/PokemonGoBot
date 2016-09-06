/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper.util.pokemon

import com.google.common.geometry.S2LatLng
import ink.abb.pogo.api.cache.MapPokemon
import ink.abb.pogo.api.network.ServerRequest
import ink.abb.pogo.api.request.DiskEncounter
import ink.abb.pogo.api.request.Encounter
import rx.Observable

fun MapPokemon.encounter(): Observable<ServerRequest> {
    val encounter: ServerRequest? =
            if (encounterKind == MapPokemon.EncounterKind.NORMAL) {
                Encounter().withEncounterId(encounterId).withSpawnPointId(spawnPointId)
            } else if (encounterKind == MapPokemon.EncounterKind.DISK) {
                DiskEncounter().withEncounterId(encounterId).withFortId(spawnPointId)
            } else {
                null
            }
    return poGoApi.queueRequest(encounter!!)
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
