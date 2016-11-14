/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper.util.directions

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.geometry.S2CellId
import com.google.common.geometry.S2LatLng
import com.google.maps.GeoApiContext
import com.squareup.okhttp.HttpUrl
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import ink.abb.pogo.scraper.Context
import ink.abb.pogo.scraper.Settings
import ink.abb.pogo.scraper.util.Log
import java.util.*

val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.106 Safari/537.36"

fun getRouteCoordinates(startLat: Double, startLong: Double, endLat: Double, endLong: Double, settings: Settings, geoApiContext: GeoApiContext): ArrayList<S2LatLng> {
    for (routeProvider in settings.followStreets) {
        if (routeProvider.isBanned()) {
            continue
        }
        var error: String?
        try {
            if (routeProvider == RouteProviderEnum.GOOGLE) {
                return routeProvider.getRoute(startLat, startLong, endLat, endLong, geoApiContext)
            } else {
                val url = routeProvider.createURLString(startLat, startLong, endLat, endLong, routeProvider.getApiKey(settings))
                val request = Request.Builder().url(url).header("User-Agent", userAgent).build()
                val response = OkHttpClient().newCall(request).execute()
                val responseBody = response.body().string()
                if (responseBody.length > 0) {
                    val coordinates = routeProvider.parseRouteResponse(responseBody)
                    if (coordinates.isNotEmpty()) {
                        routeProvider.banTime = 0 // everything is ok, reset the bantime
                        Log.normal("[Route] Got route coordinates from $routeProvider (API KEY: ${routeProvider.usingApiKey(settings)})")
                        return coordinates
                    }
                }
            }
            error = "response is not valid or empty"
        } catch (e: Exception) {
            error = e.message
        } finally {
            routeProvider.lastTry = Calendar.getInstance()
        }
        routeProvider.banMe()
        Log.red("[Route] Error from $routeProvider: $error (banned for ${routeProvider.banTime} min) (API KEY: ${routeProvider.usingApiKey(settings)})")
    }
    Log.red("[Route] No more route providers, go directly to pokestops/waypoints")
    return ArrayList()
}

fun getRouteCoordinates(start: S2LatLng, end: S2LatLng, settings: Settings, geoApiContext: GeoApiContext): ArrayList<S2LatLng> {
    return getRouteCoordinates(start.latDegrees(), start.lngDegrees(), end.latDegrees(), end.lngDegrees(), settings, geoApiContext)
}

fun isValidRouteProvider(routeName: String): Boolean {
    try {
        RouteProviderEnum.valueOf(routeName)
        return true
    } catch (e: IllegalArgumentException) {
        return false
    }
}

fun getAltitude(latitude: Double, longitude: Double, ctx: Context): Double {
    val rand = (Math.random() * 3) + 1
    val cellId = S2CellId.fromLatLng(S2LatLng.fromDegrees(latitude, longitude)).parent(15).id().toString()
    var elevation = 10.0
    var foundEle = false

    if (ctx.s2Cache.containsKey(cellId) && ctx.s2Cache[cellId] != null) {
        return ctx.s2Cache[cellId]!! + rand
    }

    try {
        val url = HttpUrl.parse("https://maps.googleapis.com/maps/api/elevation/json?locations=$latitude,$longitude&sensor=true").newBuilder().build()
        val request = Request.Builder().url(url).build()
        val result: Map<*, *>
        result = ObjectMapper().readValue(OkHttpClient().newCall(request).execute().body().string(), Map::class.java)
        val results = result["results"] as List<*>
        val firstResult = results[0] as Map<*, *>
        elevation = firstResult["elevation"].toString().toDouble()
        foundEle = true
        ctx.s2Cache[cellId] = elevation
    } catch(ex: Exception) {
        val url = HttpUrl.parse("https://elevation.mapzen.com/height?json={\"shape\":[{\"lat\":$latitude,\"lon\":$longitude}]}").newBuilder().build()
        val request = Request.Builder().url(url).build()

        try {
            val result: Map<*, *>
            result = ObjectMapper().readValue(OkHttpClient().newCall(request).execute().body().string(), Map::class.java)
            elevation = result["height"].toString().replace("[^\\d\\-]".toRegex(), "").toDouble()
            foundEle = true
            ctx.s2Cache[cellId] = elevation
        } catch (exi: Exception) {
            Log.red("Can't get elevation, using ${elevation + rand}...")
        }
    }

    if (foundEle) {
        val inp = java.io.RandomAccessFile("altitude_cache.json", "rw")
        try {
            val lock = inp.channel.lock()
            try {
                var altitudeReloadStr = ""
                val by = ByteArray(inp.length().toInt())
                inp.readFully(by)
                for (byt in by) {
                    altitudeReloadStr += byt.toChar().toString()
                }
                inp.setLength(0)
                val altitudeReload: MutableMap<String, Double> =
                        try {
                            @Suppress("UNCHECKED_CAST")
                            (ObjectMapper().readValue(altitudeReloadStr, MutableMap::class.java) as MutableMap<String, Double>)
                        } catch (ex: Exception) {
                            mutableMapOf()
                        }
                for ((s2CellId, ele) in altitudeReload) {
                    ctx.s2Cache[s2CellId] = ele
                }
                Log.normal("Saving altitude cache file...")
                ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(inp, ctx.s2Cache)
            } finally {
                lock.release()
            }
        } finally {
            inp.close()
        }
    }

    return elevation + rand
}
