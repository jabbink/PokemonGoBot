/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper.gui

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import java.util.stream.Collectors

class WebServer {
    @Throws(Exception::class)
    fun start(port: Int, socketPort: Int) {
        Thread(){
            val server = HttpServer.create(InetSocketAddress(port), 0)
            server.createContext("/", RootHandler(socketPort))
            server.executor = null
            server.start()
        }.start()
    }

    inner class RootHandler(val socketPort: Int) : HttpHandler {

        @Throws(IOException::class)
        override fun handle(t: HttpExchange) {

            val string = BufferedReader(InputStreamReader(WebServer::class.java.getResourceAsStream("index.html")))
                    .lines().collect(Collectors.joining("\n"))
                    .replace("{{socketPort}}", socketPort.toString())
            val bytes = string.toByteArray(Charset.forName("UTF-8"))
            t.responseHeaders.set("Content-Type", "text/html; charset=UTF-8")
            t.responseHeaders.set("Access-Control-Allow-Origin", "*")
            t.sendResponseHeaders(200, bytes.size.toLong())
            t.responseBody.write(bytes)
            t.responseBody.close()
        }
    }
}