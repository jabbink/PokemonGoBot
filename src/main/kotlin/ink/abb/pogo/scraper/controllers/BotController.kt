/*
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper.controllers

import POGOProtos.Inventory.Item.ItemIdOuterClass
import ink.abb.pogo.scraper.Settings
import ink.abb.pogo.scraper.services.BotService
import ink.abb.pogo.scraper.util.data.PokemonData
import ink.abb.pogo.scraper.util.Log

import org.springframework.beans.factory.annotation.Autowired

import com.pokegoapi.api.inventory.Item
import com.pokegoapi.api.inventory.ItemBag
import com.pokegoapi.api.map.pokemon.EvolutionResult


import com.pokegoapi.api.pokemon.Pokemon;
import ink.abb.pogo.scraper.Context
import ink.abb.pogo.scraper.util.data.ItemData
import ink.abb.pogo.scraper.util.pokemon.getStatsFormatted
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
class BotController {

    @Autowired
    lateinit var service: BotService

    @RequestMapping("/bots")
    fun bots(): List<Settings> {
        return service.getAllBotSettings()
    }

    @RequestMapping("/bot/{name}/load")
    fun loadBot(@PathVariable name: String) {
        service.submitBot(service.load(name))
    }

    @RequestMapping("/bot/{name}/unload")
    fun unloadBot(@PathVariable name: String): Boolean {
        return service.doWithBot(name) {
            it.stop()
            service.removeBot(it)
        }
    }

    @RequestMapping("/bot/{name}/reload")
    fun reloadBot(@PathVariable name: String): Boolean {
        if (!unloadBot(name)) return false
        loadBot(name)
        return true
    }

    @RequestMapping("/bot/{name}/start")
    fun startBot(@PathVariable name: String): Boolean {
        return service.doWithBot(name) { it.start() }
    }

    @RequestMapping("/bot/{name}/stop")
    fun stopBot(@PathVariable name: String): Boolean {
        return service.doWithBot(name) { it.stop() }
    }


    @RequestMapping(value = "/bot/{name}/pokemons", method = arrayOf(RequestMethod.GET))
    fun listPokemons(@PathVariable name: String): List<PokemonData> {

        val data = service.getBotContext(name).api.inventories.pokebank.pokemons
        val returnData = mutableListOf<PokemonData>()
        for (pokemon in data) {
            returnData.add(PokemonData().buildFromPokemon(pokemon))
        }

        return returnData
    }

    @RequestMapping(value = "/bot/{name}/pokemon/{id}/transfer", method = arrayOf(RequestMethod.POST))
    fun transferPokemon(
            @PathVariable name: String,
            @PathVariable id: Long
    ) : String {
        val result: String
        val pokemon: Pokemon? = getPokemonById(service.getBotContext(name), id)

        result = pokemon!!.transferPokemon().toString()
        Log.magenta("REST API :transferring pokemon " + pokemon.pokemonId.name + " with stats (" + pokemon.getStatsFormatted() + " CP : " + pokemon.cp + ")")

        // Update GUI
        service.getBotContext(name).server.sendPokebank()

        return result
    }

    @RequestMapping(value = "/bot/{name}/pokemon/{id}/evolve", method = arrayOf(RequestMethod.POST))
    fun evolvePokemon(
            @PathVariable name: String,
            @PathVariable id: Long
    ) : String {
        val result: String
        val pokemon: Pokemon? = getPokemonById(service.getBotContext(name), id)

        if (pokemon!!.candiesToEvolve > pokemon.candy) {
            result = "Not enough candies" + pokemon.candiesToEvolve + " " + pokemon.candy
        } else {
            val evolutionResult: EvolutionResult
            val evolved: Pokemon
            evolutionResult = pokemon.evolve()
            evolved = evolutionResult.evolvedPokemon

            Log.magenta("REST API : evolved pokemon " + pokemon.pokemonId.name + " with stats (" + pokemon.getStatsFormatted() + " CP : " + pokemon.cp + ")"
                    + "To pokemon " + evolved.pokemonId.name + "with stats (" + evolved.getStatsFormatted() + " CP : " + evolved.cp + ")")

            result = evolutionResult.result.toString()
        }

        // Update GUI
        service.getBotContext(name).server.sendPokebank()

        return result
    }

    @RequestMapping(value = "/bot/{name}/pokemon/{id}/powerup", method = arrayOf(RequestMethod.POST))
    fun powerUpPokemon(
            @PathVariable name: String,
            @PathVariable id: Long
    ) : String {

        val result: String
        val pokemon: Pokemon? = getPokemonById(service.getBotContext(name), id)

        if (pokemon!!.candyCostsForPowerup > pokemon.candy) {
            result = "Not enough candies" + pokemon.candyCostsForPowerup + " " + pokemon.candy
        } else {
            Log.magenta("REST API : powering up pokemon " + pokemon.pokemonId.name + "with stats (" + pokemon.getStatsFormatted() + " CP : " + pokemon.cp + ")")
            result = pokemon.powerUp().toString()
            Log.magenta("REST API : pokemon new CP " + pokemon.cp)
        }

        // Update GUI
        service.getBotContext(name).server.sendPokebank()

        return result
    }

    @RequestMapping(value = "/bot/{name}/pokemon/{id}/favorite", method = arrayOf(RequestMethod.POST))
    fun togglePokemonFavorite(
            @PathVariable name: String,
            @PathVariable id: Long
    ) : String {
        val result: String
        val pokemon: Pokemon? = getPokemonById(service.getBotContext(name), id)

        result = pokemon!!.setFavoritePokemon(!pokemon.isFavorite).toString()
        when (pokemon.isFavorite) {
            true -> Log.magenta("REST API : pokemon " + pokemon.pokemonId.name + "with stats (" + pokemon.getStatsFormatted() + " CP : " + pokemon.cp + ") is favorited")
            false -> Log.magenta("REST API : pokemon " + pokemon.pokemonId.name + "with stats (" + pokemon.getStatsFormatted() + " CP : " + pokemon.cp + ") is now unfavorited")
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
        return pokemon!!.renamePokemon(newName).toString()
    }

    @RequestMapping(value = "/bot/{name}/pokemon/{id}/candies", method = arrayOf(RequestMethod.GET))
    fun getPokemonCandies(
            @PathVariable name: String,
            @PathVariable id: Long
    ): String {
        val pokemon: Pokemon? = getPokemonById(service.getBotContext(name), id)
        return ""+pokemon!!.candy
    }

    @RequestMapping("/bot/{name}/items")
    fun listItems(@PathVariable name: String): List<ItemData> {

        val data = service.getBotContext(name).api.inventories.itemBag.items
        val returnData = mutableListOf<ItemData>()

        for (item in data) {
            returnData.add(ItemData().buildFromItem(item))
        }

        return returnData
    }

    @RequestMapping("/bot/{name}/item/{id}/drop/{quantity}")
    fun dropItem(
            @PathVariable name: String,
            @PathVariable id: Int,
            @PathVariable quantity: Int
    ): String {

        val itemBag: ItemBag = service.getBotContext(name).api.inventories.itemBag
        val item: Item? = itemBag.items.find { it.itemId.number == id }

        if (quantity > item!!.count) {
            return "Not enough items to drop " + item.count
        } else {
            Log.magenta("REST API : dropping " + quantity + " " + item.itemId.name)
            return itemBag.removeItem(item.itemId, quantity).toString()
        }
    }

    @RequestMapping("/bot/{name}/useIncense")
    fun useIncense(@PathVariable name: String): String {
        val itemBag = service.getBotContext(name).api.inventories.itemBag
        val count = itemBag.items.find { it.itemId == ItemIdOuterClass.ItemId.ITEM_INCENSE_ORDINARY }?.count
        if (count == 0) {
            return "Not enough incense"
        } else {
            itemBag.useIncense()
            return "SUCCESS"
        }
    }

    @RequestMapping("/bot/{name}/useLuckyEgg")
    fun useLuckyEgg(@PathVariable name: String): String {
        val itemBag = service.getBotContext(name).api.inventories.itemBag
        val count = itemBag.items.find { it.itemId == ItemIdOuterClass.ItemId.ITEM_LUCKY_EGG }?.count

        if (count == 0) {
            return "Not enough lucky egg"
        } else {
            return itemBag.useLuckyEgg().result.toString()
        }
    }

    @RequestMapping("/bot/{name}/location/{latitude}/{longitude}")
    fun changeLocation(
            @PathVariable name: String,
            @PathVariable latitude: Double,
            @PathVariable longitude: Double
    ): String {
        val ctx: Context = service.getBotContext(name)

        // Stop walking
        ctx.pauseWalking.set(true)

        ctx.lat.set(latitude)
        ctx.lng.set(longitude)

        ctx.pauseWalking.set(false)

        ctx.server.setLocation(latitude, longitude)

        return "SUCCESS"
    }

    // FIXME! currently, the IDs returned by the API are not unique. It seems that only the last 6 digits change so we remove them
    fun getPokemonById(ctx: Context, id: Long): Pokemon? {
        return ctx.api.inventories.pokebank.pokemons.find {
            ((""+id).substring(0, (""+id).length - 6)).equals((""+it.id).substring(0, (""+it.id).length - 6))
        }
    }

}
