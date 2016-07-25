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
import ink.abb.pogo.scraper.services.BotRunService
import ink.abb.pogo.scraper.util.Log
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import java.io.File
import java.util.Base64
import java.util.Properties
import javax.annotation.PostConstruct
import kotlin.reflect.companionObject

/**
 * Allow all certificate to debug with https://github.com/bettse/mitmdump_decoder
 */
@SpringBootApplication
open class Main {

    @Autowired
    lateinit var botRunService: BotRunService

    @PostConstruct
    fun createBot() {
        val names = botRunService.getSaveNames()

        if(names.size < 1) {
            val configProperties = File("config.properties")
            if(!configProperties.isFile) {
                throw IllegalStateException("No bot saves found and no config.properties to convert!")
            }

            val properties = Properties()
            configProperties.reader().use {
                properties.load(it)
            }

            val username = properties.getProperty("username", "")
            val password = if (properties.containsKey("password")) properties.getProperty("password") else String(Base64.getDecoder().decode(properties.getProperty("base64_password", "")))
            val token = properties.getProperty("token", "")

            val credentials = if(username.isEmpty() || username.contains('@')) {
                Settings.GoogleCredentials(token)
            } else {
                Settings.PokemonTrainersClubCredentials(username, password, token)
            }

            botRunService.submitBot(Settings(
                name = "config-properties-conversion",
                credentials = credentials,
                startingLatitude = getPropertyOrDie(properties, "Starting Latitude", "latitude", String::toDouble),
                startingLongitude = getPropertyOrDie(properties, "Starting Longitude", "longitude", String::toDouble),
                desiredCatchProbability = getPropertyIfSet(properties, "Catch Probability", "desired_catch_probability", 0.8, String::toDouble),
                speed = getPropertyIfSet(properties, "Speed", "speed", 2.778, String::toDouble),
                shouldHatchEggs = getPropertyIfSet(properties, "Should Hatch Eggs", "should_hatch_eggs", true, String::toBoolean),

                shouldDropItems = getPropertyIfSet(properties, "Item Drop", "drop_items", false, String::toBoolean),
                //TODO = getPropertyIfSet(properties, "Revive", "item_revive", 20, String::toInt),
                //TODO = getPropertyIfSet(properties, "Max Revive", "item_max_revive", 10, String::toInt),
                //TODO = getPropertyIfSet(properties, "Potion", "item_potion", 0, String::toInt),
                //TODO = getPropertyIfSet(properties, "Super Potion", "item_super_potion", 30, String::toInt),
                //TODO = getPropertyIfSet(properties, "Hyper Potion", "item_max_potion", 50, String::toInt),
                //TODO = getPropertyIfSet(properties, "Max Potion", "item_poke_ball", 50, String::toInt),
                //TODO = getPropertyIfSet(properties, "Poke Ball", "item_great_ball", 50, String::toInt),
                //TODO = getPropertyIfSet(properties, "Great Ball", "item_ultra_ball", 50, String::toInt),
                //TODO = getPropertyIfSet(properties, "Ultra Ball", "item_ultra_ball", 50, String::toInt),
                //TODO = getPropertyIfSet(properties, "Master Ball", "item_master_ball", -1, String::toInt),
                //TODO = getPropertyIfSet(properties, "Razzberry", "item_razz_berry", 50, String::toInt),

                preferredBall = getPropertyIfSet(properties, "Preferred Ball", "preferred_ball", ItemId.ITEM_POKE_BALL, ItemId::valueOf),

                shouldDisplayKeepalive = getPropertyIfSet(properties, "Display Keepalive Coordinates", "display_keepalive", true, String::toBoolean),
                shouldDisplayPokestopName = getPropertyIfSet(properties, "Display Pokestop Name", "display_pokestop_name", true, String::toBoolean),
                shouldDisplayWalkingToNearestUnused = getPropertyIfSet(properties, "Display Walking to Nearest Unused Pokestop", "display_nearest_unused", true, String::toBoolean),
                shouldDisplayPokestopSpinRewards = getPropertyIfSet(properties, "Display Pokestop Rewards", "display_pokestop_rewards", true, String::toBoolean),
                shouldDisplayPokemonCatchRewards = getPropertyIfSet(properties, "Display Pokemon Catch Rewards", "display_pokemon_catch_rewards", true, String::toBoolean),

                shouldAutoTransfer = getPropertyIfSet(properties, "Autotransfer", "autotransfer", false, String::toBoolean),
                keepPokemonAmount = getPropertyIfSet(properties, "Keep Pokemon Amount", "keep_pokemon_amount", 1, String::toInt),
                sortByIV = getPropertyIfSet(properties, "Sort by IV first instead of CP", "sort_by_iv", false, String::toBoolean),
                transferIVThreshold = getPropertyIfSet(properties, "Minimum IV to keep a pokemon", "transfer_iv_threshold", 80, String::toInt),
                transferCPThreshold = getPropertyIfSet(properties, "Minimum CP to keep a pokemon", "transfer_cp_threshold", 400, String::toInt),
                walkOnly = getPropertyIfSet(properties, "Only hatch eggs, no catching", "walk_only", false, String::toBoolean),
                ignoredPokemon = getPropertyIfSet(properties, "Never transfer these Pokemon", "ignored_pokemon", "MISSINGNO", String::toString).split(",").map { PokemonId.valueOf(it) },
                obligatoryTransfer = getPropertyIfSet(properties, "List of pokemon you always want to transfer regardless of CP", "obligatory_transfer", "DODUO,RATTATA,CATERPIE,PIDGEY", String::toString).split(",").map { PokemonId.valueOf(it) }
            ))
        } else {
            names.map { botRunService.load(it) }.forEach { botRunService.submitBot(it) }
        }
    }

    private fun <T> getPropertyOrDie(properties: Properties, description: String, property: String, conversion: (String) -> T): T {
        val settingString = "$description setting (\"$property\")"

        if (!properties.containsKey(property)) {
            Log.red("$settingString not specified in config.properties!")
            System.exit(1)
        }

        return conversion(properties.getProperty(property))
    }

    private fun <T> getPropertyIfSet(properties: Properties, description: String, property: String, default: T, conversion: (String) -> T): T {
        val settingString = "$description setting (\"$property\")"
        val defaulting = "defaulting to \"$default\""

        if (!properties.containsKey(property)) {
            Log.red("$settingString not specified, $defaulting.")
            return default
        }

        try {
            return conversion(properties.getProperty(property))
        } catch (e: Exception) {
            Log.red("$settingString is invalid, defaulting to $default: ${e.message}")
            return default
        }
    }
}

fun <R : Any> R.logger(): Lazy<Logger> {
    return lazy { LoggerFactory.getLogger(unwrapCompanionClass(this.javaClass).name) }
}

fun <T: Any> unwrapCompanionClass(ofClass: Class<T>): Class<*> {
    return if (ofClass.enclosingClass != null && ofClass.enclosingClass.kotlin.companionObject?.java == ofClass) {
        ofClass.enclosingClass
    } else {
        ofClass
    }
}

fun main(args: Array<String>) {
    SpringApplication.run(Main::class.java, *args)
}
