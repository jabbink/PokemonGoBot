/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper.util.directions

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.geometry.S1Angle
import com.google.common.geometry.S2LatLng
import com.google.maps.DirectionsApi
import com.google.maps.GeoApiContext
import com.google.maps.model.TravelMode
import ink.abb.pogo.scraper.Settings
import java.util.*
import java.util.regex.Pattern

enum class RouteProviderEnum {

    MAPZEN {
        override fun getRoute(startLat: Double, startLong: Double, endLat: Double, endLong: Double, geoApiContext: GeoApiContext): ArrayList<S2LatLng> {
            throw UnsupportedOperationException("not implemented")
        }

        override fun createURLString(startLat: Double, startLong: Double, endLat: Double, endLong: Double, apiKey: String): String {
            var url = "http://valhalla.mapzen.com/route?json={\"locations\":[{\"lat\":$startLat,\"lon\":$startLong},{\"lat\":$endLat,\"lon\":$endLong}],\"costing\":\"pedestrian\",\"directions_options\":{\"narrative\":\"false\"}}"
            if (apiKey.isNotBlank()) {
                url += "&api_key=$apiKey"
            }
            return url
        }

        override fun parseRouteResponse(routeParsed: String): ArrayList<S2LatLng> {

            val jsonRoot = ObjectMapper().readTree(routeParsed)
            val status = jsonRoot.path("trip").path("status").asInt()

            // status 0 == no problem
            if (status == 0) {
                val shape = jsonRoot.path("trip").findValue("shape").textValue()

                // Decode the route shape, look at https://mapzen.com/documentation/turn-by-turn/decoding/
                val precision: Double = 1E6
                val latlngList = ArrayList<S2LatLng>()
                var index: Int = 0
                var lat: Int = 0
                var lng: Int = 0
                while (index < shape.length) {
                    var b: Int
                    var shift = 0
                    var result = 0
                    do {
                        b = shape[index++].toInt() - 63
                        result = result or (b and 0x1f shl shift)
                        shift += 5
                    } while (b >= 0x20)
                    val endLat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
                    lat += endLat

                    shift = 0
                    result = 0
                    do {
                        b = shape[index++].toInt() - 63
                        result = result or (b and 0x1f shl shift)
                        shift += 5
                    } while (b >= 0x20)
                    val endLong = if (result and 1 != 0) (result shr 1).inv() else result shr 1
                    lng += endLong

                    latlngList.add(S2LatLng.fromDegrees(lat / precision, lng / precision))
                }
                return latlngList // everything is ok
            }
            return ArrayList() // can't parse
        }

        override fun getApiKey(settings: Settings): String {
            return settings.mapzenApiKey
        }
    },

    MOBROUTING {
        override fun getRoute(startLat: Double, startLong: Double, endLat: Double, endLong: Double, geoApiContext: GeoApiContext): ArrayList<S2LatLng> {
            throw UnsupportedOperationException("not implemented")
        }

        override fun createURLString(startLat: Double, startLong: Double, endLat: Double, endLong: Double, apiKey: String): String {
            return "http://mobrouting.com/api/dev/gosmore.php?flat=$startLat&flon=$startLong&tlat=$endLat&tlon=$endLong&v=foot&fast=1&layer=mapnik"
        }

        override fun parseRouteResponse(routeParsed: String): ArrayList<S2LatLng> {
            if (!routeParsed.contains("<distance>0</distance>")) {
                val matcher = Pattern.compile("(|-)\\d+.\\d+,(|-)\\d+.\\d+").matcher(routeParsed.split("<coordinates>")[1])
                val coordinatesList = ArrayList<String>()
                while (matcher.find()) {
                    coordinatesList.add(matcher.group())
                }
                val latlngList = ArrayList<S2LatLng>()
                coordinatesList.forEach {
                    latlngList.add(S2LatLng(S1Angle.degrees(it.toString().split(",")[1].toDouble()), S1Angle.degrees(it.toString().split(",")[0].toDouble())))
                }
                return latlngList // everything is ok
            }
            return ArrayList() // can't parse
        }

        override fun getApiKey(settings: Settings): String {
            return ""
        }
    },

    PROJECTOSM {
        override fun getRoute(startLat: Double, startLong: Double, endLat: Double, endLong: Double, geoApiContext: GeoApiContext): ArrayList<S2LatLng> {
            throw UnsupportedOperationException("not implemented")
        }

        override fun createURLString(startLat: Double, startLong: Double, endLat: Double, endLong: Double, apiKey: String): String {
            return "http://router.project-osrm.org/viaroute?loc=$startLat,$startLong&loc=$endLat,$endLong&compression=false"
        }

        override fun parseRouteResponse(routeParsed: String): ArrayList<S2LatLng> {
            if (routeParsed.contains("\"status\":200")) {
                val matcher = Pattern.compile("(|-)\\d+.\\d+,(|-)\\d+.\\d+").matcher(routeParsed.split("route_geometry")[1])
                val coordinatesList = ArrayList<String>()
                while (matcher.find()) {
                    coordinatesList.add(matcher.group())
                }
                val latlngList = ArrayList<S2LatLng>()
                coordinatesList.forEach {
                    latlngList.add(S2LatLng(S1Angle.degrees(it.toString().split(",")[0].toDouble()), S1Angle.degrees(it.toString().split(",")[1].toDouble())))
                }
                return latlngList // everything is ok
            }
            return ArrayList() // can't parse
        }

        override fun getApiKey(settings: Settings): String {
            return ""
        }
    },

    YOURNAVIGATION {
        override fun getRoute(startLat: Double, startLong: Double, endLat: Double, endLong: Double, geoApiContext: GeoApiContext): ArrayList<S2LatLng> {
            throw UnsupportedOperationException("not implemented")
        }

        override fun createURLString(startLat: Double, startLong: Double, endLat: Double, endLong: Double, apiKey: String): String {
            // change v=foot to v=bicycle, foot doesn't work atm, remove fast=1 (default value)
            return "http://yournavigation.org/api/dev/route.php?flat=$startLat&flon=$startLong&tlat=$endLat&tlon=$endLong&v=bicycle"
        }

        override fun parseRouteResponse(routeParsed: String): ArrayList<S2LatLng> {
            if (!routeParsed.contains("<distance>0</distance>") && !routeParsed.contains("Please try again later")) {
                val matcher = Pattern.compile("(|-)\\d+.\\d+,(|-)\\d+.\\d+").matcher(routeParsed.split("<coordinates>")[1])
                val coordinatesList = ArrayList<String>()
                while (matcher.find()) {
                    coordinatesList.add(matcher.group())
                }
                val latlngList = ArrayList<S2LatLng>()
                coordinatesList.forEach {
                    latlngList.add(S2LatLng(S1Angle.degrees(it.toString().split(",")[1].toDouble()), S1Angle.degrees(it.toString().split(",")[0].toDouble())))
                }
                return latlngList // everything is ok
            }
            return ArrayList() // can't parse
        }

        override fun getApiKey(settings: Settings): String {
            return ""
        }
    },

    GOOGLE {
        override fun getRoute(startLat: Double, startLong: Double, endLat: Double, endLong: Double, geoApiContext: GeoApiContext): ArrayList<S2LatLng> {
            try {
                val directionsRequest = DirectionsApi.getDirections(geoApiContext, "$startLat,$startLong", "$endLat,$endLong")
                directionsRequest.mode(TravelMode.WALKING)
                val directions = directionsRequest.await()
                val latlngList = ArrayList<S2LatLng>()
                directions.routes.forEach {
                    it.legs.forEach {
                        it.steps.forEach {
                            it.polyline.decodePath().forEach {
                                latlngList.add(S2LatLng(S1Angle.degrees(it.lat), S1Angle.degrees(it.lng)))
                            }
                        }
                    }
                }
                return latlngList
            } catch (e: Exception) {
                return ArrayList()
            }
        }

        override fun createURLString(startLat: Double, startLong: Double, endLat: Double, endLong: Double, apiKey: String): String {
            throw UnsupportedOperationException("not implemented")
        }

        override fun getApiKey(settings: Settings): String {
            return settings.googleApiKey
        }

        override fun parseRouteResponse(routeParsed: String): ArrayList<S2LatLng> {
            throw UnsupportedOperationException("not implemented")
        }

    };

    abstract fun createURLString(startLat: Double, startLong: Double, endLat: Double, endLong: Double, apiKey: String): String

    abstract fun parseRouteResponse(routeParsed: String): ArrayList<S2LatLng>

    abstract fun getApiKey(settings: Settings): String

    abstract fun getRoute(startLat: Double, startLong: Double, endLat: Double, endLong: Double, geoApiContext: GeoApiContext): ArrayList<S2LatLng>

    fun usingApiKey(settings: Settings): Boolean {
        return getApiKey(settings).isNotBlank()
    }

    /**
     * We ban a service provider for 1 minute
     * If it's still doesn't work, we double the time for every fail (1,2,4,8,16...)
     */
    fun banMe() {
        if (banTime == 0) {
            banTime = 1
        } else {
            banTime *= 2
        }
        banDate = Calendar.getInstance()
        banDate.add(Calendar.MINUTE, banTime)
    }

    fun isBanned(): Boolean {
        return Calendar.getInstance().before(banDate)
    }

    var lastTry: Calendar = Calendar.getInstance() // when we try to call this route provider last time?
    var banDate: Calendar = Calendar.getInstance() // when we can unban this route provider?
    var banTime: Int = 0 // how many time this route provider is banned (in minute)

}
