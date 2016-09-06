/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper.util.io

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import ink.abb.pogo.scraper.Settings
import ink.abb.pogo.scraper.util.Log
import java.io.File

class SettingsJSONWriter {
    val mapper: ObjectMapper = ObjectMapper().registerKotlinModule()
    val root = File("./bot-settings").absoluteFile!!

    init {
        root.mkdirs()
    }

    fun getJSONConfigBotNames(): List<String> {
        return root.list().filter { it.endsWith(".json") }.map { it.replace(Regex("\\.json$"), "") }
    }

    fun save(settings: Settings) {
        Log.normal("Saving settings for ${settings.name}")
        File(root, "${settings.name}.json").bufferedWriter().use {
            it.write(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(settings))
        }
    }

    fun load(name: String): Settings {
        val save = File(root, "$name.json")
        if (!save.isFile) {
            throw IllegalArgumentException("No save file found for name: $name")
        }

        return mapper.readValue(save, Settings::class.java).withName(name)
    }
}
