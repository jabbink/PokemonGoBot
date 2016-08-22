/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import ink.abb.pogo.scraper.util.camelToUnderscores
import ink.abb.pogo.scraper.util.underscoreToCamel
import ink.abb.pogo.scraper.util.credentials.*
import org.junit.Assert
import org.junit.Test
import java.io.File
import java.io.FileInputStream
import java.util.*
import java.util.regex.Pattern
import kotlin.reflect.memberProperties

class TestSettings {

    @Test
    fun testDefaultSettings() {
        val properties = Properties()
        FileInputStream("config.properties.template").use {
            properties.load(it)
        }

        properties.setProperty("latitude", "0.0")
        properties.setProperty("longitude", "0.0")
        val settingsFromTemplate = SettingsParser(properties).createSettingsFromProperties()
        val settingsFromJsonTemplate = ObjectMapper().registerKotlinModule().readValue(File("json-template.json"), Settings::class.java).withName("default")
        val settingsFromCode = Settings(name = "default", latitude = 0.0, longitude = 0.0, credentials = GoogleCredentials())
        val settingsFromCodeForJson = Settings(name = "default", latitude = 0.0, longitude = 0.0, credentials = PtcCredentials())

        Assert.assertEquals(settingsFromCode.toString().split(",").joinToString("\n"), settingsFromTemplate.toString().split(",").joinToString("\n"))
        Assert.assertEquals(settingsFromCodeForJson.toString().split(",").joinToString("\n"), settingsFromJsonTemplate.toString().split(",").joinToString("\n"))
    }

    @Test
    fun testTemplateKeys() {
        // version should not be contained in the template
        val propertyBlacklist = setOf(
                // automatically injected; user shouldn't set it
                "version",
                // TODO: need to actually test these values
                "item_revive",
                "item_max_revive",
                "item_potion",
                "item_super_potion",
                "item_hyper_potion",
                "item_max_potion",
                "item_poke_ball",
                "item_great_ball",
                "item_ultra_ball",
                "item_master_ball",
                "item_razz_berry",
                "item_lucky_egg",
                "item_incense",
                "item_lure_module",
                "uselessItems",
                // Bot immediately converts this and stores it in password
                "base64_password",
                // Bot throws this away
                "token",
                "password",
                "username",
                "credentials",
                // Bot combines this in its own property
                "startingLocation",
                // only in JSON config
                "name",
                "type",
                "ITEM_REVIVE",
                "ITEM_MAX_REVIVE",
                "ITEM_SUPER_POTION",
                "ITEM_HYPER_POTION",
                "ITEM_MAX_POTION",
                "ITEM_POKE_BALL",
                "ITEM_GREAT_BALL",
                "ITEM_ULTRA_BALL",
                "ITEM_MASTER_BALL",
                "ITEM_RAZZ_BERRY",
                "ITEM_LUCKY_EGG",
                "ITEM_INCENSE_ORDINARY",
                "ITEM_TROY_DISK",
                "ITEM_POTION"
        )

        val properties = Properties()
        FileInputStream("config.properties.template").use {
            properties.load(it)
        }
        val memberNames = Settings::class.memberProperties.filter { !propertyBlacklist.contains(it.name) }.map { it.name }
        val propertyNames = properties.keys.map { it.toString() }.filter { !propertyBlacklist.contains(it) }
        propertyNames.forEach {
            val templateName = it
            val settingsName = templateName.underscoreToCamel()
            Assert.assertTrue("$templateName set in template, $settingsName not found in Settings", memberNames.contains(settingsName))
        }
        memberNames.forEach {
            val settingsName = it
            val templateName = settingsName.camelToUnderscores()
            Assert.assertNotNull("$settingsName set in Settings, $templateName not found in template", properties.get(templateName))
        }

        val jsonkeys = ArrayList<String>()
        val regex = "\"([\\w]*)\"\\s:\\s"
        val pattern = Pattern.compile(regex)
        File("json-template.json").forEachLine {
            val matcher = pattern.matcher(it)
            while (matcher.find()) {
                jsonkeys.add(matcher.group(1))
            }
        }
        val jsonNames = jsonkeys.filter { !propertyBlacklist.contains(it) }
        jsonNames.forEach {
            Assert.assertTrue("$it set in json template, $it not found in Settings", memberNames.contains(it))
        }
        memberNames.forEach {
            Assert.assertTrue("$it set in Settings, $it not found in json template", jsonkeys.contains(it))
        }
    }

}
