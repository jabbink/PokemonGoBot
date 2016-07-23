/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper

import POGOProtos.Inventory.Item.ItemIdOuterClass.ItemId
import com.pokegoapi.api.inventory.Pokeball
import java.util.*

class Settings(val properties: Properties) {

    val pokeballItems = mapOf(Pair(ItemId.ITEM_POKE_BALL, Pokeball.POKEBALL),
            Pair(ItemId.ITEM_ULTRA_BALL, Pokeball.ULTRABALL),
            Pair(ItemId.ITEM_GREAT_BALL, Pokeball.GREATBALL),
            Pair(ItemId.ITEM_MASTER_BALL, Pokeball.MASTERBALL))

    val startingLatitude = getPropertyOrDie("Starting Latitude", "latitude", String::toDouble)
    val startingLongitude = getPropertyOrDie("Starting Longitude", "longitude", String::toDouble)

    val username = properties.getProperty("username")
    val password = if (properties.containsKey("password")) properties.getProperty("password") else String(Base64.getDecoder().decode(properties.getProperty("base64_password", "")))
    val token = properties.getProperty("token", "")

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

    val preferredBall = getPropertyIfSet("Preferred Ball", "preferred_ball", ItemId.ITEM_POKE_BALL, ItemId::valueOf)
    val shouldAutoTransfer = getPropertyIfSet("Autotransfer", "autotransfer", false, String::toBoolean)
    val shouldDisplayKeepalive = getPropertyIfSet("Display Keepalive Coordinates", "display_keepalive", true, String::toBoolean)

    val shouldDisplayWalkingToNearestUnused = getPropertyIfSet("Display Walking to nearest Unused Pokestop", "display_walking_nearest_unused", false, String::toBoolean)
    val shouldDisplayPokestopSpinRewards = getPropertyIfSet("Display Pokestop Rewards", "display_pokestop_rewards", true, String::toBoolean)
    val shouldDisplayPokemonCatchRewards = getPropertyIfSet("Display Pokemon Catch Rewards", "display_pokemon_catch_rewards", true, String::toBoolean)

    val walkOnly = getPropertyIfSet("Only walk to hatch eggs", "walk_only", false, String::toBoolean)

    val transferCPThreshold = getPropertyIfSet("Minimum CP to keep a pokemon", "transfer_cp_threshold", 400, String::toInt)

    val transferIVThreshold = getPropertyIfSet("Minimum IV percentage to keep a pokemon", "transfer_iv_threshold", 80, String::toInt)

    val ignoredPokemon = if (shouldAutoTransfer) {
        getPropertyIfSet("Never transfer these Pokemon", "ignored_pokemon", "EEVEE,MEWTWO,CHARMANDER", String::toString).split(",")
    } else {
        listOf()
    }
    val obligatoryTransfer = if (shouldAutoTransfer) {
        getPropertyIfSet("list of pokemon you always want to trancsfer regardless of CP", "obligatory_transfer", "DODUO,RATTATA,CATERPIE,PIDGEY", String::toString).split(",")
    } else {
        listOf()
    }

    private fun <T> getPropertyOrDie(description: String, property: String, conversion: (String) -> T): T {
        val settingString = "$description setting (\"$property\")"

        if (!properties.containsKey(property)) {
            println("$settingString not specified in config.properties!")
            System.exit(1)
        }

        return conversion(properties.getProperty(property))
    }

    private fun <T> getPropertyIfSet(description: String, property: String, default: T, conversion: (String) -> T): T {
        val settingString = "$description setting (\"$property\")"
        val defaulting = "defaulting to \"$default\""

        if (!properties.containsKey(property)) {
            println("$settingString not specified, $defaulting.")
            return default
        }

        try {
            return conversion(properties.getProperty(property))
        } catch (e: Exception) {
            println("$settingString is invalid, defaulting to $default: ${e.message}")
            return default
        }
    }
}
