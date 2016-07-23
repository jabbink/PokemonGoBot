/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper

import POGOProtos.Inventory.ItemIdOuterClass
import POGOProtos.Inventory.ItemIdOuterClass.ItemId
import com.pokegoapi.api.inventory.Pokeball
import java.util.*

class Settings(val properties: Properties) {

    val pokeballItems = linkedMapOf(
            Pair(ItemIdOuterClass.ItemId.ITEM_MASTER_BALL, Pokeball.MASTERBALL),
            Pair(ItemIdOuterClass.ItemId.ITEM_ULTRA_BALL, Pokeball.ULTRABALL),
            Pair(ItemIdOuterClass.ItemId.ITEM_GREAT_BALL, Pokeball.GREATBALL),
            Pair(ItemIdOuterClass.ItemId.ITEM_POKE_BALL, Pokeball.POKEBALL))

    val uselessItems = mapOf(
            Pair(ItemId.ITEM_REVIVE, 20),
            Pair(ItemId.ITEM_MAX_REVIVE, 10),
            Pair(ItemId.ITEM_POTION, 0),
            Pair(ItemId.ITEM_SUPER_POTION, 10),
            Pair(ItemId.ITEM_HYPER_POTION, 50),
            Pair(ItemId.ITEM_MAX_POTION, 50),
            Pair(ItemId.ITEM_POKE_BALL, 50),
            Pair(ItemId.ITEM_GREAT_BALL, 50),
            Pair(ItemId.ITEM_ULTRA_BALL, 50),
            Pair(ItemId.ITEM_MASTER_BALL, 30),
            Pair(ItemId.ITEM_RAZZ_BERRY, 30)
    )

    val startingLatitude = getPropertyOrDie("Starting Latitude", "latitude", String::toDouble)
    val startingLongitude = getPropertyOrDie("Starting Longitude", "longitude", String::toDouble)

    val username = properties.getProperty("username")
    val password = if (properties.containsKey("password")) properties.getProperty("password") else String(Base64.getDecoder().decode(properties.getProperty("base64_password", "")))
    val token = properties.getProperty("token", "")

    val speed = getPropertyIfSet("Speed", "speed", 2.778, String::toDouble)
    val shouldDropItems = getPropertyIfSet("Item Drop", "drop_items", false, String::toBoolean)
    val preferredBall = getPropertyIfSet("Preferred Ball", "preferred_ball", ItemId.ITEM_POKE_BALL, ItemId::valueOf)
    val shouldAutoTransfer = getPropertyIfSet("Autotransfer", "autotransfer", false, String::toBoolean)
    val shouldDisplayKeepAlive = getPropertyIfSet("Display Keepalive Coordinates", "display_keepalive", false, String::toBoolean)
    val transferCPThreshold = getPropertyIfSet("Minimum CP to keep a pokemon", "transfer_cp_threshold", 400, String::toInt)
    val ignoredPokemon = if(shouldAutoTransfer) getPropertyIfSet("Never transfer these Pokemon", "ignored_pokemon", "EEVEE,MEWTWO,CHARMENDER", String::toString).split(",").toSet() else emptySet()
    val obligatoryTransfer = if (shouldAutoTransfer) getPropertyIfSet("list of pokemon you always want to trancsfer regardless of CP", "obligatory_transfer", "DODUO,RATTATA,CATERPIE,PIDGEY", String::toString).split(",").toSet() else emptySet()

    val ultraBallPrefOverride = getPropertyIfSet("Use ultra ball for the first pick before falling back to others", "ultra_ball_first", "", String::toString).split(",").toSet()
    val greatBallPrefOverride = getPropertyIfSet("Use great ball for the first pick before falling back to others", "great_ball_first", "", String::toString).split(",").toSet()
    val masterBallPrefOverride = getPropertyIfSet("Use master ball for the first pick before falling back to others", "master_ball_first", "", String::toString).split(",").toSet()

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
