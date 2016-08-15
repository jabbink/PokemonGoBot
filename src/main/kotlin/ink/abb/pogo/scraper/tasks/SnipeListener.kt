/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper.tasks

import ink.abb.pogo.scraper.Bot
import ink.abb.pogo.scraper.Context
import ink.abb.pogo.scraper.Settings
import ink.abb.pogo.scraper.Task
import ink.abb.pogo.scraper.pokedexId2Names
import kotlin.collections.Map
import java.io.*
import java.net.*
import java.util.ArrayList
import com.fasterxml.jackson.databind.*
import ink.abb.pogo.scraper.util.Log

class SnipeListener : Task {
    override fun run(bot: Bot, ctx: Context, settings: Settings) {
        val clientSocket = Socket("localhost", settings.snipingPort)

        while (true) {
            val dataReceived: BufferedReader
            val multipleSnipes = ArrayList<Map<*,*>>()

            try {
                dataReceived = BufferedReader(InputStreamReader(clientSocket.inputStream))
                val results = dataReceived.readLine().split("\n")
                Log.red("$results")
                for (result in results)
                {
                    val result_map = ObjectMapper().readValue(result, Map::class.java)
                    multipleSnipes.add(result_map)
                }

            }
            catch (e: Exception) {
                continue
            }

            // We use a for loop because it processes multiple pokemon snipes at once.
            for (snipe in multipleSnipes)
            {
                val latitude = snipe["Latitude"].toString().toDouble()
                val longitude = snipe["Longitude"].toString().toDouble()
                val pokemonId = snipe["Id"].toString().toInt()
                val pokemonName: String = (pokedexId2Names[pokemonId + 1]).toString()

                Log.green(text="ayo data: $snipe with name $pokemonName")
                bot.task(SnipePokemon(latitude, longitude, pokemonName))
            }

        }
    }
}