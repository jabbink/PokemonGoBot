/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper.controllers


import POGOProtos.Data.PokedexEntryOuterClass
import POGOProtos.Enums.PokemonIdOuterClass
import POGOProtos.Inventory.Item.ItemIdOuterClass
import POGOProtos.Networking.Responses.SetFavoritePokemonResponseOuterClass
import POGOProtos.Networking.Responses.UpgradePokemonResponseOuterClass
import com.google.common.geometry.S2LatLng
import ink.abb.pogo.api.cache.BagPokemon
import ink.abb.pogo.api.request.*
import ink.abb.pogo.scraper.Context
import ink.abb.pogo.scraper.Settings
import ink.abb.pogo.scraper.services.BotService
import ink.abb.pogo.scraper.util.ApiAuthProvider
import ink.abb.pogo.scraper.util.Log
import ink.abb.pogo.scraper.util.credentials.GoogleAutoCredentials
import ink.abb.pogo.scraper.util.data.*
import ink.abb.pogo.scraper.util.pokemon.candyCostsForPowerup
import ink.abb.pogo.scraper.util.pokemon.getStatsFormatted
import ink.abb.pogo.scraper.util.pokemon.meta
import ink.abb.pogo.scraper.util.pokemon.stardustCostsForPowerup
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import java.util.concurrent.atomic.AtomicInteger
import javax.servlet.http.HttpServletResponse

@RestController
@CrossOrigin
@RequestMapping("/api")
class BotController {

    @Autowired
    lateinit var service: BotService

    @Autowired
    lateinit var authProvider: ApiAuthProvider

    @RequestMapping("/bots")
    fun bots(): List<Settings> {
        return service.getAllBotSettings()
    }

    @RequestMapping(value = "/bot/{name}/auth", method = arrayOf(RequestMethod.POST))
    fun auth(
            @PathVariable name: String,
            @RequestBody pass: String,
            httpResponse: HttpServletResponse
    ): String {

        val ctx = service.getBotContext(name)

        if (ctx.restApiPassword.equals("")) {
            Log.red("REST API: There is no REST API password set in the configuration for bot $name, generating one now...")
            authProvider.generateRestPassword(name)
            return "REST API password generated for bot $name, check your console output!"
        }

        authProvider.generateAuthToken(name)

        if (!pass.equals(ctx.restApiPassword)) {
            httpResponse.status = HttpServletResponse.SC_UNAUTHORIZED
            return "Your authentication request ($pass) does not match the REST API password from the bot $name configuration!"
        }

        return ctx.restApiToken
    }

    @RequestMapping(value = "/bot/{name}/load", method = arrayOf(RequestMethod.POST))
    fun loadBot(@PathVariable name: String): Settings {
        Log.magenta("REST API: Load bot $name")

        return service.submitBot(name)
    }

    @RequestMapping(value = "/bot/{name}/unload", method = arrayOf(RequestMethod.POST))
    fun unloadBot(@PathVariable name: String): String {
        Log.magenta("REST API: Unload bot $name")

        return service.doWithBot(name) {
            it.stop()
            service.removeBot(it)
        }.toString()
    }

    @RequestMapping(value = "/bot/{name}/reload", method = arrayOf(RequestMethod.POST))
    fun reloadBot(@PathVariable name: String): Settings {
        Log.magenta("REST API: Reload bot $name")

        if (unloadBot(name).equals("false")) // return default settings
            return Settings(
                    credentials = GoogleAutoCredentials(),
                    latitude = 0.0,
                    longitude = 0.0
            )

        return loadBot(name)
    }

    @RequestMapping(value = "/bot/{name}/start", method = arrayOf(RequestMethod.POST))
    fun startBot(@PathVariable name: String): String {
        Log.magenta("REST API: Starting bot $name")

        return service.doWithBot(name) { it.start() }.toString()
    }

    @RequestMapping(value = "/bot/{name}/stop", method = arrayOf(RequestMethod.POST))
    fun stopBot(@PathVariable name: String): String {
        Log.magenta("REST API: Stopping bot $name")

        return service.doWithBot(name) { it.stop() }.toString()
    }

    @RequestMapping(value = "/bot/{name}/pokemons", method = arrayOf(RequestMethod.GET))
    fun listPokemons(@PathVariable name: String): List<PokemonData> {
        return service.getBotContext(name).api.inventory.pokemon.map { PokemonData().buildFromPokemon(it.value) }
    }

    @RequestMapping(value = "/bot/{name}/pokemon/{id}/transfer", method = arrayOf(RequestMethod.POST))
    fun transferPokemon(
            @PathVariable name: String,
            @PathVariable id: Long
    ): String {
        val pokemon: BagPokemon? = getPokemonById(service.getBotContext(name), id)

        val release = ReleasePokemon().withPokemonId(pokemon!!.pokemonData.id)
        Log.magenta("REST API: Transferring pokemon ${pokemon.pokemonData.pokemonId.name} with stats (${pokemon.pokemonData.getStatsFormatted()} CP: ${pokemon.pokemonData.cp})")

        val result = service.getBotContext(name).api.queueRequest(release).toBlocking().first().response

        // Update GUI
        service.getBotContext(name).server.sendPokebank()

        return result.result.toString()
    }

    @RequestMapping(value = "/bot/{name}/pokemon/{id}/evolve", method = arrayOf(RequestMethod.POST))
    fun evolvePokemon(
            @PathVariable name: String,
            @PathVariable id: Long,
            httpResponse: HttpServletResponse
    ): String {
        val result: String
        val pokemon: BagPokemon? = getPokemonById(service.getBotContext(name), id)

        val requiredCandy = pokemon!!.pokemonData.meta.candyToEvolve
        val candy = service.getBotContext(name).api.inventory.candies.getOrPut(pokemon.pokemonData.meta.family, { AtomicInteger(0) }).get()

        if (requiredCandy > candy) {
            httpResponse.status = HttpServletResponse.SC_BAD_REQUEST

            result = "Not enough candies to evolve: ${candy}/${requiredCandy}"
        } else {
            val evolve = EvolvePokemon().withPokemonId(pokemon.pokemonData.id)
            val evolutionResult = service.getBotContext(name).api.queueRequest(evolve).toBlocking().first().response
            val evolved = evolutionResult.evolvedPokemonData

            Log.magenta("REST API: Evolved pokemon ${pokemon.pokemonData.pokemonId.name} with stats (${pokemon.pokemonData.getStatsFormatted()} CP: ${pokemon.pokemonData.cp})"
                    + " to pokemon ${evolved.pokemonId.name} with stats (${evolved.getStatsFormatted()} CP: ${evolved.cp})")

            result = evolutionResult.result.toString()
        }

        // Update GUI
        service.getBotContext(name).server.sendPokebank()

        return result
    }

    @RequestMapping(value = "/bot/{name}/pokemon/{id}/powerup", method = arrayOf(RequestMethod.POST))
    fun powerUpPokemon(
            @PathVariable name: String,
            @PathVariable id: Long,
            httpResponse: HttpServletResponse
    ): String {

        val pokemon = getPokemonById(service.getBotContext(name), id)

        val candy = service.getBotContext(name).api.inventory.candies.getOrPut(pokemon!!.pokemonData.meta.family, { AtomicInteger(0) }).get()
        val stardust = service.getBotContext(name).api.inventory.currencies.getOrPut("STARDUST", { AtomicInteger(0) }).get()
        val result = if (pokemon.pokemonData.candyCostsForPowerup > candy) {
            httpResponse.status = HttpServletResponse.SC_BAD_REQUEST

            "Not enough candies to powerup: ${candy}/${pokemon.pokemonData.candyCostsForPowerup}"
        } else if (pokemon.pokemonData.stardustCostsForPowerup > stardust) {
            httpResponse.status = HttpServletResponse.SC_BAD_REQUEST

            "Not enough stardust to powerup: ${stardust}/${pokemon.pokemonData.stardustCostsForPowerup}"
        } else {
            Log.magenta("REST API: Powering up pokemon ${pokemon.pokemonData.pokemonId.name} with stats (${pokemon.pokemonData.getStatsFormatted()} CP: ${pokemon.pokemonData.cp})")
            val upgrade = UpgradePokemon().withPokemonId(pokemon.pokemonData.id)

            Log.magenta("REST API : pokemon new CP " + pokemon.pokemonData.cp)
            val response = service.getBotContext(name).api.queueRequest(upgrade).toBlocking().first()
            if (response.response.result == UpgradePokemonResponseOuterClass.UpgradePokemonResponse.Result.SUCCESS) {
                Log.magenta("REST API: Pokemon new CP ${response.response.upgradedPokemon.cp}")
            }

            response!!.response.result.toString()
        }

        // Update GUI
        service.getBotContext(name).server.sendPokebank()

        return result
    }

    @RequestMapping(value = "/bot/{name}/pokemon/{id}/favorite", method = arrayOf(RequestMethod.POST))
    fun togglePokemonFavorite(
            @PathVariable name: String,
            @PathVariable id: Long
    ): String {
        val pokemon = getPokemonById(service.getBotContext(name), id)

        val setFav = SetFavoritePokemon().withIsFavorite(pokemon!!.pokemonData.favorite == 0).withPokemonId(pokemon.pokemonData.id)
        val result = service.getBotContext(name).api.queueRequest(setFav).toBlocking().first().response.result
        if (result == SetFavoritePokemonResponseOuterClass.SetFavoritePokemonResponse.Result.SUCCESS) {
            when (pokemon.pokemonData.favorite > 0) {
                false -> Log.magenta("REST API: Pokemon ${pokemon.pokemonData.pokemonId.name} with stats (${pokemon.pokemonData.getStatsFormatted()} CP: ${pokemon.pokemonData.cp}) is favorited")
                true -> Log.magenta("REST API: Pokemon ${pokemon.pokemonData.pokemonId.name} with stats (${pokemon.pokemonData.getStatsFormatted()} CP: ${pokemon.pokemonData.cp}) is now unfavorited")
            }
        }

        // Update GUI
        service.getBotContext(name).server.sendPokebank()

        return result.toString()
    }

    @RequestMapping(value = "/bot/{name}/pokemon/{id}/rename", method = arrayOf(RequestMethod.POST))
    fun renamePokemon(
            @PathVariable name: String,
            @PathVariable id: Long,
            @RequestBody newName: String
    ): String {
        val pokemon = getPokemonById(service.getBotContext(name), id)
        val rename = NicknamePokemon().withNickname(newName).withPokemonId(pokemon!!.pokemonData.id)
        val result = service.getBotContext(name).api.queueRequest(rename).toBlocking().first().response.result.toString()
        Log.magenta("REST API: Renamed pokemon ${pokemon.pokemonData.pokemonId.name} with stats (${pokemon.pokemonData.getStatsFormatted()} CP: ${pokemon.pokemonData.cp}) to $newName")
        return result
    }

    @RequestMapping("/bot/{name}/items")
    fun listItems(@PathVariable name: String): List<ItemData> {
        return service.getBotContext(name).api.inventory.items.map { ItemData().buildFromItem(it.key, it.value.get()) }
    }

    @RequestMapping(value = "/bot/{name}/item/{id}/drop/{quantity}", method = arrayOf(RequestMethod.DELETE))
    fun dropItem(
            @PathVariable name: String,
            @PathVariable id: Int,
            @PathVariable quantity: Int,
            httpResponse: HttpServletResponse
    ): String {
        val itemBag = service.getBotContext(name).api.inventory.items
        val itemId = ItemIdOuterClass.ItemId.forNumber(id)
        val item = itemBag.getOrPut(itemId, { AtomicInteger(0) })
        if (quantity > item.get()) {
            httpResponse.status = HttpServletResponse.SC_BAD_REQUEST

            return "Not enough items to drop ${item.get()}"
        } else {
            Log.magenta("REST API: Dropping ${quantity} ${itemId.name}")

            val recycle = RecycleInventoryItem().withCount(quantity).withItemId(itemId)
            val result = service.getBotContext(name).api.queueRequest(recycle).toBlocking().first().response.result.toString()

            return result
        }
    }

    @RequestMapping(value = "/bot/{name}/useIncense", method = arrayOf(RequestMethod.POST))
    fun useIncense(
            @PathVariable name: String,
            httpResponse: HttpServletResponse
    ): String {
        val itemBag = service.getBotContext(name).api.inventory.items
        val count = itemBag.getOrPut(ItemIdOuterClass.ItemId.ITEM_INCENSE_ORDINARY, { AtomicInteger(0) }).get()

        if (count == 0) {
            httpResponse.status = HttpServletResponse.SC_BAD_REQUEST

            return "Not enough incenses"
        } else {
            val useIncense = UseIncense().withIncenseType(ItemIdOuterClass.ItemId.ITEM_INCENSE_ORDINARY)
            val result = service.getBotContext(name).api.queueRequest(useIncense).toBlocking().first().response.result.toString()

            Log.magenta("REST API: Used incense")

            return result
        }
    }

    @RequestMapping(value = "/bot/{name}/useLuckyEgg", method = arrayOf(RequestMethod.POST))
    fun useLuckyEgg(
            @PathVariable name: String,
            httpResponse: HttpServletResponse
    ): String {
        val itemBag = service.getBotContext(name).api.inventory.items
        val count = itemBag.getOrPut(ItemIdOuterClass.ItemId.ITEM_LUCKY_EGG, { AtomicInteger(0) }).get()

        if (count == 0) {
            httpResponse.status = HttpServletResponse.SC_BAD_REQUEST

            return "Not enough lucky eggs"
        } else {
            val useEgg = UseItemXpBoost().withItemId(ItemIdOuterClass.ItemId.ITEM_LUCKY_EGG)
            val result = service.getBotContext(name).api.queueRequest(useEgg).toBlocking().first().response.result.toString()

            Log.magenta("REST API: Used lucky egg")

            return result
        }
    }

    @RequestMapping(value = "/bot/{name}/location", method = arrayOf(RequestMethod.GET))
    fun getLocation(@PathVariable name: String): LocationData {
        return LocationData(
                service.getBotContext(name).api.latitude,
                service.getBotContext(name).api.longitude
        )
    }

    @RequestMapping(value = "/bot/{name}/location/{latitude}/{longitude}", method = arrayOf(RequestMethod.POST))
    fun changeLocation(
            @PathVariable name: String,
            @PathVariable latitude: Double,
            @PathVariable longitude: Double,
            httpResponse: HttpServletResponse
    ): String {

        val ctx: Context = service.getBotContext(name)

        if (!latitude.isNaN() && !longitude.isNaN()) {
            ctx.server.coordinatesToGoTo.add(S2LatLng.fromDegrees(latitude, longitude))

            Log.magenta("REST API: Added ToGoTo coordinates $latitude $longitude")

            return "SUCCESS"
        } else {
            httpResponse.status = HttpServletResponse.SC_BAD_REQUEST

            return "FAIL"
        }

    }

    @RequestMapping(value = "/bot/{name}/profile", method = arrayOf(RequestMethod.GET))
    fun getProfile(@PathVariable name: String): ProfileData {
        return ProfileData().buildFromApi(service.getBotContext(name).api)
    }

    @RequestMapping(value = "/bot/{name}/pokedex", method = arrayOf(RequestMethod.GET))
    fun getPokedex(@PathVariable name: String): List<PokedexEntry> {

        val pokedex = mutableListOf<PokedexEntry>()
        val api = service.getBotContext(name).api

        for (i in 0..151) {
            val entry: PokedexEntryOuterClass.PokedexEntry? = api.inventory.pokedex.get(PokemonIdOuterClass.PokemonId.forNumber(i))
            entry ?: continue

            pokedex.add(PokedexEntry().buildFromEntry(entry))
        }

        return pokedex
    }

    @RequestMapping(value = "/bot/{name}/eggs", method = arrayOf(RequestMethod.GET))
    fun getEggs(@PathVariable name: String): List<EggData> {

        //service.getBotContext(name).api.inventories.updateInventories(true)
        return service.getBotContext(name).api.inventory.eggs.map { EggData().buildFromEggPokemon(it.value) }
    }

    // FIXME! currently, the IDs returned by the API are not unique. It seems that only the last 6 digits change so we remove them
    fun getPokemonById(ctx: Context, id: Long): BagPokemon? {
        return ctx.api.inventory.pokemon[id]
    }

}
