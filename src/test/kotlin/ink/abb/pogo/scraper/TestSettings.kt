/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper

import org.junit.Assert
import org.junit.Test
import java.io.FileInputStream
import java.util.*

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
        val settingsFromCode = Settings(startingLatitude = 0.0, startingLongitude = 0.0, credentials = GoogleCredentials())

        Assert.assertEquals(settingsFromCode, settingsFromTemplate)
    }

}