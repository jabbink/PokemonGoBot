/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper

import POGOProtos.Enums.PokemonIdOuterClass.PokemonId
import POGOProtos.Inventory.Item.ItemIdOuterClass.ItemId
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.google.common.geometry.S2LatLng
import ink.abb.pogo.scraper.util.Log
import ink.abb.pogo.scraper.util.credentials.Credentials
import ink.abb.pogo.scraper.util.credentials.GoogleAutoCredentials
import ink.abb.pogo.scraper.util.credentials.GoogleCredentials
import ink.abb.pogo.scraper.util.credentials.PtcCredentials
import ink.abb.pogo.scraper.util.directions.RouteProviderEnum
import ink.abb.pogo.scraper.util.directions.isValidRouteProvider
import java.io.BufferedReader
import java.io.FileOutputStream
import java.io.FileReader
import java.util.*


class SettingsParser(val properties: Properties) {
    fun createSettingsFromProperties(): Settings {
        val defaults = Settings(name = "", credentials = GoogleCredentials(), latitude = 0.0, longitude = 0.0)
        val dropItems = getPropertyIfSet("Item Drop", "drop_items", defaults.dropItems, String::toBoolean)


        return Settings(
                name = "default",
                profileUpdateTimer = getPropertyIfSet("Set Profile Update Timer", "profile_update_timer", defaults.profileUpdateTimer, String::toLong),
                timerWalkToStartPokestop = getPropertyIfSet("Set Timer to return the first Pokestop (minutes)", "timer_walk_to_start_pokestop", defaults.timerWalkToStartPokestop, String::toLong),
                latitude = getPropertyOrDie("Starting Latitude", "latitude", String::toDouble),
                longitude = getPropertyOrDie("Starting Longitude", "longitude", String::toDouble),
                saveLocationOnShutdown = getPropertyIfSet("Save last location when the bot stop", "save_location_on_shutdown", defaults.saveLocationOnShutdown, String::toBoolean),
                savedLatitude = getPropertyIfSet("Saved start Latitude", "saved_latitude", defaults.savedLatitude, String::toDouble),
                savedLongitude = getPropertyIfSet("Saved start Longitude", "saved_longitude", defaults.savedLongitude, String::toDouble),

                credentials = if (properties.getProperty("username", "").isEmpty()) {
                    GoogleCredentials(properties.getProperty("token", ""))
                } else if (properties.getProperty("username", "").contains("@")) {
                    GoogleAutoCredentials(properties.getProperty("username"), getPasswordProperty())
                } else {
                    PtcCredentials(properties.getProperty("username"), getPasswordProperty())
                },

                proxyServer = getPropertyIfSet("Proxy server to be used by the bot", "proxy_server", defaults.proxyServer, String::toString),
                proxyPort = getPropertyIfSet("Proxy server port to be used by the bot", "proxy_port", defaults.proxyPort, String::toInt),
                proxyType = getPropertyIfSet("Type of the proxy server (HTTP/SOCKS/DIRECT)", "proxy_type", defaults.proxyType, String::toString),
                proxyUsername = getPropertyIfSet("Username for the proxy server", "proxy_username", defaults.proxyUsername, String::toString),
                proxyPassword = getPropertyIfSet("Password for the proxy server", "proxy_password", defaults.proxyPassword, String::toString),

                speed = getPropertyIfSet("Speed", "speed", defaults.speed, String::toDouble),
                randomSpeedRange = getPropertyIfSet("Define random speed range around the original speed", "random_speed_range", defaults.randomSpeedRange, String::toDouble),
                followStreets = getPropertyIfSet("Should the bot follow the streets", "follow_streets", defaults.followStreets.map { it.name }.joinToString(","), String::toString).split(",").filter { it.isNotBlank() && isValidRouteProvider(it) }.map { RouteProviderEnum.valueOf(it) },
                mapzenApiKey = getPropertyIfSet("If you use MAPZEN as route provider, you can use a Mapzen Turn by Turn API key", "mapzen_api_key", defaults.mapzenApiKey, String::toString),
                googleApiKey = getPropertyIfSet("If you use GOOGLE as route provider, you must use a Google API key", "google_api_key", defaults.googleApiKey, String::toString),
                dropItems = dropItems,
                itemDropDelay = getPropertyIfSet("Delay between each drop of items", "item_drop_delay", defaults.itemDropDelay, String::toLong),
                groupItemsByType = getPropertyIfSet("Should the items that are kept be grouped by type (keep best from same type)", "group_items_by_type", defaults.groupItemsByType, String::toBoolean),

                uselessItems = mapOf(
                        Pair(ItemId.ITEM_REVIVE, getPropertyIfSet("Max number of items to keep from type ITEM_REVIVE", "item_revive", 20, String::toInt)),
                        Pair(ItemId.ITEM_MAX_REVIVE, getPropertyIfSet("Max number of items to keep from type ITEM_MAX_REVIVE", "item_max_revive", 10, String::toInt)),
                        Pair(ItemId.ITEM_POTION, getPropertyIfSet("Max number of items to keep from type ITEM_POTION", "item_potion", 0, String::toInt)),
                        Pair(ItemId.ITEM_SUPER_POTION, getPropertyIfSet("Max number of items to keep from type ITEM_SUPER_POTION", "item_super_potion", 30, String::toInt)),
                        Pair(ItemId.ITEM_HYPER_POTION, getPropertyIfSet("Max number of items to keep from type ITEM_HYPER_POTION", "item_hyper_potion", 50, String::toInt)),
                        Pair(ItemId.ITEM_MAX_POTION, getPropertyIfSet("Max number of items to keep from type ITEM_MAX_POTION", "item_max_potion", 50, String::toInt)),
                        Pair(ItemId.ITEM_POKE_BALL, getPropertyIfSet("Max number of items to keep from type ITEM_POKE_BALL", "item_poke_ball", 40, String::toInt)),
                        Pair(ItemId.ITEM_GREAT_BALL, getPropertyIfSet("Max number of items to keep from type ITEM_GREAT_BALL", "item_great_ball", 50, String::toInt)),
                        Pair(ItemId.ITEM_ULTRA_BALL, getPropertyIfSet("Max number of items to keep from type ITEM_ULTRA_BALL", "item_ultra_ball", 50, String::toInt)),
                        Pair(ItemId.ITEM_MASTER_BALL, getPropertyIfSet("Max number of items to keep from type ITEM_MASTER_BALL", "item_master_ball", 10, String::toInt)),
                        Pair(ItemId.ITEM_RAZZ_BERRY, getPropertyIfSet("Max number of items to keep from type ITEM_RAZZ_BERRY", "item_razz_berry", 40, String::toInt)),
                        Pair(ItemId.ITEM_LUCKY_EGG, getPropertyIfSet("Max number of items to keep from type ITEM_LUCKY_EGG", "item_lucky_egg", -1, String::toInt)),
                        Pair(ItemId.ITEM_INCENSE_ORDINARY, getPropertyIfSet("Max number of items to keep from type ITEM_INCENSE_ORDINARY", "item_incense", -1, String::toInt)),
                        Pair(ItemId.ITEM_TROY_DISK, getPropertyIfSet("Max number of items to keep from type ITEM_TROY_DISK (lure module)", "item_lure_module", -1, String::toInt))
                ),

                randomNextPokestopSelection = getPropertyIfSet("Number of pokestops to select next", "random_next_pokestop_selection", defaults.randomNextPokestopSelection, String::toInt),
                campLurePokestop = getPropertyIfSet("Camp around x lured pokestops", "camp_lure_pokestop", defaults.campLurePokestop, String::toInt),
                desiredCatchProbability = getPropertyIfSet("Desired chance to catch a Pokemon with 1 ball", "desired_catch_probability", defaults.desiredCatchProbability, String::toDouble),
                desiredCatchProbabilityUnwanted = getPropertyIfSet("Desired probability to catch unwanted Pokemon (obligatory_transfer; low IV; low CP)", "desired_catch_probability_unwanted", defaults.desiredCatchProbabilityUnwanted, String::toDouble),
                randomBallThrows = getPropertyIfSet("Randomize Ball Throwing", "random_ball_throws", defaults.randomBallThrows, String::toBoolean),
                waitBetweenThrows = getPropertyIfSet("Waiting between throws", "wait_between_throws", defaults.waitBetweenThrows, String::toBoolean),
                autotransfer = getPropertyIfSet("Autotransfer", "autotransfer", defaults.autotransfer, String::toBoolean),
                autotransferTimeDelay = getPropertyIfSet("Delay between each transfer", "autotransfer_time_delay", defaults.autotransferTimeDelay, String::toLong),
                keepPokemonAmount = getPropertyIfSet("minimum keep pokemon amount", "keep_pokemon_amount", defaults.keepPokemonAmount, String::toInt),
                maxPokemonAmount = getPropertyIfSet("maximum keep pokemon amount", "max_pokemon_amount", defaults.maxPokemonAmount, String::toInt),
                displayKeepalive = getPropertyIfSet("Display Keepalive Coordinates", "display_keepalive", defaults.displayKeepalive, String::toBoolean),

                displayPokestopName = getPropertyIfSet("Display Pokestop Name", "display_pokestop_name", defaults.displayPokestopName, String::toBoolean),
                displayPokestopRewards = getPropertyIfSet("Display Pokestop Rewards", "display_pokestop_rewards", defaults.displayPokestopRewards, String::toBoolean),
                displayPokemonCatchRewards = getPropertyIfSet("Display Pokemon Catch Rewards", "display_pokemon_catch_rewards", defaults.displayPokemonCatchRewards, String::toBoolean),
                displayIfPokemonFromLure = getPropertyIfSet("Display If Pokemon Was Caught From Lure", "display_if_pokemon_from_lure", defaults.displayIfPokemonFromLure, String::toBoolean),

                lootPokestop = getPropertyIfSet("Loot Pokestops", "loot_pokestop", defaults.lootPokestop, String::toBoolean),
                catchPokemon = getPropertyIfSet("Catch Pokemons", "catch_pokemon", defaults.catchPokemon, String::toBoolean),
                autoFillIncubator = getPropertyIfSet("Auto Fill Incubators", "auto_fill_incubator", defaults.autoFillIncubator, String::toBoolean),

                sortByIv = getPropertyIfSet("Sort by IV first instead of CP", "sort_by_iv", defaults.sortByIv, String::toBoolean),

                desiredCurveRate = getPropertyIfSet("Define curved balls probability", "desired_curve_rate", defaults.desiredCurveRate, String::toDouble),

                neverUseBerries = getPropertyIfSet("Never use berries", "never_use_berries", defaults.neverUseBerries, String::toBoolean),

                allowLeaveStartArea = getPropertyIfSet("Allow leaving the starting area", "allow_leave_start_area", defaults.allowLeaveStartArea, String::toBoolean),

                spawnRadius = getPropertyIfSet("Max distance from starting point the bot should ever go", "spawn_radius", defaults.spawnRadius, String::toInt),

                banSpinCount = getPropertyIfSet("Number of times the pokestop should be spun to attempt softban bypass", "ban_spin_count", defaults.banSpinCount, String::toInt),

                transferCpThreshold = getPropertyIfSet("Minimum CP to keep a pokemon", "transfer_cp_threshold", defaults.transferCpThreshold, String::toInt),

                transferCpMinThreshold = getPropertyIfSet("Minimum CP % in relation to max CP of pokemon to your current trainer lvl to keep pokemon", "transfer_cp_min_threshold", defaults.transferCpMinThreshold, String::toInt),

                transferIvThreshold = getPropertyIfSet("Minimum IV percentage to keep a pokemon", "transfer_iv_threshold", defaults.transferIvThreshold, String::toInt),

                ignoredPokemon = getPropertyIfSet("Never transfer these Pokemon", "ignored_pokemon", defaults.ignoredPokemon.map { it.name }.joinToString(","), String::toString).split(",").filter { it.isNotBlank() }.map { PokemonId.valueOf(it) },

                obligatoryTransfer = getPropertyIfSet("list of pokemon you always want to transfer regardless of CP", "obligatory_transfer", defaults.obligatoryTransfer.map { it.name }.joinToString(","), String::toString).split(",").filter { it.isNotBlank() }.map { PokemonId.valueOf(it) },

                evolveBeforeTransfer = getPropertyIfSet("list of pokemon you always want to evolve before transfer to maximize XP", "evolve_before_transfer", defaults.evolveBeforeTransfer.map { it.name }.joinToString(","), String::toString).split(",").filter { it.isNotBlank() }.map { PokemonId.valueOf(it) },

                neverCatchPokemon = getPropertyIfSet("list of pokemon you NEVER want to catch", "never_catch_pokemon", defaults.neverCatchPokemon.map { it.name }.joinToString(","), String::toString).split(",").filter { it.isNotBlank() }.map { PokemonId.valueOf(it) },

                evolveStackLimit = getPropertyIfSet("The stack of evolves needed to pop lucky egg and evolve all", "evolve_stack_limit", defaults.evolveStackLimit, String::toInt),

                useLuckyEgg = getPropertyIfSet("Use lucky egg before evolves", "use_lucky_egg", defaults.useLuckyEgg, String::toInt),

                evolveTimeDelay = getPropertyIfSet("Set time delay between evolutions", "evolve_time_delay", defaults.evolveTimeDelay, String::toLong),

                export = getPropertyIfSet("Export on Profile Update", "export", defaults.export, String::toString),

                guiPortSocket = getPropertyIfSet("Port where the socketserver should listen", "gui_port_socket", defaults.guiPortSocket, String::toInt),

                restApiPassword = getPropertyIfSet("REST API password for the bot", "rest_api_password", defaults.restApiPassword, String::toString),

                initialMapSize = getPropertyIfSet("Initial map size (S2 tiles) to fetch", "initial_map_size", defaults.initialMapSize, String::toInt),

                waitChance = getPropertyIfSet("Chance to wait on a pokestop", "wait_chance", defaults.waitChance, String::toDouble),

                waitTimeMin = getPropertyIfSet("Minimal time to wait", "wait_time_min", defaults.waitTimeMin, String::toInt),

                waitTimeMax = getPropertyIfSet("Maximum time to wait", "wait_time_max", defaults.waitTimeMax, String::toInt),

                botTimeoutAfterMinutes = getPropertyIfSet("Bot times out after X minutes and waits", "bot_timeout_after_minutes", defaults.botTimeoutAfterMinutes, String::toInt),
                botTimeoutAfterCatchingPokemon = getPropertyIfSet("Bot times out after X minutes and waits", "bot_timeout_after_catching_pokemon", defaults.botTimeoutAfterCatchingPokemon, String::toInt),
                botTimeoutAfterVisitingPokestops = getPropertyIfSet("Bot times out after X minutes and waits", "bot_timeout_after_visiting_pokestops", defaults.botTimeoutAfterVisitingPokestops, String::toInt),

                buddyPokemon = getPropertyIfSet("Desired buddy pokemon", "buddy_pokemon", defaults.buddyPokemon, String::toString)
        )
    }

    fun getPasswordProperty(): String {
        return if (properties.containsKey("password") && properties.getProperty("password").isNotBlank()) properties.getProperty("password") else String(Base64.getDecoder().decode(properties.getProperty("base64_password", "")))
    }

    fun <T> getPropertyOrDie(description: String, property: String, conversion: (String) -> T): T {
        val settingString = "$description setting (\"$property\")"

        if (!properties.containsKey(property)) {
            Log.red("$settingString not specified in config.properties!")
            System.exit(1)
        }

        val result: T?
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

@JsonIgnoreProperties("startingLocation", ignoreUnknown = true)
data class Settings(
        var name: String = "",

        var latitude: Double,
        var longitude: Double,
        var savedLatitude: Double = 0.0,
        var savedLongitude: Double = 0.0,
        val saveLocationOnShutdown: Boolean = true,

        val startingLocation: S2LatLng = S2LatLng.fromDegrees(latitude, longitude),
        val credentials: Credentials,

        val proxyServer: String = "",
        val proxyPort: Int = -1,
        var proxyType: String = "SOCKS",
        var proxyUsername: String = "",
        var proxyPassword: String = "",

        val speed: Double = 2.8,
        val randomSpeedRange: Double = 0.0,
        val followStreets: List<RouteProviderEnum> = emptyList(),
        val mapzenApiKey: String = "",
        val googleApiKey: String = "",
        val groupItemsByType: Boolean = false,
        val dropItems: Boolean = true,
        val itemDropDelay: Long = -1,
        val uselessItems: Map<ItemId, Int> = mapOf(
                Pair(ItemId.ITEM_REVIVE, 20),
                Pair(ItemId.ITEM_MAX_REVIVE, 10),
                Pair(ItemId.ITEM_POTION, 0),
                Pair(ItemId.ITEM_SUPER_POTION, 30),
                Pair(ItemId.ITEM_HYPER_POTION, 50),
                Pair(ItemId.ITEM_MAX_POTION, 50),
                Pair(ItemId.ITEM_POKE_BALL, 40),
                Pair(ItemId.ITEM_GREAT_BALL, 50),
                Pair(ItemId.ITEM_ULTRA_BALL, 50),
                Pair(ItemId.ITEM_MASTER_BALL, 10),
                Pair(ItemId.ITEM_RAZZ_BERRY, 40),
                Pair(ItemId.ITEM_LUCKY_EGG, -1),
                Pair(ItemId.ITEM_INCENSE_ORDINARY, -1),
                Pair(ItemId.ITEM_TROY_DISK, -1)
        ),

        val profileUpdateTimer: Long = 60,
        val timerWalkToStartPokestop: Long = -1L,
        val randomNextPokestopSelection: Int = 5,
        val campLurePokestop: Int = -1,
        val desiredCatchProbability: Double = 0.4,
        val desiredCatchProbabilityUnwanted: Double = 0.0,
        val autotransfer: Boolean = true,
        val autotransferTimeDelay: Long = -1,
        val randomBallThrows: Boolean = false,
        val waitBetweenThrows: Boolean = false,
        val keepPokemonAmount: Int = 1,
        val maxPokemonAmount: Int = -1,

        val displayKeepalive: Boolean = true,
        val displayPokestopName: Boolean = false,
        val displayPokestopRewards: Boolean = true,
        val displayPokemonCatchRewards: Boolean = true,
        val displayIfPokemonFromLure: Boolean = true,

        val lootPokestop: Boolean = true,
        var catchPokemon: Boolean = true,
        val autoFillIncubator: Boolean = true,

        val sortByIv: Boolean = false,
        val desiredCurveRate: Double = 0.0,
        val neverUseBerries: Boolean = true,
        val allowLeaveStartArea: Boolean = false,
        val spawnRadius: Int = -1,
        val banSpinCount: Int = 0,
        val transferCpThreshold: Int = 400,
        val transferCpMinThreshold: Int = -1,
        val transferIvThreshold: Int = 80,
        val ignoredPokemon: List<PokemonId> = listOf(PokemonId.EEVEE, PokemonId.MEWTWO, PokemonId.CHARMANDER),

        val obligatoryTransfer: List<PokemonId> = listOf(PokemonId.DODUO, PokemonId.RATTATA, PokemonId.CATERPIE, PokemonId.PIDGEY),

        val evolveBeforeTransfer: List<PokemonId> = listOf(PokemonId.CATERPIE, PokemonId.RATTATA, PokemonId.WEEDLE, PokemonId.PIDGEY),
        val neverCatchPokemon: List<PokemonId> = listOf(),
        val evolveStackLimit: Int = 100,
        val useLuckyEgg: Int = 1,
        val evolveTimeDelay: Long = 300,

        val export: String = "",

        val guiPortSocket: Int = 8001,

        var restApiPassword: String = "",

        var initialMapSize: Int = 9,

        val version: String = Settings.version,

        val waitChance: Double = 0.0,
        val waitTimeMin: Int = 0,
        val waitTimeMax: Int = 0,

        val botTimeoutAfterMinutes: Int = -1,
        val botTimeoutAfterCatchingPokemon: Int = -1,
        val botTimeoutAfterVisitingPokestops: Int = -1,

        val buddyPokemon: String = ""
) {
    fun withName(name: String): Settings {
        this.name = name
        return this
    }

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

    companion object Version {

        val version: String

        init {
            val versionProperties = Properties()
            version = try {
                SettingsParser::class.java.getResourceAsStream("version.properties").use {
                    versionProperties.load(it)
                }
                versionProperties["version"].toString()
            } catch (e: Exception) {
                ""
            }
        }
    }
}
