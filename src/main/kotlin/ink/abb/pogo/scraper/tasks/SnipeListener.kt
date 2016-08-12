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
import com.fasterxml.jackson.databind.*
import ink.abb.pogo.scraper.util.Log

class SnipeListener : Task {
    override fun run(bot: Bot, ctx: Context, settings: Settings) {
        val clientSocket = Socket("localhost", 16969)

        while (true) {
            val dataReceived: BufferedReader
            val result: Map<*,*>
            try {
                dataReceived = BufferedReader(InputStreamReader(clientSocket.inputStream))
                result = ObjectMapper().readValue(dataReceived, Map::class.java)
            }
            catch (e: Exception) {
                continue
            }

            val latitude = result["Latitude"].toString().toDouble()
            val longitude = result["Longitude"].toString().toDouble()
            val pokemonId = result["Id"].toString().toInt()
            val pokemonName: String = (pokedexId2Names[pokemonId + 1]).toString()

            if (latitude == null || longitude == null)
            {
                continue
            }

            ctx.snipeLat.set(latitude)
            ctx.snipeLong.set(longitude)
            ctx.snipeName = pokemonName

            Log.green(text="ayo data: $result with name ${ctx.snipeName}")
            bot.task(SnipePokemon())
        }
    }
}