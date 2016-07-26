package ink.abb.pogo.scraper.gui

import POGOProtos.Data.PokemonDataOuterClass
import com.corundumstudio.socketio.Configuration
import com.corundumstudio.socketio.SocketIOServer
import com.pokegoapi.api.map.fort.Pokestop
import com.pokegoapi.api.player.PlayerProfile
import com.pokegoapi.api.pokemon.Pokemon
import ink.abb.pogo.scraper.Context
import ink.abb.pogo.scraper.util.pokemon.getIvPercentage

class SocketServer {
    val requiredXp = arrayOf(0, 1000, 3000, 6000, 10000, 15000, 21000, 28000, 36000, 45000, 55000, 65000, 75000,
            85000, 100000, 120000, 140000, 160000, 185000, 210000, 260000, 335000, 435000, 560000, 710000, 900000, 1100000,
            1350000, 1650000, 2000000, 2500000, 3000000, 3750000, 4750000, 6000000, 7500000, 9500000, 12000000, 15000000, 20000000)

    private var ctx: Context? = null
    private var server: SocketIOServer? = null

    fun start(ctx: Context, port: Int) {
        val config = Configuration()
        config.hostname = "localhost"
        config.port = port

        this.ctx = ctx

        server = SocketIOServer(config)
        server?.addEventListener("init", EventInit::class.java) { client, data, ackRequest ->
            run {
                sendProfile()
                sendPokebank()
                setLocation(ctx.api.latitude, ctx.api.longitude)
            }
        }

        server?.start()
    }

    fun sendProfile(){
        if(ctx != null){
            val profile = EventProfile()
            profile.username = ctx!!.api.playerProfile.username
            profile.team = ctx!!.api.playerProfile.team.name
            profile.stardust = ctx!!.api.playerProfile.currencies[PlayerProfile.Currency.STARDUST]
            profile.level = ctx!!.api.playerProfile.stats.level
            val curLevelXP = ctx!!.api.playerProfile.stats.experience - requiredXp[ctx!!.api.playerProfile.stats.level - 1]
            profile.levelXp = curLevelXP
            val nextXP = requiredXp[ctx!!.api.playerProfile.stats.level] - requiredXp[ctx!!.api.playerProfile.stats.level - 1]
            val ratio = ((curLevelXP.toDouble() / nextXP.toDouble()) * 100).toInt()
            profile.levelRatio = ratio
            profile.pokebank = ctx!!.api.inventories.pokebank.pokemons.size
            profile.pokebankMax = ctx!!.api.playerProfile.pokemonStorage
            profile.items = ctx!!.api.inventories.itemBag.items.size
            profile.itemsMax = ctx!!.api.playerProfile.itemStorage
            server?.broadcastOperations?.sendEvent("profile", profile)
        }
    }

    fun sendPokebank() {
        if(ctx != null){
            val pokebank = EventPokebank()
            for(pokemon in ctx!!.api.inventories.pokebank.pokemons){
                val pokemonObj = EventPokebank.Pokemon()
                pokemonObj.id = pokemon.id
                pokemonObj.pokemonId = pokemon.pokemonId.number
                pokemonObj.name = pokemon.pokemonId.name
                pokemonObj.cp = pokemon.cp
                pokemonObj.iv = pokemon.getIvPercentage()
                pokebank.pokemon.add(pokemonObj)
            }
            server?.broadcastOperations?.sendEvent("pokebank", pokebank)
        }
    }

    fun sendPokestop(pokestop: Pokestop) {
        val pokestopObj = EventPokestop()
        pokestopObj.id = pokestop.id
        pokestopObj.name = pokestop.details.name
        pokestopObj.lat = pokestop.latitude
        pokestopObj.lng = pokestop.longitude
        server?.broadcastOperations?.sendEvent("pokestop", pokestopObj)
    }

    fun setLocation(lat: Double, lng: Double){
        val newLocation = EventNewLocation()
        newLocation.lat = lat
        newLocation.lng = lng
        server?.broadcastOperations?.sendEvent("newLocation", newLocation)
    }

    fun newPokemon(lat: Double, lng: Double, pokemon: PokemonDataOuterClass.PokemonData){
        val newPokemon = EventNewPokemon()
        newPokemon.lat = lat
        newPokemon.lng = lng
        newPokemon.id = pokemon.id
        newPokemon.pokemonId = pokemon.pokemonId.number
        newPokemon.name = pokemon.pokemonId.name
        newPokemon.cp = pokemon.cp
        newPokemon.iv = pokemon.getIvPercentage()
        server?.broadcastOperations?.sendEvent("newPokemon", newPokemon)
    }

    fun releasePokemon(id: Long){
        val release = EventReleasePokemon()
        release.id = id
        server?.broadcastOperations?.sendEvent("releasePokemon", release)
    }

    fun sendLog(type: String, text: String){
        val log = EventLog()
        log.type = type
        log.text = text
        server?.broadcastOperations?.sendEvent("log", log)
    }

    class EventInit {

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
        var pokemon = mutableListOf<Pokemon>()

        class Pokemon {
            var id: Long? = null
            var pokemonId: Int? = null
            var name: String? = null
            var cp: Int? = null
            var iv: Int? = null
        }
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
    }

    class EventReleasePokemon {
        var id: Long? = null
    }

    class EventLog {
        var type: String? = null
        var text: String? = null
    }
}