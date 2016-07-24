package ink.abb.pogo.scraper.util.directions

import java.net.HttpURLConnection
import java.net.URL


var KMLprovider = "http://yournavigation.org/api/dev/route.php"

fun getKMLfile(olat: Double, olng: Double, dlat: Double, dlng: Double): String {
    val connection = URL(createURLString(olat, olng, dlat, dlng)).openConnection() as HttpURLConnection
    connection.setRequestProperty("Accept","text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
    //connection.setRequestProperty("Accept-Encoding","gzip, deflate, sdch")
    connection.setRequestProperty("Accept-Language","en")
    connection.setRequestProperty("Cache-Control","max=0")
    connection.setRequestProperty("Connection","keep-alive")
    connection.setRequestProperty("DNT","1")
    connection.setRequestProperty("Host","yournavigation.org")
    connection.setRequestProperty("Upgrade-Insecure-Requests","1")
    connection.setRequestProperty("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.106 Safari/537.36")
    var kmlfile = String()
    connection.inputStream.bufferedReader().use {
        for (lines in it.lines()){
            kmlfile += "$lines\n"
        }
    }
    return kmlfile
}

fun createURLString(olat: Double, olng: Double, dlat: Double, dlng: Double): String {
    return "$KMLprovider?flat=$olat&flon=$olng&tlat=$dlat&tlon=$dlng&v=foot&fast=1"
}

//test
fun main(args: Array<String>) {
    val s = getKMLfile(11.036610, 44.873847, 11.035022, 44.874022)
    println(s)
}
