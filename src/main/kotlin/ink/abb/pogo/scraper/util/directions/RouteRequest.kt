package ink.abb.pogo.scraper.util.directions

import com.fasterxml.jackson.databind.ObjectMapper
import com.pokegoapi.google.common.geometry.S2LatLng
import ink.abb.pogo.scraper.util.Log
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

//var routeProvider = "http://yournavigation.org/api/dev/route.php"
//var routeProvider = "http://router.project-osrm.org/viaroute"
//var routeProvider = "http://mobrouting.com/api/dev/gosmore.php"
val routeProvider = "http://valhalla.mapzen.com/route?json="


fun getRoutefile(olat: Double, olng: Double, dlat: Double, dlng: Double): String {
    val connection = URL(createURLString(olat, olng, dlat, dlng)).openConnection() as HttpURLConnection
    connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
    connection.setRequestProperty("Accept-Language", "en")
    connection.setRequestProperty("Cache-Control", "max=0")
    connection.setRequestProperty("Connection", "keep-alive")
    connection.setRequestProperty("DNT", "1")
    connection.setRequestProperty("Host", "valhalla.mapzen.com")
    connection.setRequestProperty("Upgrade-Insecure-Requests", "1")
    connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.106 Safari/537.36")
    var routeFile = String()
    try {
        connection.inputStream.bufferedReader().lines().forEach {
            routeFile += "$it\n"
        }
    } catch (e: Exception) {
        Log.red("Error fetching route from provider: " + e.message)
    }
    return routeFile
}


fun createURLString(olat: Double, olng: Double, dlat: Double, dlng: Double): String {
    //return "$routeProvider?flat=$olat&flon=$olng&tlat=$dlat&tlon=$dlng&v=foot&fast=1"
    //return "$routeProvider?flat=$olat&flon=$olng&tlat=$dlat&tlon=$dlng&v=foot&fast=1&layer=mapnik" // used for mobrouting.com
    return routeProvider + "{\"locations\":[{\"lat\":$olat,\"lon\":$olng},{\"lat\":$dlat,\"lon\":$dlng}],\"costing\":\"pedestrian\",\"directions_options\":{\"narrative\":\"false\"}}"
}


//Keep this, used for mobrouting.com
/*
fun getRouteCoordinates(olat: Double, olng: Double, dlat: Double, dlng: Double): ArrayList<S2LatLng> {
    var routeParsed = getRoutefile(olat, olng, dlat, dlng)
    if (routeParsed.length > 0 && !routeParsed.contains("<distance>0</distance>")) {
        routeParsed = routeParsed.split("<coordinates>")[1]
        val matcher = Pattern.compile("(|-)\\d+.\\d+,(|-)\\d+.\\d+").matcher(routeParsed)
        val coordinatesList = ArrayList<String>()
        while (matcher.find()) {
            coordinatesList.add(matcher.group())
        }
        val latlngList = ArrayList<S2LatLng>()
        coordinatesList.forEach {
            latlngList.add(S2LatLng(S1Angle.degrees(it.toString().split(",")[1].toDouble()), S1Angle.degrees(it.toString().split(",")[0].toDouble())))
        }
        return latlngList
    } else {
        return ArrayList()
    }
}
*/

fun getRouteCoordinates(olat: Double, olng: Double, dlat: Double, dlng: Double): ArrayList<S2LatLng> {
    var routeParsed = getRoutefile(olat, olng, dlat, dlng)

    if (routeParsed.length > 0) {

        val jsonRoot = ObjectMapper().readTree(routeParsed)
        val status = jsonRoot.path("trip").path("status").asInt()

        // status 0 == no problem
        if (status == 0) {
            val shape = jsonRoot.path("trip").findValue("shape").textValue()

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
                val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
                lat += dlat

                shift = 0
                result = 0
                do {
                    b = shape[index++].toInt() - 63
                    result = result or (b and 0x1f shl shift)
                    shift += 5
                } while (b >= 0x20)
                val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
                lng += dlng

                latlngList.add(S2LatLng.fromDegrees(lat / precision, lng / precision))
            }
            // everything is ok
            return latlngList
        }
    }
    // can't parse
    return ArrayList()
}


//Keep this in case yournavigation.org goes down
/*fun getRouteCoordinates(olat: Double, olng: Double, dlat: Double, dlng: Double): ArrayList<S2LatLng> {
    var route = getRoutefile(olat, olng, dlat, dlng)
    if (route.length > 0 && route.contains("\"status\":200")) {
        route = route.split("route_geometry")[1]
        val matcher = Pattern.compile("(|-)\\d+.\\d+,(|-)\\d+.\\d+").matcher(route)
        val coordinatesList = ArrayList<String>()
        while (matcher.find()) {
            coordinatesList.add(matcher.group())
        }
        val latlngList = ArrayList<S2LatLng>()
        coordinatesList.forEach {
            latlngList.add(S2LatLng(S1Angle.degrees(it.toString().split(",")[0].toDouble()), S1Angle.degrees(it.toString().split(",")[1].toDouble())))
        }
        return latlngList
    } else {
        return ArrayList()
    }
}*/


fun getRouteCoordinates(start: S2LatLng, end: S2LatLng): ArrayList<S2LatLng> {
    return getRouteCoordinates(start.latDegrees(), start.lngDegrees(), end.latDegrees(), end.lngDegrees())
}
