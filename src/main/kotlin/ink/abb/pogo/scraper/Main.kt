/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper

import POGOProtos.Inventory.ItemIdOuterClass.ItemId
import ink.abb.pogo.scraper.services.BotRunService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import java.io.FileInputStream
import java.util.*
import javax.annotation.PostConstruct

/**
 * Allow all certificate to debug with https://github.com/bettse/mitmdump_decoder
 */
@SpringBootApplication
open class Main {

    @Autowired
    lateinit var botRunService: BotRunService

    @PostConstruct
    fun createBot() {
        val name = "config-properties-conversion"

        val settings = try {
            botRunService.load(name)
        } catch (e: IllegalArgumentException) {
            val properties = Properties()
            FileInputStream("config.properties").use {
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

            Settings(
                name = name,
                credentials = credentials,
                startingLatitude = getPropertyOrDie(properties, "Starting Latitude", "latitude", String::toDouble),
                startingLongitude = getPropertyOrDie(properties, "Starting Longitude", "longitude", String::toDouble),
                speed = getPropertyIfSet(properties, "Speed", "speed", 2.778, String::toDouble),
                shouldDropItems = getPropertyIfSet(properties, "Item Drop", "drop_items", false, String::toBoolean),
                preferredBall = getPropertyIfSet(properties, "Preferred Ball", "preferred_ball", ItemId.ITEM_POKE_BALL, ItemId::valueOf),
                shouldAutoTransfer = getPropertyIfSet(properties, "Autotransfer", "autotransfer", false, String::toBoolean),
                shouldDisplayKeepalive = getPropertyIfSet(properties, "Display Keepalive Coordinates", "display_keepalive", true, String::toBoolean),
                transferCPThreshold = getPropertyIfSet(properties, "Minimum CP to keep a pokemon", "transfer_cp_threshold", 400, String::toInt),
                ignoredPokemon = getPropertyIfSet(properties, "Never transfer these Pokemon", "ignored_pokemon", "EEVEE,MEWTWO,CHARMENDER", String::toString).split(","),
                obligatoryTransfer = getPropertyIfSet(properties, "list of pokemon you always want to trancsfer regardless of CP", "obligatory_transfer", "DODUO,RATTATA,CATERPIE,PIDGEY", String::toString).split(",")
            )
        }

        botRunService.submitBot(settings)
    }

    private fun <T> getPropertyOrDie(properties: Properties, description: String, property: String, conversion: (String) -> T): T {
        val settingString = "$description setting (\"$property\")"

        if (!properties.containsKey(property)) {
            println("$settingString not specified in config.properties!")
            System.exit(1)
        }

        return conversion(properties.getProperty(property))
    }

    private fun <T> getPropertyIfSet(properties: Properties, description: String, property: String, default: T, conversion: (String) -> T): T {
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

fun main(args: Array<String>) {
    SpringApplication.run(Main::class.java, *args)
}
