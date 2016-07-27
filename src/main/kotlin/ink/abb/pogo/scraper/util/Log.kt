/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper.util

import ink.abb.pogo.scraper.Context
import java.text.SimpleDateFormat
import java.util.*

class Log {
    companion object {
        private var ctx: Context? = null

        private val black_ = "\u001B[30m"
        private val red_ = "\u001B[31;1m"
        private val green_ = "\u001B[32;1m"
        private val yellow_ = "\u001B[33;1m"
        private val blue_ = "\u001B[34;1m"
        private val magenta_ = "\u001B[35;1m"
        private val cyan_ = "\u001B[36;1m"
        private val white_ = "\u001B[1m"
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

        fun normal(text: String = "") {
            output(text)
            ctx?.server?.sendLog("normal", text)
        }

        fun black(text: String = "") {
            output(text, black_)
            ctx?.server?.sendLog("black", text)
        }

        fun red(text: String = "") {
            output(text, red_)
            ctx?.server?.sendLog("red", text)
        }

        fun green(text: String = "") {
            output(text, green_)
            ctx?.server?.sendLog("green", text)
        }

        fun yellow(text: String = "") {
            output(text, yellow_)
            ctx?.server?.sendLog("yellow", text)
        }

        fun blue(text: String = "") {
            output(text, blue_)
            ctx?.server?.sendLog("blue", text)
        }

        fun magenta(text: String = "") {
            output(text, magenta_)
            ctx?.server?.sendLog("magenta", text)
        }

        fun cyan(text: String = "") {
            output(text, cyan_)
            ctx?.server?.sendLog("cyan", text)
        }

        fun white(text: String = "") {
            output(text, white_)
            ctx?.server?.sendLog("white", text)
        }

        fun setContext(ctx: Context){
            this.ctx = ctx
        }
    }
}