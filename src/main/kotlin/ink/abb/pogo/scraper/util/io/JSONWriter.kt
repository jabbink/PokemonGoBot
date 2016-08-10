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
import java.util.ArrayList


class JSONWriter(val filename: String = "export.json") {
    fun write(output: ArrayList<Array<String>>) {
        val mapper = ObjectMapper()

        mapper.writeValue(File(filename), output)
    }
}
