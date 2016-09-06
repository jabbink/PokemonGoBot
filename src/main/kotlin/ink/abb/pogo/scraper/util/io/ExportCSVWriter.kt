/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper.util.io

import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.util.*

class ExportCSVWriter(val filename: String = "export.csv", val delimiter: String = ",") {
    fun write(profile: Map<String, String>, eggs: ArrayList<Map<String, String>>, items: ArrayList<Map<String, String>>, pokemons: ArrayList<Map<String, String>>) {
        // UTF-8 with BOM to fix borked UTF-8 chars in MS Excel (for nickname output)
        // https://en.wikipedia.org/wiki/Byte_order_mark#UTF-8
        FileOutputStream(filename).use {
            it.write(239)
            it.write(187)
            it.write(191)
            val pw = PrintWriter(OutputStreamWriter(it, "UTF-8"))

            // Print Profile
            pw.println("Overview Profile")
            for ((key, value) in profile) {
                pw.println(createCSVLine(arrayOf(key, value).toSet(), delimiter))
            }
            pw.println("")

            // Print Eggs
            pw.println("Overview Eggs")
            pw.println(createCSVLine(eggs.first().keys, delimiter))

            for (egg in eggs) {
                pw.println(createCSVLine(egg.values, delimiter))
            }
            pw.println("")

            // Print Items
            pw.println("Overview Items")
            pw.println(createCSVLine(items.first().keys, delimiter))

            for (item in items) {
                pw.println(createCSVLine(item.values, delimiter))
            }
            pw.println("")

            // Print pokemons
            pw.println("Overview Pokebank")
            pw.println(createCSVLine(pokemons.first().keys, delimiter))

            for (pokemon in pokemons) {
                pw.println(createCSVLine(pokemon.values, delimiter))
            }

            pw.close()
        }
    }

    private fun createCSVLine(line: Collection<String>, delimiter: String): String {
        val sb = StringJoiner(delimiter)
        for (value in line) {
            var result = value

            // https://tools.ietf.org/html/rfc4180
            if (result.contains("\"")) {
                result = result.replace("\"", "\"\"")
            }

            sb.add(result)
        }

        return sb.toString()
    }
}
