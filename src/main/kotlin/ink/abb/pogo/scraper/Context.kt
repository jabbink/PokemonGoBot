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
import okhttp3.Response
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

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
        var restApiToken: String = "",

        val walking: AtomicBoolean = AtomicBoolean(false),

        val pauseWalking: AtomicBoolean = AtomicBoolean(false)

) {
    fun getAltitude(latitude: Double?, longitude: Double?): Double {
        val url = HttpUrl.parse("https://elevation.mapzen.com/height?json={\"shape\":[{\"lat\":" + latitude.toString() + ",\"lon\":" + longitude.toString() + "}]}").newBuilder().build()
        val request = okhttp3.Request.Builder().url(url).build()
        var elevation = 10.0
        try {
            val result: Map<*,*>
            result = ObjectMapper().readValue(OkHttpClient().newCall(request).execute().body().string(), Map::class.java)
            elevation = java.lang.Double.parseDouble(result["height"].toString().replace("[^\\d\\-]".toRegex(), ""))
        } catch (ex: Exception) {
        }
        return elevation
    }
}