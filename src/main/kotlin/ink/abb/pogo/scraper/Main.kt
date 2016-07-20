package ink.abb.pogo.scraper

import POGOProtos.Inventory.ItemIdOuterClass
import POGOProtos.Map.Fort.FortDataOuterClass
import POGOProtos.Networking.Responses.CatchPokemonResponseOuterClass
import POGOProtos.Networking.Responses.EncounterResponseOuterClass
import POGOProtos.Networking.Responses.FortSearchResponseOuterClass
import com.google.common.geometry.S2LatLng
import com.google.common.util.concurrent.AtomicDouble
import com.pokegoapi.api.PokemonGo
import com.pokegoapi.api.map.MapObjects
import com.pokegoapi.api.player.PlayerProfile
import com.pokegoapi.auth.PTCLogin
import com.pokegoapi.exceptions.NoSuchItemException
import okhttp3.OkHttpClient
import java.net.InetSocketAddress
import java.net.Proxy
import java.security.cert.X509Certificate
import java.util.*
import java.util.concurrent.TimeUnit
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSession
import javax.net.ssl.TrustManager
import javax.security.cert.CertificateException
import kotlin.concurrent.fixedRateTimer
import kotlin.concurrent.thread

/**
 * Allow all certificate to debug with https://github.com/bettse/mitmdump_decoder
 */
fun allowProxy(builder: OkHttpClient.Builder) {
    builder.proxy(Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved("localhost", 8888)))
    val trustAllCerts = arrayOf<TrustManager>(object : javax.net.ssl.X509TrustManager {
        override fun getAcceptedIssuers(): Array<out X509Certificate> {
            return emptyArray()
        }

        @Throws(CertificateException::class)
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
        }

        @Throws(CertificateException::class)
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
        }
    })

    // Install the all-trusting trust manager
    val sslContext = SSLContext.getInstance("SSL")
    sslContext.init(null, trustAllCerts, java.security.SecureRandom())
    // Create an ssl socket factory with our all-trusting manager
    val sslSocketFactory = sslContext.getSocketFactory()

    builder.sslSocketFactory(sslSocketFactory);
    builder.hostnameVerifier(object : HostnameVerifier {
        override fun verify(hostname: String, session: SSLSession): Boolean {
            return true
        }
    })
}

fun main(args: Array<String>) {
    val builder = OkHttpClient.Builder()
    // allowProxy(builder)
    builder.connectTimeout(60, TimeUnit.SECONDS)
    builder.readTimeout(60, TimeUnit.SECONDS)
    builder.writeTimeout(60, TimeUnit.SECONDS)
    val http = builder.build()

    val properties = Properties()

    Thread.currentThread().contextClassLoader.getResourceAsStream("config.properties").use {
        properties.load(it)
    }
    lat.set(properties.getProperty("latitude").toDouble())
    lng.set(properties.getProperty("longitude").toDouble())
    val auth = (PTCLogin(http).login(properties.getProperty("username"), properties.getProperty("password")))

    val go = PokemonGo(auth, http)
    while (profile == null) {
        profile = go.playerProfile
        Thread.sleep(1000)
    }
    println("Logged in as ${profile!!.username}")
    println("Pokecoin: ${profile!!.currencies.get(PlayerProfile.Currency.POKECOIN)}")
    println("Stardust: ${profile!!.currencies.get(PlayerProfile.Currency.STARDUST)}")
    println("Level ${profile!!.stats.level}, Experience ${profile!!.stats.experience}")

    go.setLocation(lat.get() + randomLatLng(), lng.get() + randomLatLng(), 0.0)
    println("Getting map of ${lat.get()} ${lng.get()}")


    var reply = go.map.mapObjects

    processMapObjects(go, reply)
    fixedRateTimer("GetMapObjects", false, 5000, 5000, action = {
        thread(block = {
            // query a small area to keep alive
            println("Getting map of ${lat.get()} ${lng.get()}")
            reply = go.map.getMapObjects(0)
            processMapObjects(go, reply)
        })
    })
}

fun randomLatLng(): Double {
    return Math.random() * 0.0001 - 0.00005
}

val speed = 2.778 * 5

var walking = false

val lat = AtomicDouble()
val lng = AtomicDouble()
var profile: PlayerProfile? = null

fun walk(end: S2LatLng, speed: Double) {
    if (walking) {
        return
    }
    walking = true
    val start = S2LatLng.fromDegrees(lat.get(), lng.get())
    val diff = end.sub(start)
    val distance = start.getEarthDistance(end)
    val timeout = 200L
    val timeRequired = distance / speed
    val stepsRequired = timeRequired / (timeout.toDouble() / 1000.toDouble())
    val deltaLat = diff.latDegrees() / stepsRequired
    val deltaLng = diff.lngDegrees() / stepsRequired

    var remainingSteps = stepsRequired


    fixedRateTimer("Walk", false, 0, timeout, action = {
        lat.addAndGet(deltaLat)
        lng.addAndGet(deltaLng)
        remainingSteps--
        if (remainingSteps <= 0) {
            println("destination reached")
            walking = false
            cancel()
        }
    })
}

var usedPokestops = HashMap<String, Long>()

var pokestops: Collection<FortDataOuterClass.FortData>? = null

fun processMapObjects(api: PokemonGo, mapObjects: MapObjects?) {
    if ((pokestops != null && pokestops!!.size < mapObjects?.pokestops?.size ?: 0) || (pokestops == null && mapObjects != null && mapObjects.pokestops != null)) {
        pokestops = mapObjects?.pokestops
    }

    if (mapObjects != null) {
        if (mapObjects.catchablePokemons.size > 0) {
            val catchablePokemon = mapObjects.catchablePokemons.first()
            // TODO: Fix this mess
            var normalBalls = 0
            /*var greatBalls = 0
            var ultraBalls = 0
            var masterBalls = 0*/
            try {
                normalBalls = api.bag.getItem(ItemIdOuterClass.ItemId.ITEM_POKE_BALL).count
            } catch (e: NoSuchItemException) {
            }
            /*try {
                greatBalls = api.bag.getItem(ItemIdOuterClass.ItemId.ITEM_GREAT_BALL).count
            } catch (e: NoSuchItemException) {
            }
            try {
                ultraBalls = api.bag.getItem(ItemIdOuterClass.ItemId.ITEM_ULTRA_BALL).count
            } catch (e: NoSuchItemException) {
            }
            try {
                masterBalls = api.bag.getItem(ItemIdOuterClass.ItemId.ITEM_MASTER_BALL).count
            } catch (e: NoSuchItemException) {
            }*/
            //println("normalBalls: ${normalBalls}, greatBalls: ${greatBalls}, ultraBalls: ${ultraBalls}, masterBalls: ${masterBalls}")
            if (normalBalls > 0) {
                println("encountering pokemon ${catchablePokemon.pokemonId}")
                api.setLocation(lat.get(), lng.get(), 0.0)
                val encounterResult = api.map.encounterPokemon(catchablePokemon)
                if (encounterResult.status == EncounterResponseOuterClass.EncounterResponse.Status.ENCOUNTER_SUCCESS) {
                    val result = api.map.catchPokemon(catchablePokemon, 1.0, 1.95 + Math.random() * 0.05, 0.85 + Math.random() * 0.15, ItemIdOuterClass.ItemId.ITEM_POKE_BALL_VALUE)
                    if (result.status == CatchPokemonResponseOuterClass.CatchPokemonResponse.CatchStatus.CATCH_SUCCESS) {
                        println("Caught a ${catchablePokemon.pokemonId}")
                    }
                }
                return
            }
        }
        val sortedPokestops = pokestops?.sortedWith(Comparator { a, b ->
            val locationA = S2LatLng.fromDegrees(a.latitude, a.longitude)
            val locationB = S2LatLng.fromDegrees(b.latitude, b.longitude)
            var self = S2LatLng.fromDegrees(lat.get(), lng.get())
            val distanceA = self.getEarthDistance(locationA)
            val distanceB = self.getEarthDistance(locationB)
            distanceA.compareTo(distanceB)
        })!!

        val nearbyPokestops = sortedPokestops.filter {
            val location = S2LatLng.fromDegrees(it.latitude, it.longitude)
            var self = S2LatLng.fromDegrees(lat.get(), lng.get())
            val distance = self.getEarthDistance(location)
            distance < 30 && usedPokestops.getOrElse(it.id, { 0 }) < System.currentTimeMillis()
        }
        val nearestUnused = sortedPokestops.filter {
            usedPokestops.getOrElse(it.id, { 0 }) < System.currentTimeMillis()
        }.first()
        walk(S2LatLng.fromDegrees(nearestUnused.latitude, nearestUnused.longitude), speed)
        if (nearbyPokestops.size > 0) {
            println("Found nearby pokestop")
            val closest = nearbyPokestops.first()
            api.setLocation(lat.get(), lng.get(), 0.0)
            val result = api.map.searchFort(closest)
            if (result.result == FortSearchResponseOuterClass.FortSearchResponse.Result.SUCCESS) {
                usedPokestops.put(closest.id, result.cooldownCompleteTimestampMs)
            } else if (result.result == FortSearchResponseOuterClass.FortSearchResponse.Result.IN_COOLDOWN_PERIOD) {
                usedPokestops.put(closest.id, System.currentTimeMillis() + 5 * 60 * 1000)
            }
            if (result.result == FortSearchResponseOuterClass.FortSearchResponse.Result.SUCCESS) {
                println("Activated portal ${closest.id}")
            } else if (result.result == FortSearchResponseOuterClass.FortSearchResponse.Result.INVENTORY_FULL) {
                println("Activated portal ${closest.id}, but inventory is full")
            } else if (result.result == FortSearchResponseOuterClass.FortSearchResponse.Result.OUT_OF_RANGE) {
                val location = S2LatLng.fromDegrees(closest.latitude, closest.longitude)
                var self = S2LatLng.fromDegrees(lat.get(), lng.get())
                val distance = self.getEarthDistance(location)
                println("Portal out of range; distance: ${distance}")
            } else {
                println("${result.result}")
            }
        }
    }
}
