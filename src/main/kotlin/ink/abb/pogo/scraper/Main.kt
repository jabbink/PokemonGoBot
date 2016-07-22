/**
 * Pokemon Go Bot  Copyright (C) 2016  Jasper Abbink
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper

import POGOProtos.Inventory.ItemIdOuterClass
import POGOProtos.Inventory.ItemIdOuterClass.ItemId
import POGOProtos.Networking.Envelopes.RequestEnvelopeOuterClass
import POGOProtos.Networking.Responses.CatchPokemonResponseOuterClass
import POGOProtos.Networking.Responses.FortSearchResponseOuterClass.FortSearchResponse.Result
import POGOProtos.Networking.Responses.RecycleInventoryItemResponseOuterClass
import com.google.common.geometry.S2LatLng
import com.google.common.util.concurrent.AtomicDouble
import com.pokegoapi.api.PokemonGo
import com.pokegoapi.api.inventory.Pokeball
import com.pokegoapi.api.map.MapObjects
import com.pokegoapi.api.map.fort.Pokestop
import com.pokegoapi.api.player.PlayerProfile
import com.pokegoapi.api.pokemon.Pokemon
import com.pokegoapi.auth.GoogleLogin
import com.pokegoapi.auth.PtcLogin
import okhttp3.OkHttpClient
import ink.abb.pogo.scraper.Context
import ink.abb.pogo.scraper.tasks.Release
import java.awt.List
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


val properties = Properties()

val pokeballItems = mapOf(Pair(ItemIdOuterClass.ItemId.ITEM_POKE_BALL, Pokeball.POKEBALL),
        Pair(ItemIdOuterClass.ItemId.ITEM_ULTRA_BALL, Pokeball.ULTRABALL),
        Pair(ItemIdOuterClass.ItemId.ITEM_GREAT_BALL, Pokeball.GREATBALL),
        Pair(ItemIdOuterClass.ItemId.ITEM_MASTER_BALL, Pokeball.MASTERBALL))
var context: Context? = null

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

    builder.sslSocketFactory(sslSocketFactory);
    builder.hostnameVerifier { hostname, session -> true }
}

fun main(args: Array<String>) {
    val builder = OkHttpClient.Builder()
    // allowProxy(builder)
    builder.connectTimeout(60, TimeUnit.SECONDS)
    builder.readTimeout(60, TimeUnit.SECONDS)
    builder.writeTimeout(60, TimeUnit.SECONDS)
    val http = builder.build()

    FileInputStream("config.properties").use {
        properties.load(it)
    }

    try {
        lat.set(properties.getProperty("latitude").toDouble())
        lng.set(properties.getProperty("longitude").toDouble())
    } catch (e: Exception) {
        println("Starting location not set in config.properties")
        System.exit(1)
    }

    try {
        speed = properties.getProperty("speed").toDouble()
    } catch (e: Exception) {
        println("No speed specified, defaulting to $speed")
    }

    try {
        autotransfer = properties.getProperty("autotransfer").toBoolean()
    } catch (e: Exception) {
        println("No autotransfer specified, defaulting to $autotransfer")
    }

    var shouldDropItems = false
    try {
        shouldDropItems = properties.getProperty("drop_items").toBoolean()
    } catch (e: Exception) {
        println("No item drop policy specified, defaulting to $shouldDropItems")
    }

    val username = properties.getProperty("username")
    var auth: RequestEnvelopeOuterClass.RequestEnvelope.AuthInfo
    if (username.contains('@')) {
        auth = GoogleLogin(http).login(username, properties.getProperty("password"))
    } else {
        auth = PtcLogin(http).login(username, properties.getProperty("password"))
    }
    println("Logged in as ${properties.getProperty("username")}")

    print("Get default data from pogo server")
    val go = PokemonGo(auth, http)
    while (profile == null) {
        profile = go.playerProfile
        print(".")
        Thread.sleep(1000)
    }
    context = Context(go, lat, lng, profile, speed, walking, auth, http)
    println("Context built!")

    println("Pokecoin: ${profile!!.currencies.get(PlayerProfile.Currency.POKECOIN)}")
    println("Stardust: ${profile!!.currencies.get(PlayerProfile.Currency.STARDUST)}")
    println("Level ${profile!!.stats.level}, Experience ${profile!!.stats.experience}")

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

var speed = 2.778
var autotransfer = false
var walking = false

val lat = AtomicDouble()
val lng = AtomicDouble()
var profile: PlayerProfile? = null

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

    val wild_pokemons = api.map.mapObjects.wildPokemons

    val sorted_wild_pokemons = wild_pokemons.sortedWith(Comparator { a, b ->
        val locationA = S2LatLng.fromDegrees(a.latitude, a.longitude)
        val locationB = S2LatLng.fromDegrees(b.latitude, b.longitude)
        val self = S2LatLng.fromDegrees(lat.get(), lng.get())
        val distanceA = self.getEarthDistance(locationA)
        val distanceB = self.getEarthDistance(locationB)
        distanceA.compareTo(distanceB)
    })

    if (sorted_wild_pokemons.isNotEmpty()) {
        val wild_pokemon = wild_pokemons.first()
        println("found wild pokemon ${wild_pokemon.pokemonData.pokemonId}")
        walk(S2LatLng.fromDegrees(wild_pokemons.first().latitude, wild_pokemons.first().longitude), speed)
    }

    val pokemon = api.map.catchablePokemon
    if (pokemon.isNotEmpty()) {
        val catchablePokemon = pokemon.first()
        var ball: ItemIdOuterClass.ItemId? = null
        try {
            val preferred_ball = ItemIdOuterClass.ItemId.valueOf(properties.getProperty("preferred_ball", "ITEM_POKE_BALL"));
            var item = api.bag.getItem(preferred_ball)

            // if we dont have our prefered pokeball, try fallback to other
            if (item == null || item.count == 0)
                for (other in pokeballItems) {
                    if (preferred_ball == other) continue

                    item = api.bag.getItem(other.key);
                    if (item != null && item.count > 0)
                        ball = other.key
                }
            else
                ball = preferred_ball
        } catch (e: Exception) {
            throw e;
        }

        if (ball != null) {
            val usedPokeball = pokeballItems[ball]
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
            return //always catch more
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

        /*val pokestop = com.pokegoapi.google.common.geometry.S2LatLng.fromDegrees(nearestUnused.latitude, nearestUnused.longitude)
        val player = com.pokegoapi.google.common.geometry.S2LatLng.fromDegrees(api.latitude, api.longitude)
        val distance = pokestop.getEarthDistance(player)
        println("CanLoot: ${nearestUnused.canLoot()}; distance: $distance; timestamp: ${nearestUnused.cooldownCompleteTimestampMs}")*/
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

val uselessItems = mapOf(
        Pair(ItemId.ITEM_REVIVE, 30),
        Pair(ItemId.ITEM_MAX_REVIVE, 8),
        Pair(ItemId.ITEM_POTION, 5),
        Pair(ItemId.ITEM_SUPER_POTION, 50),
        Pair(ItemId.ITEM_HYPER_POTION, 50),
        Pair(ItemId.ITEM_MAX_POTION, 5),
        Pair(ItemId.ITEM_POKE_BALL, 20),
        Pair(ItemId.ITEM_GREAT_BALL, 200),
        Pair(ItemId.ITEM_ULTRA_BALL, 200),
        Pair(ItemId.ITEM_MASTER_BALL, 200)
)

fun dropUselessItems(api: PokemonGo) {
    uselessItems.forEach {
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
