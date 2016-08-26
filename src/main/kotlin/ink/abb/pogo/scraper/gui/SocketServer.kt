/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper.gui

import POGOProtos.Data.PokemonDataOuterClass
import com.corundumstudio.socketio.Configuration
import com.corundumstudio.socketio.SocketConfig
import com.corundumstudio.socketio.SocketIOServer
import com.google.common.geometry.S2LatLng
import ink.abb.pogo.api.cache.Pokestop
import ink.abb.pogo.scraper.Context
import ink.abb.pogo.scraper.requiredXp
import ink.abb.pogo.scraper.util.Log
import ink.abb.pogo.scraper.util.data.PokemonData
import ink.abb.pogo.scraper.util.pokemon.eggKmWalked
import ink.abb.pogo.scraper.util.pokemon.getIvPercentage
import ink.abb.pogo.scraper.util.pokemon.getStatsFormatted
import io.netty.util.concurrent.Future
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger

class SocketServer {
    private var ctx: Context? = null
    private var server: SocketIOServer? = null

    val coordinatesToGoTo = mutableListOf<S2LatLng>()

    fun start(ctx: Context, port: Int) {
        val config = Configuration()
        config.port = port
        config.socketConfig = SocketConfig().apply {
            isReuseAddress = true
        }

        this.ctx = ctx

        server = SocketIOServer(config)
        server?.addEventListener("init", EventInit::class.java) { client, data, ackRequest ->
            run {
                sendProfile()
                sendPokebank()
                sendEggs()
                setLocation(ctx.api.latitude, ctx.api.longitude)
            }
        }
        server?.addEventListener("goto", EventGoto::class.java) { client, data, ackRequest ->
            run {
                if (data.lat != null && data.lng != null) {
                    coordinatesToGoTo.add(S2LatLng.fromDegrees(data.lat!!, data.lng!!))
                }
            }
        }

        var startAttempt: Future<Void>? = null
        do {
            Log.normal("Attempting to bind Socket Server to port $port")
            try {
                startAttempt = server?.startAsync()?.syncUninterruptibly()
            } catch (e: Exception) {
                Log.red("Failed to bind Socket Server to port $port; retrying in 5 seconds")
                Thread.sleep(5000)
            }
        } while (startAttempt == null)
        Log.green("Bound Socket Server to port $port")
    }

    fun stop() {
        server?.stop()
    }

    fun sendGotoDone() {
        server?.broadcastOperations?.sendEvent("gotoDone")
    }

    fun sendProfile() {
        if (ctx != null) {
            val profile = EventProfile()
            profile.username = ctx!!.api.playerData.username
            profile.team = ctx!!.api.playerData.team.name
            profile.stardust = ctx!!.api.inventory.currencies.getOrPut("STARDUST", { AtomicInteger(0) }).get()
            profile.level = ctx!!.api.inventory.playerStats.level
            val curLevelXP = ctx!!.api.inventory.playerStats.experience - requiredXp[ctx!!.api.inventory.playerStats.level - 1]
            profile.levelXp = curLevelXP
            val nextXP = if (ctx!!.api.inventory.playerStats.level == requiredXp.size) {
                curLevelXP
            } else {
                (requiredXp[ctx!!.api.inventory.playerStats.level] - requiredXp[ctx!!.api.inventory.playerStats.level - 1]).toLong()
            }
            val ratio = ((curLevelXP.toDouble() / nextXP.toDouble()) * 100).toInt()
            profile.levelRatio = ratio
            profile.pokebank = ctx!!.api.inventory.pokemon.size
            profile.pokebankMax = ctx!!.api.playerData.maxPokemonStorage
            profile.items = ctx!!.api.inventory.size
            profile.itemsMax = ctx!!.api.playerData.maxItemStorage
            server?.broadcastOperations?.sendEvent("profile", profile)
        }
    }

    fun sendPokebank() {
        if (ctx != null) {
            val pokebank = EventPokebank()

            for (pokemon in ctx!!.api.inventory.pokemon) {
                pokebank.pokemon.add(PokemonData().buildFromPokemon(pokemon.value))
            }
            server?.broadcastOperations?.sendEvent("pokebank", pokebank)
        }
    }

    fun sendPokestop(pokestop: Pokestop) {
        val pokestopObj = EventPokestop()
        pokestopObj.id = pokestop.id
        pokestopObj.name = pokestop.name
        pokestopObj.lat = pokestop.fortData.latitude
        pokestopObj.lng = pokestop.fortData.longitude
        server?.broadcastOperations?.sendEvent("pokestop", pokestopObj)
    }

    fun setLocation(lat: Double, lng: Double) {
        val newLocation = EventNewLocation()
        newLocation.lat = lat
        newLocation.lng = lng
        server?.broadcastOperations?.sendEvent("newLocation", newLocation)
    }

    fun newPokemon(lat: Double, lng: Double, pokemon: PokemonDataOuterClass.PokemonData) {
        val newPokemon = EventNewPokemon()
        newPokemon.lat = lat
        newPokemon.lng = lng
        newPokemon.id = pokemon.id
        newPokemon.pokemonId = pokemon.pokemonId.number
        newPokemon.name = pokemon.pokemonId.name
        newPokemon.cp = pokemon.cp
        newPokemon.iv = pokemon.getIvPercentage()
        newPokemon.stats = pokemon.getStatsFormatted()
        newPokemon.individualStamina = pokemon.individualStamina
        newPokemon.individualAttack = pokemon.individualAttack
        newPokemon.individualDefense = pokemon.individualDefense
        newPokemon.creationTimeMs = pokemon.creationTimeMs
        newPokemon.move1 = pokemon.move1.name
        newPokemon.move2 = pokemon.move2.name
        newPokemon.deployedFortId = pokemon.deployedFortId
        newPokemon.stamina = pokemon.stamina
        newPokemon.maxStamina = pokemon.stamina
        server?.broadcastOperations?.sendEvent("newPokemon", newPokemon)
    }

    fun releasePokemon(id: Long) {
        val release = EventReleasePokemon()
        release.id = id
        server?.broadcastOperations?.sendEvent("releasePokemon", release)
    }

    fun sendLog(type: String, text: String) {
        val log = EventLog()
        log.type = type
        log.text = text
        server?.broadcastOperations?.sendEvent("log", log)
    }

    fun sendEggs() {
        if (ctx != null) {
            val eggs = EventEggs()
            for (egg in ctx!!.api.inventory.eggs) {
                val eggObj = EventEggs.Egg()
                eggObj.distanceWalked = egg.value.pokemonData.eggKmWalked(ctx!!.api)
                eggObj.distanceTarget = egg.value.pokemonData.eggKmWalkedTarget
                eggs.eggs.add(eggObj)
            }
            server?.broadcastOperations?.sendEvent("eggs", eggs)
        }
    }

    class EventInit {

    }

    class EventGoto {
        var lat: Double? = null
        var lng: Double? = null
    }

    class EventProfile {
        var username: String? = null
        var team: String? = null
        var stardust: Int? = null
        var level: Int? = null
        var levelXp: Long? = null
        var levelRatio: Int? = null
        var pokebank: Int? = null
        var pokebankMax: Int? = null
        var items: Int? = null
        var itemsMax: Int? = null
    }

    class EventPokebank {
        var pokemon = mutableListOf<PokemonData>()
    }

    class EventPokestop {
        var id: String? = null
        var name: String? = null
        var lat: Double? = null
        var lng: Double? = null
    }

    class EventNewLocation {
        var lat: Double? = null
        var lng: Double? = null
    }

    class EventNewPokemon {
        var lat: Double? = null
        var lng: Double? = null
        var id: Long? = null
        var pokemonId: Int? = null
        var name: String? = null
        var cp: Int? = null
        var iv: Int? = null
        var stats: String? = null
        var individualStamina: Int? = null
        var individualAttack: Int? = null
        var individualDefense: Int? = null
        var creationTimeMs: Long? = null
        var move1: String? = null
        var move2: String? = null
        var deployedFortId: String? = null
        var stamina: Int? = null
        var maxStamina: Int? = null
    }

    class EventReleasePokemon {
        var id: Long? = null
    }

    class EventLog {
        var type: String? = null
        var text: String? = null
    }

    class EventEggs {
        var eggs = mutableListOf<Egg>()

        class Egg {
            var distanceWalked: Double? = null
            var distanceTarget: Double? = null
        }
    }
}
