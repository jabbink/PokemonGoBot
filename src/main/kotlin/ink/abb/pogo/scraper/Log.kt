/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

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

        fun green(text: String) {
            println("$green_${format.format(Date())}: $text $reset")
        }

        fun normal(text: String) {
            println("${format.format(Date())}: $text")
        }

        fun red(text: String) {
            println("$red_${format.format(Date())}: $text $reset")
        }

        fun yellow(text: String) {
            println("$yellow_${format.format(Date())}: $text $reset")
        }

        fun white(text: String) {
            println("$white_${format.format(Date())}: $text $reset")
        }

        fun black(text: String) {
            println("$black_${format.format(Date())}: $text $reset")
        }
    }
}