/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper.util

import java.text.SimpleDateFormat
import java.util.*

class Log {

    companion object {

        private val green_ = "\u001B[32m"
        private val black_ = "\u001B[30m"
        private val yellow_ = "\u001B[33m"
        private val red_ = "\u001B[31m"
        private val white_ = "\u001B[37m"
        private val reset = "\u001B[0m"

        var format = SimpleDateFormat("dd MMM HH:mm:ss")

        private fun output(text: String, color: String? = null) {
            val output = "${format.format(Date())}: $text"
            if (color != null) {
                println("${color}$output ${reset}")
            } else {
                println(output)
            }
        }

        fun green(text: String = "") {
            output(text, green_)
        }

        fun normal(text: String = "") {
            println(text)
        }

        fun red(text: String = "") {
            output(text, red_)
        }

        fun yellow(text: String = "") {
            output(text, yellow_)
        }

        fun white(text: String = "") {
            output(text, white_)
        }

        fun black(text: String = "") {
            output(text, black_)
        }
    }
}