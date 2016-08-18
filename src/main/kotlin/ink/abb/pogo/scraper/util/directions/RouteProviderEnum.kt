package ink.abb.pogo.scraper.util.directions

import com.fasterxml.jackson.databind.ObjectMapper
import com.pokegoapi.google.common.geometry.S1Angle
import com.pokegoapi.google.common.geometry.S2LatLng
import java.util.*
import java.util.regex.Pattern


enum class RouteProviderEnum {

    MAPZEN {
        override fun createURLString(startLat: Double, startLong: Double, endLat: Double, endLong: Double): String {
            return "http://valhalla.mapzen.com/route?json={\"locations\":[{\"lat\":$startLat,\"lon\":$startLong},{\"lat\":$endLat,\"lon\":$endLong}],\"costing\":\"pedestrian\",\"directions_options\":{\"narrative\":\"false\"}}"
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
    },

    MOBROUTING {
        override fun createURLString(startLat: Double, startLong: Double, endLat: Double, endLong: Double): String {
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
    },

    PROJECTOSM {
        override fun createURLString(startLat: Double, startLong: Double, endLat: Double, endLong: Double): String {
            return "http://router.project-osrm.org/viaroute?flat=$startLat&flon=$startLong&tlat=$endLat&tlon=$endLong&v=foot&fast=1"
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
            } else {
                return ArrayList() // can't parse
            }
        }
    },

    YOURNAVIGATION {
        override fun createURLString(startLat: Double, startLong: Double, endLat: Double, endLong: Double): String {
            return "http://yournavigation.org/api/dev/route.php?flat=$startLat&flon=$startLong&tlat=$endLat&tlon=$endLong&v=foot&fast=1"
        }

        override fun parseRouteResponse(routeParsed: String): ArrayList<S2LatLng> {
            if (!routeParsed.contains("<distance>0</distance>") && !routeParsed.contains("Please try again later")) {
                val matcher = Pattern.compile("(|-)\\d+.(|-)\\d+,(|-)\\d+.(|-)\\d+").matcher(routeParsed.split("<coordinates>")[1])
                val coordinatesList = ArrayList<String>()
                while (matcher.find()) {
                    coordinatesList.add(matcher.group())
                }
                val latlngList = ArrayList<S2LatLng>()
                coordinatesList.forEach {
                    latlngList.add(S2LatLng(S1Angle.degrees(it.toString().split(",")[1].toDouble()), S1Angle.degrees(it.toString().split(",")[0].toDouble())))
                }
                return latlngList // everything is ok
            } else {
                return ArrayList() // can't parse
            }
        }
    };

    abstract fun createURLString(startLat: Double, startLong: Double, endLat: Double, endLong: Double): String

    abstract fun parseRouteResponse(routeParsed: String): ArrayList<S2LatLng>

}
