/*
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper

import POGOProtos.Enums.PokemonIdOuterClass.PokemonId
import POGOProtos.Inventory.Item.ItemIdOuterClass.ItemId
import com.pokegoapi.google.common.geometry.S2LatLng
import ink.abb.pogo.scraper.util.Log
import java.io.BufferedReader
import java.io.FileOutputStream
import java.io.FileReader
import java.util.*


class SettingsParser(val properties: Properties) {
    fun createSettingsFromProperties(): Settings {
        val defaults = Settings(credentials = GoogleCredentials(), startingLatitude = 0.0, startingLongitude = 0.0)
        val shouldDropItems = getPropertyIfSet("Item Drop", "drop_items", defaults.shouldDropItems, String::toBoolean)

        return Settings(
            profileUpdateTimer = getPropertyIfSet("Set Profile Update Timer", "profile_update_timer", defaults.profileUpdateTimer, String::toLong),

            startingLatitude = getPropertyOrDie("Starting Latitude", "latitude", String::toDouble),
            startingLongitude = getPropertyOrDie("Starting Longitude", "longitude", String::toDouble),

            credentials = if (properties.getProperty("username", "").isEmpty()) {
                GoogleCredentials(properties.getProperty("token", ""))
            } else if(properties.getProperty("username", "").contains("@")) {
                GoogleAutoCredentials(properties.getProperty("username"), getPasswordProperty())
            } else {
                PtcCredentials(properties.getProperty("username"), getPasswordProperty())
            },

            speed = getPropertyIfSet("Speed", "speed", defaults.speed, String::toDouble),
            shouldFollowStreets = getPropertyIfSet("Should the bot follow the streets (true) or just go directly to pokestops/waypoints", "follow_streets", defaults.shouldFollowStreets, String::toBoolean),
            shouldDropItems = shouldDropItems,

            uselessItems = if(shouldDropItems) mapOf(
                Pair(ItemId.ITEM_REVIVE, getPropertyIfSet("Max number of items to keep from type ITEM_REVIVE", "item_revive", 20, String::toInt)),
                Pair(ItemId.ITEM_MAX_REVIVE, getPropertyIfSet("Max number of items to keep from type ITEM_MAX_REVIVE", "item_max_revive", 10, String::toInt)),
                Pair(ItemId.ITEM_POTION, getPropertyIfSet("Max number of items to keep from type ITEM_POTION", "item_potion", 0, String::toInt)),
                Pair(ItemId.ITEM_SUPER_POTION, getPropertyIfSet("Max number of items to keep from type ITEM_SUPER_POTION", "item_super_potion", 30, String::toInt)),
                Pair(ItemId.ITEM_HYPER_POTION, getPropertyIfSet("Max number of items to keep from type ITEM_HYPER_POTION", "item_hyper_potion", 50, String::toInt)),
                Pair(ItemId.ITEM_MAX_POTION, getPropertyIfSet("Max number of items to keep from type ITEM_MAX_POTION", "item_max_potion", 50, String::toInt)),
                Pair(ItemId.ITEM_POKE_BALL, getPropertyIfSet("Max number of items to keep from type ITEM_POKE_BALL", "item_poke_ball", 40, String::toInt)),
                Pair(ItemId.ITEM_GREAT_BALL, getPropertyIfSet("Max number of items to keep from type ITEM_GREAT_BALL", "item_great_ball", 45, String::toInt)),
                Pair(ItemId.ITEM_ULTRA_BALL, getPropertyIfSet("Max number of items to keep from type ITEM_ULTRA_BALL", "item_ultra_ball", 50, String::toInt)),
                Pair(ItemId.ITEM_MASTER_BALL, getPropertyIfSet("Max number of items to keep from type ITEM_MASTER_BALL", "item_master_ball", 10, String::toInt)),
                Pair(ItemId.ITEM_RAZZ_BERRY, getPropertyIfSet("Max number of items to keep from type ITEM_RAZZ_BERRY", "item_razz_berry", 30, String::toInt)),
                Pair(ItemId.ITEM_LUCKY_EGG, getPropertyIfSet("Max number of items to keep from type ITEM_LUCKY_EGG", "item_lucky_egg", 5, String::toInt)),
                Pair(ItemId.ITEM_INCENSE_ORDINARY, getPropertyIfSet("Max number of items to keep from type ITEM_INCENSE_ORDINARY", "item_incense", 5, String::toInt)),
                Pair(ItemId.ITEM_TROY_DISK, getPropertyIfSet("Max number of items to keep from type ITEM_TROY_DISK (lure module)", "item_lure_module", 5, String::toInt))
            ) else mapOf(),

            randomNextPokestop = getPropertyIfSet("Number of pokestops to select next", "random_next_pokestop_selection", defaults.randomNextPokestop, String::toInt),

            desiredCatchProbability = getPropertyIfSet("Desired chance to catch a Pokemon with 1 ball", "desired_catch_probability", defaults.desiredCatchProbability, String::toDouble),
            desiredCatchProbabilityUnwanted = getPropertyIfSet("Desired probability to catch unwanted Pokemon (obligatory_transfer; low IV; low CP)", "desired_catch_probability_unwanted", defaults.desiredCatchProbabilityUnwanted, String::toDouble),
            shouldAutoTransfer = getPropertyIfSet("Autotransfer", "autotransfer", defaults.shouldAutoTransfer, String::toBoolean),
            keepPokemonAmount = getPropertyIfSet("minimum keep pokemon amount", "keep_pokemon_amount", defaults.keepPokemonAmount, String::toInt),
            maxPokemonAmount = getPropertyIfSet("maximum keep pokemon amount", "max_pokemon_amount", defaults.maxPokemonAmount, String::toInt),
            shouldDisplayKeepalive = getPropertyIfSet("Display Keepalive Coordinates", "display_keepalive", defaults.shouldDisplayKeepalive, String::toBoolean),

            shouldDisplayPokestopName = getPropertyIfSet("Display Pokestop Name", "display_pokestop_name", defaults.shouldDisplayPokestopName, String::toBoolean),
            shouldDisplayPokestopSpinRewards = getPropertyIfSet("Display Pokestop Rewards", "display_pokestop_rewards", defaults.shouldDisplayPokestopSpinRewards, String::toBoolean),
            shouldDisplayPokemonCatchRewards = getPropertyIfSet("Display Pokemon Catch Rewards", "display_pokemon_catch_rewards", defaults.shouldDisplayPokemonCatchRewards, String::toBoolean),

            shouldLootPokestop = getPropertyIfSet("Loot Pokestops", "loot_pokestop", defaults.shouldLootPokestop, String::toBoolean),
            shouldCatchPokemons = getPropertyIfSet("Catch Pokemons", "catch_pokemon", defaults.shouldCatchPokemons, String::toBoolean),
            shouldAutoFillIncubators = getPropertyIfSet("Auto Fill Incubators", "auto_fill_incubator", defaults.shouldAutoFillIncubators, String::toBoolean),

            sortByIV = getPropertyIfSet("Sort by IV first instead of CP", "sort_by_iv", defaults.sortByIV, String::toBoolean),

            alwaysCurve = getPropertyIfSet("Always throw curveballs", "always_curve", defaults.alwaysCurve, String::toBoolean),

            neverUseBerries = getPropertyIfSet("Never use berries", "never_use_berries", defaults.neverUseBerries, String::toBoolean),

            allowLeaveStartArea = getPropertyIfSet("Allow leaving the starting area", "allow_leave_start_area", defaults.allowLeaveStartArea, String::toBoolean),

            spawnRadius = getPropertyIfSet("Max distance from starting point the bot should ever go", "spawn_radius", defaults.spawnRadius, String::toInt),

            transferCPThreshold = getPropertyIfSet("Minimum CP to keep a pokemon", "transfer_cp_threshold", defaults.transferCPThreshold, String::toInt),

            transferIVThreshold = getPropertyIfSet("Minimum IV percentage to keep a pokemon", "transfer_iv_threshold", defaults.transferIVThreshold, String::toInt),

            ignoredPokemon = getPropertyIfSet("Never transfer these Pokemon", "ignored_pokemon", defaults.ignoredPokemon.map {it.name}.joinToString(","), String::toString).split(",").filter { it.isNotBlank() }.map { PokemonId.valueOf(it) },

            obligatoryTransfer = getPropertyIfSet("list of pokemon you always want to transfer regardless of CP", "obligatory_transfer", defaults.obligatoryTransfer.map {it.name}.joinToString(","), String::toString).split(",").filter { it.isNotBlank() }.map { PokemonId.valueOf(it) },

            export = getPropertyIfSet("Export on Profile Update", "export", defaults.export, String::toString),

            guiPort = getPropertyIfSet("Port where the webserver should listen", "gui_port", defaults.guiPort, String::toInt),
            guiPortSocket = getPropertyIfSet("Port where the socketserver should listen", "gui_port_socket", defaults.guiPortSocket, String::toInt)
        )
    }

    fun getPasswordProperty(): String {
        return if (properties.containsKey("password")) properties.getProperty("password") else String(Base64.getDecoder().decode(properties.getProperty("base64_password", "")))
    }

    fun <T> getPropertyOrDie(description: String, property: String, conversion: (String) -> T): T {
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

    fun <T> getPropertyIfSet(description: String, property: String, default: T, conversion: (String) -> T): T {
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
}

data class Settings(
    val profileUpdateTimer: Long = 60,
    val startingLatitude: Double,
    val startingLongitude: Double,

    val startingLocation: S2LatLng = S2LatLng.fromDegrees(startingLatitude, startingLongitude),
    val credentials: Credentials,
    val speed: Double = 2.778,
    val shouldFollowStreets: Boolean = false,
    val shouldDropItems: Boolean = false,
    val uselessItems: Map<ItemId, Int> = mapOf(
        Pair(ItemId.ITEM_REVIVE, 20),
        Pair(ItemId.ITEM_MAX_REVIVE, 10),
        Pair(ItemId.ITEM_POTION, 0),
        Pair(ItemId.ITEM_SUPER_POTION, 30),
        Pair(ItemId.ITEM_HYPER_POTION, 50),
        Pair(ItemId.ITEM_MAX_POTION, 50),
        Pair(ItemId.ITEM_POKE_BALL, 40),
        Pair(ItemId.ITEM_GREAT_BALL, 45),
        Pair(ItemId.ITEM_ULTRA_BALL, 50),
        Pair(ItemId.ITEM_MASTER_BALL, 10),
        Pair(ItemId.ITEM_RAZZ_BERRY, 30),
        Pair(ItemId.ITEM_LUCKY_EGG, 5),
        Pair(ItemId.ITEM_INCENSE_ORDINARY, 5),
        Pair(ItemId.ITEM_TROY_DISK, 5)

    ),

    val randomNextPokestop: Int = 5,
    val desiredCatchProbability: Double = 0.4,
    val desiredCatchProbabilityUnwanted: Double = 0.0,
    val shouldAutoTransfer: Boolean = false,
    val keepPokemonAmount: Int = 1,
    val maxPokemonAmount: Int = -1,
    val shouldDisplayKeepalive: Boolean = true,

    val shouldDisplayPokestopName: Boolean = false,
    val shouldDisplayPokestopSpinRewards: Boolean = true,
    val shouldDisplayPokemonCatchRewards: Boolean = true,

    val shouldLootPokestop: Boolean = true,
    var shouldCatchPokemons: Boolean = true,
    val shouldAutoFillIncubators: Boolean = true,

    val sortByIV: Boolean = false,
    val alwaysCurve: Boolean = false,
    val neverUseBerries: Boolean = true,
    val allowLeaveStartArea: Boolean = false,
    val spawnRadius: Int = -1,
    val transferCPThreshold: Int = 400,
    val transferIVThreshold: Int = 80,
    val ignoredPokemon: List<PokemonId> = listOf(PokemonId.EEVEE, PokemonId.MEWTWO),

    val obligatoryTransfer: List<PokemonId> = listOf(PokemonId.DODUO, PokemonId.RATTATA, PokemonId.CATERPIE, PokemonId.PIDGEY),

    val export: String = "",

    val guiPort: Int = 8000,
    val guiPortSocket: Int = 8001
) {

    fun writeProperty(propertyFile: String, key: String, value: Any) {
        // TODO: This function does not work with lists, like obligatory_transfer
        val file = BufferedReader(FileReader(propertyFile))
        var propertiesText = String()
        var foundKey = false

        val newKeyValue = "$key=${value.toString()}\r\n"

        file.lines().forEach {
            if (it != null && it.startsWith(key)) {
                propertiesText += newKeyValue
                foundKey = true
            } else if (it != null) {
                propertiesText += "$it\r\n"
            }
        }

        if (!foundKey) {
            propertiesText += newKeyValue
        }
        file.close()

        val out = FileOutputStream(propertyFile)

        out.write(propertiesText.toByteArray())
        out.close()
    }
}

interface Credentials

data class GoogleCredentials(var token: String = "") : Credentials
data class GoogleAutoCredentials(var username: String = "", var password: String = "") : Credentials

data class PtcCredentials(val username: String = "", val password: String = "") : Credentials
