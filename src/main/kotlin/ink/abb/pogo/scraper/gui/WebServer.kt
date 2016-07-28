/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper.gui

import ink.abb.pogo.scraper.Context
import spark.Request
import spark.Response
import spark.Spark.get
import spark.Spark.port
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.stream.Collectors

class WebServer {

    fun start(ctx: Context, port: Int, socketPort: Int) {
        port(port)
        get("/") { request: Request, response: Response ->
            val string = BufferedReader(InputStreamReader(WebServer::class.java.getResourceAsStream("index.html")))
                    .lines().collect(Collectors.joining("\n"))
                    .replace("{{socketPort}}", socketPort.toString())
                    .replace("{{username}}", ctx.api.playerProfile.username)
            response.header("Access-Control-Allow-Origin", "*")
            string
        }
    }
}