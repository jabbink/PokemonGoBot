/*
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper

import POGOProtos.Inventory.Item.ItemIdOuterClass.ItemId
import com.pokegoapi.api.inventory.Pokeball
import ink.abb.pogo.scraper.util.Log
import java.io.BufferedReader
import java.io.FileOutputStream
import java.io.FileReader
import java.util.*
import com.pokegoapi.google.common.geometry.S2LatLng

class Settings(val properties: Properties) {

    val pokeballItems = mapOf(Pair(ItemId.ITEM_POKE_BALL, Pokeball.POKEBALL),
            Pair(ItemId.ITEM_ULTRA_BALL, Pokeball.ULTRABALL),
            Pair(ItemId.ITEM_GREAT_BALL, Pokeball.GREATBALL),
            Pair(ItemId.ITEM_MASTER_BALL, Pokeball.MASTERBALL))

    val startingLatitude = getPropertyOrDie("Starting Latitude", "latitude", String::toDouble)
    val startingLongitude = getPropertyOrDie("Starting Longitude", "longitude", String::toDouble)

    val startingLocation = S2LatLng.fromDegrees(startingLatitude, startingLongitude)

    val username = properties.getProperty("username")
    val password = if (properties.containsKey("password")) properties.getProperty("password") else String(Base64.getDecoder().decode(properties.getProperty("base64_password", "")))
    val token = { properties.getProperty("token", "") }

    val speed = getPropertyIfSet("Speed", "speed", 2.778, String::toDouble)
    val shouldDropItems = getPropertyIfSet("Item Drop", "drop_items", false, String::toBoolean)

    val uselessItems = if (shouldDropItems) {
        mapOf(
                Pair(ItemId.ITEM_REVIVE, getPropertyIfSet("Max number of items to keep from type ITEM_REVIVE", "item_revive", 20, String::toInt)),
                Pair(ItemId.ITEM_MAX_REVIVE, getPropertyIfSet("Max number of items to keep from type ITEM_MAX_REVIVE", "item_max_revive", 10, String::toInt)),
                Pair(ItemId.ITEM_POTION, getPropertyIfSet("Max number of items to keep from type ITEM_POTION", "item_potion", 0, String::toInt)),
                Pair(ItemId.ITEM_SUPER_POTION, getPropertyIfSet("Max number of items to keep from type ITEM_SUPER_POTION", "item_super_potion", 30, String::toInt)),
                Pair(ItemId.ITEM_HYPER_POTION, getPropertyIfSet("Max number of items to keep from type ITEM_HYPER_POTION", "item_hyper_potion", 50, String::toInt)),
                Pair(ItemId.ITEM_MAX_POTION, getPropertyIfSet("Max number of items to keep from type ITEM_MAX_POTION", "item_max_potion", 50, String::toInt)),
                Pair(ItemId.ITEM_POKE_BALL, getPropertyIfSet("Max number of items to keep from type ITEM_POKE_BALL", "item_poke_ball", 50, String::toInt)),
                Pair(ItemId.ITEM_GREAT_BALL, getPropertyIfSet("Max number of items to keep from type ITEM_GREAT_BALL", "item_great_ball", 50, String::toInt)),
                Pair(ItemId.ITEM_ULTRA_BALL, getPropertyIfSet("Max number of items to keep from type ITEM_ULTRA_BALL", "item_ultra_ball", 50, String::toInt)),
                Pair(ItemId.ITEM_MASTER_BALL, getPropertyIfSet("Max number of items to keep from type ITEM_MASTER_BALL", "item_master_ball", 10, String::toInt)),
                Pair(ItemId.ITEM_RAZZ_BERRY, getPropertyIfSet("Max number of items to keep from type ITEM_RAZZ_BERRY", "item_razz_berry", 50, String::toInt))
        )
    } else {
        mapOf(
                Pair(ItemId.ITEM_REVIVE, 20),
                Pair(ItemId.ITEM_MAX_REVIVE, 10),
                Pair(ItemId.ITEM_POTION, 0),
                Pair(ItemId.ITEM_SUPER_POTION, 30),
                Pair(ItemId.ITEM_HYPER_POTION, 50),
                Pair(ItemId.ITEM_MAX_POTION, 50),
                Pair(ItemId.ITEM_POKE_BALL, 50),
                Pair(ItemId.ITEM_GREAT_BALL, 50),
                Pair(ItemId.ITEM_ULTRA_BALL, 50),
                Pair(ItemId.ITEM_MASTER_BALL, 10),
                Pair(ItemId.ITEM_RAZZ_BERRY, 50)
        )
    }

    val randomNextPokestop = getPropertyIfSet("Number of pokestops to select next", "random_next_pokestop_selection", 5, String::toInt)

    val desiredCatchProbability = getPropertyIfSet("Desired chance to catch a Pokemon with 1 ball", "desired_catch_probability", 0.4, String::toDouble)
    val desiredCatchProbabilityUnwanted = getPropertyIfSet("Desired probability to catch unwanted Pokemon (obligatory_transfer; low IV; low CP)", "desired_catch_probability_unwanted", 0.0, String::toDouble)
  
    val shouldAutoTransfer = getPropertyIfSet("Autotransfer", "autotransfer", false, String::toBoolean)
    val keepPokemonAmount = getPropertyIfSet("minimum keep pokemon amount", "keep_pokemon_amount", 1, String::toInt)
    val shouldDisplayKeepalive = getPropertyIfSet("Display Keepalive Coordinates", "display_keepalive", true, String::toBoolean)

    val shouldDisplayPokestopName = getPropertyIfSet("Display Pokestop Name", "display_pokestop_name", false, String::toBoolean)
    val shouldDisplayPokestopSpinRewards = getPropertyIfSet("Display Pokestop Rewards", "display_pokestop_rewards", true, String::toBoolean)
    val shouldDisplayPokemonCatchRewards = getPropertyIfSet("Display Pokemon Catch Rewards", "display_pokemon_catch_rewards", true, String::toBoolean)

    val shouldLootPokestop = getPropertyIfSet("Loot Pokestops", "loot_pokestop", true, String::toBoolean)
    val shouldCatchPokemons = getPropertyIfSet("Catch Pokemons", "catch_pokemon", true, String::toBoolean)
    val shouldAutoFillIncubators = getPropertyIfSet("Auto Fill Incubators", "auto_fill_incubator", true, String::toBoolean)

    val sortByIV = getPropertyIfSet("Sort by IV first instead of CP", "sort_by_iv", false, String::toBoolean)

    val alwaysCurve = getPropertyIfSet("Always throw curveballs", "always_curve", false, String::toBoolean)

    val neverUseBerries = getPropertyIfSet("Never use berries", "never_use_berries", true, String::toBoolean)

    val allowLeaveStartArea = getPropertyIfSet("Allow leaving the starting area", "allow_leave_start_area", false, String::toBoolean)

    val spawnRadius = getPropertyIfSet("Max distance from starting point the bot should ever go", "spawn_radius", -1, String::toInt)

    val transferCPThreshold = getPropertyIfSet("Minimum CP to keep a pokemon", "transfer_cp_threshold", 400, String::toInt)

    val transferIVThreshold = getPropertyIfSet("Minimum IV percentage to keep a pokemon", "transfer_iv_threshold", 80, String::toInt)

    val ignoredPokemon = if (shouldAutoTransfer) {
        getPropertyIfSet("Never transfer these Pokemon", "ignored_pokemon", "EEVEE,MEWTWO,CHARMANDER", String::toString).split(",")
    } else {
        listOf()
    }
    val obligatoryTransfer = if (shouldAutoTransfer) {
        getPropertyIfSet("list of pokemon you always want to transfer regardless of CP", "obligatory_transfer", "DODUO,RATTATA,CATERPIE,PIDGEY", String::toString).split(",")
    } else {
        listOf()
    }

    private fun <T> getPropertyOrDie(description: String, property: String, conversion: (String) -> T): T {
        val settingString = "$description setting (\"$property\")"

        if (!properties.containsKey(property)) {
            Log.red("$settingString not specified in config.properties!")
            System.exit(1)
        }

        var result: T?
        try {
            result = conversion(properties.getProperty(property))
        } catch (e: Exception) {
            Log.red("Failed to interpret $settingString, got \"${properties.getProperty(property)}\"")
            System.exit(1)
            throw IllegalArgumentException()
        }
        return result
    }

    private fun <T> getPropertyIfSet(description: String, property: String, default: T, conversion: (String) -> T): T {
        val settingString = "$description setting (\"$property\")"
        val defaulting = "defaulting to \"$default\""

        if (!properties.containsKey(property)) {
            Log.yellow("$settingString not specified, $defaulting.")
            return default
        }

        try {
            return conversion(properties.getProperty(property))
        } catch (e: Exception) {
            Log.yellow("$settingString is invalid, defaulting to $default: ${e.message}")
            return default
        }
    }

    fun setToken(value: String) {
        properties.setProperty("token", value)
    }

    fun writeToken(propertyFile: String) {
        val file = BufferedReader(FileReader(propertyFile))
        var propertiesText = String()
        var foundToken = false

        file.lines().forEach {
            if (it != null && it.startsWith("token")) {
                propertiesText += "token=${this.properties.getProperty("token")}\n"
                foundToken = true
            } else if (it != null) {
                propertiesText += "$it\n"
            }
        }

        if (!foundToken) {
            propertiesText += "token=${this.properties.getProperty("token")}\n"
        }
        file.close()

        val out = FileOutputStream(propertyFile)

        out.write(propertiesText.toByteArray())
        out.close()
    }
}
