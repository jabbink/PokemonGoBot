/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper.util.map

import com.google.common.geometry.S2LatLng
import ink.abb.pogo.api.cache.Fort
import ink.abb.pogo.api.cache.Pokestop
import ink.abb.pogo.api.request.FortSearch
import rx.Observable

fun Pokestop.canLoot(ignoreDistance: Boolean = false, lootTimeouts: Map<String, Long>): Boolean {
    val canLoot = lootTimeouts.getOrElse(id, { cooldownCompleteTimestampMs }) < poGoApi.currentTimeMillis()
    return (ignoreDistance || inRange(poGoApi.fortSettings.interactionRangeMeters)) && canLoot
}

fun Pokestop.inRange(maxDistance: Double): Boolean {
    return distance < maxDistance
}

fun Pokestop.inRangeForLuredPokemon(): Boolean {
    return distance < poGoApi.mapSettings.encounterRangeMeters
}

fun Pokestop.loot(): Observable<FortSearch> {
    val loot: FortSearch = FortSearch().withFortId(fortData.id).withFortLatitude(fortData.latitude).withFortLongitude(fortData.longitude)
    return poGoApi.queueRequest(loot)
}

val Fort.distance: Double
    get() {
        val playerLocation = S2LatLng.fromDegrees(poGoApi.latitude, poGoApi.longitude)
        val fortLocation = S2LatLng.fromDegrees(fortData.latitude, fortData.longitude)
        return playerLocation.getEarthDistance(fortLocation)
    }
