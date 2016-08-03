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

class CSVWriter(val delimiter: String = ",") {
    fun write(output: ArrayList<Array<String>>) {
        // UTF-8 with BOM to fix borked UTF-8 chars in MS Excel (for nickname output)
        // https://en.wikipedia.org/wiki/Byte_order_mark#UTF-8
        FileOutputStream("export.csv").use {
            it.write(239)
            it.write(187)
            it.write(191)
            val pw = PrintWriter(OutputStreamWriter(it, "UTF-8"))

            for (line in output) {
                pw.println(createCSVLine(line, delimiter))
            }

            pw.close()
        }
    }

    private fun createCSVLine(line: Array<String>, delimiter: String): String {
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