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
import com.pokegoapi.api.inventory.Item
import com.pokegoapi.api.inventory.ItemBag
import com.pokegoapi.api.map.pokemon.EvolutionResult
import com.pokegoapi.api.player.PlayerProfile
import com.pokegoapi.api.pokemon.Pokemon
import com.pokegoapi.google.common.geometry.S2LatLng
import ink.abb.pogo.scraper.Context
import ink.abb.pogo.scraper.Settings
import ink.abb.pogo.scraper.services.BotService
import ink.abb.pogo.scraper.util.ApiAuthProvider
import ink.abb.pogo.scraper.util.Log
import ink.abb.pogo.scraper.util.credentials.GoogleAutoCredentials
import ink.abb.pogo.scraper.util.data.*
import ink.abb.pogo.scraper.util.pokemon.getStatsFormatted
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
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

        if (!pass.equals(ctx.restApiPassword))
        {
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
        service.getBotContext(name).api.inventories.updateInventories(true)

        val pokemons = mutableListOf<PokemonData>()
        for (pokemon in service.getBotContext(name).api.inventories.pokebank.pokemons) {
            pokemons.add(PokemonData().buildFromPokemon(pokemon))
        }

        return pokemons
    }

    @RequestMapping(value = "/bot/{name}/pokemon/{id}/transfer", method = arrayOf(RequestMethod.POST))
    fun transferPokemon(
            @PathVariable name: String,
            @PathVariable id: Long
    ): String {
        val result: String
        val pokemon: Pokemon? = getPokemonById(service.getBotContext(name), id)

        result = pokemon!!.transferPokemon().toString()

        Log.magenta("REST API: Transferring pokemon ${pokemon.pokemonId.name} with stats (${pokemon.getStatsFormatted()} CP: ${pokemon.cp})")

        // Update GUI
        service.getBotContext(name).server.sendPokebank()

        return result
    }

    @RequestMapping(value = "/bot/{name}/pokemon/{id}/evolve", method = arrayOf(RequestMethod.POST))
    fun evolvePokemon(
            @PathVariable name: String,
            @PathVariable id: Long,
            httpResponse: HttpServletResponse
    ): String {
        val result: String
        val pokemon: Pokemon? = getPokemonById(service.getBotContext(name), id)

        if (pokemon!!.candiesToEvolve > pokemon.candy) {
            httpResponse.status = HttpServletResponse.SC_BAD_REQUEST

            result = "Not enough candies to evolve: ${pokemon.candy}/${pokemon.candiesToEvolve}"
        } else {
            val evolutionResult: EvolutionResult
            val evolved: Pokemon

            evolutionResult = pokemon.evolve()
            evolved = evolutionResult.evolvedPokemon

            Log.magenta("REST API: Evolved pokemon ${pokemon.pokemonId.name} with stats (${pokemon.getStatsFormatted()} CP: ${pokemon.cp})"
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
        val result: String
        val pokemon: Pokemon? = getPokemonById(service.getBotContext(name), id)

        if (pokemon!!.candyCostsForPowerup > pokemon.candy) {
            httpResponse.status = HttpServletResponse.SC_BAD_REQUEST

            result = "Not enough candies to powerup: ${pokemon.candy}/${pokemon.candyCostsForPowerup}"
        } else if (pokemon.stardustCostsForPowerup > service.getBotContext(name).api.playerProfile.currencies.get(PlayerProfile.Currency.STARDUST)!!.toInt()) {
            httpResponse.status = HttpServletResponse.SC_BAD_REQUEST

            result = "Not enough stardust to powerup: ${service.getBotContext(name).api.playerProfile.currencies.get(PlayerProfile.Currency.STARDUST)}/${pokemon.stardustCostsForPowerup}"
        } else {
            Log.magenta("REST API: Powering up pokemon ${pokemon.pokemonId.name} with stats (${pokemon.getStatsFormatted()} CP: ${pokemon.cp})")

            result = pokemon.powerUp().toString()

            Log.magenta("REST API: Pokemon new CP ${pokemon.cp}")
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
        val result: String
        val pokemon: Pokemon? = getPokemonById(service.getBotContext(name), id)

        result = pokemon!!.setFavoritePokemon(!pokemon.isFavorite).toString()
        when (pokemon.isFavorite) {
            true -> Log.magenta("REST API: Pokemon ${pokemon.pokemonId.name} with stats (${pokemon.getStatsFormatted()} CP: ${pokemon.cp}) is favorited")
            false -> Log.magenta("REST API: Pokemon ${pokemon.pokemonId.name} with stats (${pokemon.getStatsFormatted()} CP: ${pokemon.cp}) is now unfavorited")
        }

        // Update GUI
        service.getBotContext(name).server.sendPokebank()

        return result
    }

    @RequestMapping(value = "/bot/{name}/pokemon/{id}/rename", method = arrayOf(RequestMethod.POST))
    fun renamePokemon(
            @PathVariable name: String,
            @PathVariable id: Long,
            @RequestBody newName: String
    ): String {
        val pokemon: Pokemon? = getPokemonById(service.getBotContext(name), id)

        Log.magenta("REST API: Renamed pokemon ${pokemon!!.pokemonId.name} with stats (${pokemon.getStatsFormatted()} CP: ${pokemon.cp}) to $newName")

        return pokemon!!.renamePokemon(newName).toString()
    }

    @RequestMapping("/bot/{name}/items")
    fun listItems(@PathVariable name: String): List<ItemData> {
        service.getBotContext(name).api.inventories.updateInventories(true)

        val items = mutableListOf<ItemData>()
        for (item in service.getBotContext(name).api.inventories.itemBag.items) {
            items.add(ItemData().buildFromItem(item))
        }

        return items
    }

    @RequestMapping(value = "/bot/{name}/item/{id}/drop/{quantity}", method = arrayOf(RequestMethod.DELETE))
    fun dropItem(
            @PathVariable name: String,
            @PathVariable id: Int,
            @PathVariable quantity: Int,
            httpResponse: HttpServletResponse
    ): String {
        val itemBag: ItemBag = service.getBotContext(name).api.inventories.itemBag
        val item: Item? = itemBag.items.find { it.itemId.number == id }

        if (quantity > item!!.count) {
            httpResponse.status = HttpServletResponse.SC_BAD_REQUEST

            return "Not enough items to drop ${item.count}"
        } else {
            Log.magenta("REST API: Dropping ${quantity} ${item.itemId.name}")

            return itemBag.removeItem(item.itemId, quantity).toString()
        }
    }

    @RequestMapping(value = "/bot/{name}/useIncense", method = arrayOf(RequestMethod.POST))
    fun useIncense(
            @PathVariable name: String,
            httpResponse: HttpServletResponse
    ): String {
        val itemBag = service.getBotContext(name).api.inventories.itemBag
        val count = itemBag.items.find { it.itemId == ItemIdOuterClass.ItemId.ITEM_INCENSE_ORDINARY }?.count

        if (count == 0) {
            httpResponse.status = HttpServletResponse.SC_BAD_REQUEST

            return "Not enough incenses"
        } else {
            itemBag.useIncense()

            Log.magenta("REST API: Used incense")

            return "SUCCESS"
        }
    }

    @RequestMapping(value = "/bot/{name}/useLuckyEgg", method = arrayOf(RequestMethod.POST))
    fun useLuckyEgg(
            @PathVariable name: String,
            httpResponse: HttpServletResponse
    ): String {
        val itemBag = service.getBotContext(name).api.inventories.itemBag
        val count = itemBag.items.find { it.itemId == ItemIdOuterClass.ItemId.ITEM_LUCKY_EGG }?.count

        if (count == 0) {
            httpResponse.status = HttpServletResponse.SC_BAD_REQUEST

            return "Not enough lucky eggs"
        } else {
            Log.magenta("REST API: Used lucky egg")

            return itemBag.useLuckyEgg().result.toString()
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
            val entry: PokedexEntryOuterClass.PokedexEntry? = api.inventories.pokedex.getPokedexEntry(PokemonIdOuterClass.PokemonId.forNumber(i))
            entry ?: continue

            pokedex.add(PokedexEntry().buildFromEntry(entry))
        }

        return pokedex
    }

    @RequestMapping(value = "/bot/{name}/eggs", method = arrayOf(RequestMethod.GET))
    fun getEggs(@PathVariable name: String): List<EggData> {
        service.getBotContext(name).api.inventories.updateInventories(true)

        val eggs = mutableListOf<EggData>()
        for (egg in service.getBotContext(name).api.inventories.hatchery.eggs) {
            eggs.add(EggData().buildFromEggPokemon(egg))
        }

        return eggs
    }

    // FIXME! currently, the IDs returned by the API are not unique. It seems that only the last 6 digits change so we remove them
    fun getPokemonById(ctx: Context, id: Long): Pokemon? {
        return ctx.api.inventories.pokebank.pokemons.find {
            (("" + id).substring(0, ("" + id).length - 6)).equals(("" + it.id).substring(0, ("" + it.id).length - 6))
        }
    }
}
