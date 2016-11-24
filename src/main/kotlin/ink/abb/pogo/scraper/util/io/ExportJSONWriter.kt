/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper.util.io

import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File
import java.util.*

class ExportJSONWriter(val filename: String = "export.json") {
    fun write(profile: Map<String, String>, eggs: ArrayList<Map<String, String>>, items: ArrayList<Map<String, String>>, pokemons: ArrayList<Map<String, String>>) {
        val mapper = ObjectMapper()
        val export = JSON_export(profile, eggs, items, pokemons)

        mapper.writerWithDefaultPrettyPrinter().writeValue(File(filename), export)
    }
}

data class JSON_export(
        var profile: Map<String, String>,
        var eggs: ArrayList<Map<String, String>>,
        var items: ArrayList<Map<String, String>>,
        var pokemons: ArrayList<Map<String, String>>
)
