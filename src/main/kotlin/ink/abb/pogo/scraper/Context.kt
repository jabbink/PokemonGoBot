/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.util.concurrent.AtomicDouble
import com.pokegoapi.api.PokemonGo
import com.pokegoapi.api.player.PlayerProfile
import ink.abb.pogo.scraper.gui.SocketServer
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import com.pokegoapi.google.common.geometry.S2LatLng
import com.pokegoapi.google.common.geometry.S2CellId
import java.util.*

data class Context(
        val api: PokemonGo,
        val profile: PlayerProfile,
        val lat: AtomicDouble,
        val lng: AtomicDouble,

        val startXp: AtomicLong,
        val startTime: LocalDateTime,
        val pokemonStats: Pair<AtomicInteger, AtomicInteger>,
        val luredPokemonStats: AtomicInteger,
        val itemStats: Pair<AtomicInteger, AtomicInteger>,

        val blacklistedEncounters: MutableSet<Long>,
        val server: SocketServer,

        val pokemonInventoryFullStatus: Pair<AtomicBoolean, AtomicBoolean>,

        var restApiPassword: String,
        var s2Cache: MutableMap<String, Double>,
        var restApiToken: String = "",

        val walking: AtomicBoolean = AtomicBoolean(false),

        val pauseWalking: AtomicBoolean = AtomicBoolean(false)

) {
    fun getAltitude(latitude: Double, longitude: Double): Double {
        val rand = (Math.random() * 3) + 1
        val cellId = S2CellId.fromLatLng(S2LatLng.fromDegrees(latitude, longitude)).parent(15).id().toString()
        if (this.s2Cache.containsKey(cellId) && this.s2Cache[cellId] != null) {
            return this.s2Cache[cellId]!! + rand
        }
        var elevation = 10.0
        var url = HttpUrl.parse("https://maps.googleapis.com/maps/api/elevation/json?locations=$latitude,$longitude&sensor=true").newBuilder().build()
        var request = okhttp3.Request.Builder().url(url).build()
        try {
            val result: Map<*, *>
            result = ObjectMapper().readValue(OkHttpClient().newCall(request).execute().body().string(), Map::class.java)
            val results = result["results"] as List<*>
            val firstResult = results[0] as Map<*, *>
            elevation = firstResult["elevation"].toString().toDouble()
            this.s2Cache[cellId] = elevation
        } catch(ex: Exception) {
            url = HttpUrl.parse("https://elevation.mapzen.com/height?json={\"shape\":[{\"lat\":$latitude,\"lon\":$longitude}]}").newBuilder().build()
            request = okhttp3.Request.Builder().url(url).build()
            try {
                val result: Map<*, *>
                result = ObjectMapper().readValue(OkHttpClient().newCall(request).execute().body().string(), Map::class.java)
                elevation = result["height"].toString().replace("[^\\d\\-]".toRegex(), "").toDouble()
                this.s2Cache[cellId] = elevation
            } catch (exi: Exception) {
            }
        }
        return elevation + rand
    }
}