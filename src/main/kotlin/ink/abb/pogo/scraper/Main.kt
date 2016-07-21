/**
 * Pokemon Go Bot  Copyright (C) 2016  Jasper Abbink
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper

import POGOProtos.Inventory.ItemIdOuterClass
import POGOProtos.Networking.Envelopes.RequestEnvelopeOuterClass
import POGOProtos.Networking.Responses.CatchPokemonResponseOuterClass
import POGOProtos.Networking.Responses.FortSearchResponseOuterClass.FortSearchResponse.Result
import POGOProtos.Networking.Responses.RecycleInventoryItemResponseOuterClass
import com.google.common.geometry.S2LatLng
import com.google.common.util.concurrent.AtomicDouble
import com.pokegoapi.api.PokemonGo
import com.pokegoapi.api.map.MapObjects
import com.pokegoapi.api.map.fort.Pokestop
import com.pokegoapi.api.player.PlayerProfile
import com.pokegoapi.auth.GoogleLogin
import com.pokegoapi.auth.PTCLogin
import ink.abb.pogo.scraper.tasks.Release
import okhttp3.OkHttpClient
import java.io.FileInputStream
import java.net.InetSocketAddress
import java.net.Proxy
import java.security.cert.X509Certificate
import java.text.DecimalFormat
import java.util.*
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
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
    val sslSocketFactory = sslContext.socketFactory

    builder.sslSocketFactory(sslSocketFactory)
    builder.hostnameVerifier { hostname, session -> true }
}

fun main(args: Array<String>) {
    val builder = OkHttpClient.Builder()
    // allowProxy(builder)
    builder.connectTimeout(60, TimeUnit.SECONDS)
    builder.readTimeout(60, TimeUnit.SECONDS)
    builder.writeTimeout(60, TimeUnit.SECONDS)
    val http = builder.build()
    val properties = Properties()
    val settings = Settings(properties)

    FileInputStream("config.properties").use {
        properties.load(it)
    }

    try {
        settings.getLatitude()
        settings.getLongitude()
    } catch (e: Exception) {
        println("Starting latitude/longitude location not set in config.properties")
        System.exit(1)
    }

    val username = settings.getUsername()
    val password = settings.getPassword()

    val auth: RequestEnvelopeOuterClass.RequestEnvelope.AuthInfo
    if (username.contains('@')) {
        auth = GoogleLogin(http).login(username, password)
    } else {
        auth = PTCLogin(http).login(username, password)
    }
    println("Logged in as ${username}")

    Bot(auth, http, settings).run()
}

class Bot(val auth: RequestEnvelopeOuterClass.RequestEnvelope.AuthInfo, val http: OkHttpClient, val settings: Settings) {
    val lat = AtomicDouble(settings.getLatitude())
    val lng = AtomicDouble(settings.getLongitude())

    val shouldDropItems = settings.shouldDropItems()
    val speed = settings.getSpeed()
    val autotransfer = settings.shouldAutoTransfer()

    var walking = false
    var profile: PlayerProfile? = null
    var context: Context? = null

    fun run() {
        print("Get default data from pogo server")
        val go = PokemonGo(auth, http)
        while (profile == null) {
            profile = go.playerProfile
            print(".")
            Thread.sleep(1000)
        }
        println(".")
        context = Context(go, lat, lng, profile, speed, walking, auth, http)

        println()
        println("Name: ${profile!!.username}")
        println("Team: ${profile!!.team}")
        println("Pokecoin: ${profile!!.currencies.get(PlayerProfile.Currency.POKECOIN)}")
        println("Pokecoin: ${profile!!.currencies.get(PlayerProfile.Currency.POKECOIN)}")
        println("Stardust: ${profile!!.currencies.get(PlayerProfile.Currency.STARDUST)}")
        println("Level ${profile!!.stats.level}, Experience ${profile!!.stats.experience}")
        println()

        go.setLocation(lat.get() + randomLatLng(), lng.get() + randomLatLng(), 0.0)
        println("Getting map of ${lat.get()} ${lng.get()}")

        go.pokebank.pokemons.map { "Got ${it.pokemonId.name} (${it.nickname}) with ${it.cp} CP" }.forEach { println(it) }

        var reply: MapObjects? = null
        print("Getting initial pokestops")
        while (reply == null) {
            reply = go.map.mapObjects
            print(".")
            Thread.sleep(1000)
        }
        println(".")

        val pokestops = reply.pokestops!!

        processMapObjects(go, pokestops)
        fixedRateTimer("GetMapObjects", false, 5000, 5000, action = {
            thread(block = {
                // query a small area to keep alive
                println("Getting map of ${lat.get()} ${lng.get()}")
                go.setLocation(lat.get() + randomLatLng(), lng.get() + randomLatLng(), 0.0)
                reply = go.map.getMapObjects(0)
                processMapObjects(go, pokestops)
                if (shouldDropItems) {
                    dropUselessItems(go)
                }
            })
        })
    }

    fun randomLatLng(): Double {
        return Math.random() * 0.0001 - 0.00005
    }

    fun walk(end: S2LatLng, speed: Double) {
        if (walking)
            return

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

    fun processMapObjects(api: PokemonGo, pokestops: MutableCollection<Pokestop>) {
        val pokemon = api.map.catchablePokemon
        if (pokemon.isNotEmpty()) {
            val catchablePokemon = pokemon.first()
            var ball: ItemIdOuterClass.ItemId? = null
            try {
                val preferred_ball = settings.getPreferredBall()
                var item = api.bag.getItem(preferred_ball)

                // if we dont have our prefered pokeball, try fallback to other
                if (item == null || item.count == 0)
                    for (other in settings.pokeballItems) {
                        if (preferred_ball == other) continue

                        item = api.bag.getItem(other.key);
                        if (item != null && item.count > 0)
                            ball = other.key
                    }
                else
                    ball = preferred_ball
            } catch (e: Exception) {
                throw e
            }

            if (ball != null) {
                val usedPokeball = settings.pokeballItems[ball]
                println("found pokemon ${catchablePokemon.pokemonId}")
                api.setLocation(lat.get(), lng.get(), 0.0)
                val encounterResult = catchablePokemon.encounterPokemon()
                if (encounterResult.wasSuccessful()) {
                    println("encountered pokemon ${catchablePokemon.pokemonId}")
                    val result = catchablePokemon.catchPokemon(usedPokeball)

                    if (result.status == CatchPokemonResponseOuterClass.CatchPokemonResponse.CatchStatus.CATCH_SUCCESS)
                        println("Caught a ${catchablePokemon.pokemonId} using a ${ball}")
                    else
                        println("Capture of ${catchablePokemon.pokemonId} failed with status : ${result.status}")
                }
            }

        }

        val sortedPokestops = pokestops.sortedWith(Comparator { a, b ->
            val locationA = S2LatLng.fromDegrees(a.latitude, a.longitude)
            val locationB = S2LatLng.fromDegrees(b.latitude, b.longitude)
            val self = S2LatLng.fromDegrees(lat.get(), lng.get())
            val distanceA = self.getEarthDistance(locationA)
            val distanceB = self.getEarthDistance(locationB)
            distanceA.compareTo(distanceB)
        })

        val nearbyPokestops = sortedPokestops.filter {
            it.canLoot()
        }

        val nearestUnused = sortedPokestops.filter {
            it.canLoot(true)
        }

        if (nearestUnused.size > 0) {
            walk(S2LatLng.fromDegrees(nearestUnused.first().latitude, nearestUnused.first().longitude), speed)
        }

        if (nearbyPokestops.size > 0) {
            println("Found nearby pokestop")
            val closest = nearbyPokestops.first()
            api.setLocation(lat.get(), lng.get(), 0.0)
            val result = closest.loot()
            when (result.result) {
                Result.SUCCESS -> println("Activated portal ${closest.id}")
                Result.INVENTORY_FULL -> {
                    println("Activated portal ${closest.id}, but inventory is full")
                }
                Result.OUT_OF_RANGE -> {
                    val location = S2LatLng.fromDegrees(closest.latitude, closest.longitude)
                    val self = S2LatLng.fromDegrees(lat.get(), lng.get())
                    val distance = self.getEarthDistance(location)
                    println("Portal out of range; distance: $distance")
                }
                else -> println(result.result)
            }
            return
        }

        val player = api.getPlayerProfile(true)
        val nextXP = player.stats.nextLevelXp - player.stats.prevLevelXp
        val curLevelXP = player.stats.experience - player.stats.prevLevelXp
        val ratio = DecimalFormat("##.00").format(curLevelXP.toDouble() / nextXP.toDouble() * 100.0)
        println("Profile update : ${player.stats.experience} XP on LVL ${player.stats.level}; $curLevelXP/$nextXP (${ratio}%) to LVL ${player.stats.level + 1}")
        if (player != null && autotransfer) {
            // TODO: The API allows to release pokemon in batches, the app does not
            Release().run(context)
        }
    }

    fun dropUselessItems(api: PokemonGo) {
        settings.uselessItems.forEach {
            val item = api.bag.getItem(it.key)
            val count = item.count - it.value
            if (count > 0) {
                val result = api.bag.removeItem(it.key, count)
                if (result == RecycleInventoryItemResponseOuterClass.RecycleInventoryItemResponse.Result.SUCCESS) {
                    println("Dropped ${count}x ${it.key.name}")
                } else {
                    println("Failed to drop ${count}x ${it.key.name}: ${result}")
                }
            }
        }
    }
}
