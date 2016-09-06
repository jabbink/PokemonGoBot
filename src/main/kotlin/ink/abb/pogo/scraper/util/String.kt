/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper.util

/**
 * Takes a camel cased identifier name and returns an underscore separated
 * name
 *
 * Example:
 *     String.camelToUnderscores("thisIsA1Test") == "this_is_a_1_test"
 */
fun String.camelToUnderscores() = "[A-Z\\d]".toRegex().replace(this, {
    "_" + it.groups[0]!!.value.toLowerCase()
})

/*
 * Takes an underscore separated identifier name and returns a camel cased one
 *
 * Example:
 *    String.underscoreToCamel("this_is_a_1_test") == "thisIsA1Test"
 */

fun String.underscoreToCamel() = "_([a-z\\d])".toRegex().replace(this, {
    it.groups[1]!!.value.toUpperCase()
})
