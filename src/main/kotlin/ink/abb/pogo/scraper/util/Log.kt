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

        private val green_ = "\u001B[32m"
        private val black_ = "\u001B[30m"
        private val yellow_ = "\u001B[33m"
        private val red_ = "\u001B[31m"
        private val white_ = "\u001B[37m"
        private val reset = "\u001B[0m"

        var format = SimpleDateFormat("dd MMM HH:mm:ss")

        fun setContext(ctx: Context){
            this.ctx = ctx
        }

        fun green(text: String) {
            println("${green_}${format.format(Date())}: $text ${reset}")
            ctx?.server?.sendLog("green", text)
        }

        fun normal(text: String) {
            println("${format.format(Date())}: $text")
            ctx?.server?.sendLog("normal", text)
        }

        fun red(text: String) {
            println("${red_}${format.format(Date())}: $text ${reset}")
            ctx?.server?.sendLog("red", text)
        }

        fun yellow(text: String) {
            println("${yellow_}${format.format(Date())}: $text ${reset}")
            ctx?.server?.sendLog("yellow", text)
        }

        fun white(text: String) {
            println("${white_}${format.format(Date())}: $text ${reset}")
        }

        fun black(text: String) {
            println("${black_}${format.format(Date())}: $text ${reset}")
        }
    }
}