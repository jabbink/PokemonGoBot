package ink.abb.pogo.scraper.util.directions

import com.pokegoapi.google.common.geometry.S2LatLng
import ink.abb.pogo.scraper.Settings
import ink.abb.pogo.scraper.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.*

fun getRouteCoordinates(startLat: Double, startLong: Double, endLat: Double, endLong: Double, settings: Settings): ArrayList<S2LatLng> {
    val routeProvider = settings.followStreetsProvider
    try {
        val url = routeProvider.createURLString(startLat, startLong, endLat, endLong)
        val request = Request.Builder().url(url).build()
        val response = OkHttpClient().newCall(request).execute()
        val responseBody = response.body().string()
        if (responseBody.length > 0) {
            val coordinates = routeProvider.parseRouteResponse(responseBody)
            if (coordinates.isNotEmpty()) {
                return coordinates
            }
        }
        Log.red("Error fetching route from $routeProvider: response is empty")
    } catch (e: Exception) {
        Log.red("Error fetching route from $routeProvider: ${e.message}")
    }
    return ArrayList()
}

fun getRouteCoordinates(start: S2LatLng, end: S2LatLng, settings: Settings): ArrayList<S2LatLng> {
    return getRouteCoordinates(start.latDegrees(), start.lngDegrees(), end.latDegrees(), end.lngDegrees(), settings)
}