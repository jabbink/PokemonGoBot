/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper

import ink.abb.pogo.scraper.util.camelToUnderscores
import ink.abb.pogo.scraper.util.underscoreToCamel
import org.junit.Assert
import org.junit.Test
import java.io.FileInputStream
import java.util.*
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
        val settingsFromCode = Settings(name="default", latitude = 0.0, longitude = 0.0, credentials = GoogleCredentials())

        Assert.assertEquals(settingsFromCode.toString().split(",").joinToString("\n"), settingsFromTemplate.toString().split(",").joinToString("\n"))
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
                "name"
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
            Assert.assertTrue("$templateName set in template, ${settingsName} not found in Settings", memberNames.contains(settingsName))
        }
        memberNames.forEach {
            val settingsName = it
            val templateName = settingsName.camelToUnderscores()
            Assert.assertNotNull("$settingsName set in Settings, ${templateName} not found in template", properties.get(templateName))
        }
    }

}
