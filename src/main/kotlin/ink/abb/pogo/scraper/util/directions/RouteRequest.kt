package ink.abb.pogo.scraper.util.directions

import com.pokegoapi.google.common.geometry.S1Angle
import com.pokegoapi.google.common.geometry.S2LatLng
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import java.util.regex.Pattern
import java.util.zip.GZIPInputStream

//var routeProvider = "http://yournavigation.org/api/dev/route.php"
var routeProvider = "http://router.project-osrm.org/viaroute"


fun getRoutefile(olat: Double, olng: Double, dlat: Double, dlng: Double): String {
    val connection = URL(createURLString(olat, olng, dlat, dlng)).openConnection() as HttpURLConnection
    connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
    //connection.setRequestProperty("Accept-Encoding", "gzip")
    connection.setRequestProperty("Accept-Language", "en")
    connection.setRequestProperty("Cache-Control", "max=0")
    connection.setRequestProperty("Connection", "keep-alive")
    connection.setRequestProperty("DNT", "1")
    connection.setRequestProperty("Host", "router.project-osrm.org")
    connection.setRequestProperty("Upgrade-Insecure-Requests", "1")
    connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.106 Safari/537.36")
    var routeFile = String()
    connection.inputStream.bufferedReader().lines().forEach {
        routeFile += "$it\n"
    }
    return routeFile
}


fun createURLString(olat: Double, olng: Double, dlat: Double, dlng: Double): String {
    //return "$routeProvider?flat=$olat&flon=$olng&tlat=$dlat&tlon=$dlng&v=foot&fast=1"
    return "$routeProvider?loc=$olat,$olng&loc=$dlat,$dlng&compression=false"
}

fun getRouteCoordinates(olat: Double, olng: Double, dlat: Double, dlng: Double): ArrayList<S2LatLng> {
    val routeJSONParsed = JSONObject(getRoutefile(olat, olng, dlat, dlng))
    var coordinates = routeJSONParsed.get("route_geometry").toString()
    coordinates = coordinates.replace("[", "")
    coordinates = coordinates.replace("]", "")
    val matcher = Pattern.compile("(|-)\\d+.(|-)\\d+,(|-)\\d+.(|-)\\d+").matcher(coordinates)
    val coordinatesList = ArrayList<String>()
    while (matcher.find()) {
        coordinatesList.add(matcher.group())
    }
    val latlngList = ArrayList<S2LatLng>()
    coordinatesList.forEach {
        latlngList.add(S2LatLng(S1Angle.degrees(it.toString().split(",")[0].toDouble()), S1Angle.degrees(it.toString().split(",")[1].toDouble())))
    }
    return latlngList
}

fun getRouteCoordinates(start: S2LatLng, end: S2LatLng): ArrayList<S2LatLng> {
    return getRouteCoordinates(start.latDegrees(),start.lngDegrees(),end.latDegrees(),end.lngDegrees())
}
