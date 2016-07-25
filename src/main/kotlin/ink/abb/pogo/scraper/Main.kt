/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper

import POGOProtos.Networking.Envelopes.RequestEnvelopeOuterClass
import com.pokegoapi.api.PokemonGo
import com.pokegoapi.auth.GoogleLogin
import com.pokegoapi.auth.GoogleLoginSecrets
import com.pokegoapi.auth.PtcLogin
import ink.abb.pogo.scraper.util.Log
import okhttp3.OkHttpClient
import java.io.FileInputStream
import java.util.*
import java.util.concurrent.TimeUnit

fun main(args: Array<String>) {
    val builder = OkHttpClient.Builder()
    builder.connectTimeout(60, TimeUnit.SECONDS)
    builder.readTimeout(60, TimeUnit.SECONDS)
    builder.writeTimeout(60, TimeUnit.SECONDS)
    val http = builder.build()

    val properties = Properties()

    val input = FileInputStream("config.properties")
    input.use {
        properties.load(it)
    }
    input.close()

    val settings = Settings(properties)

    val username = settings.username
    val password = settings.password

    val token = settings.token

    Log.normal("Logging in to game server...")
    val auth: RequestEnvelopeOuterClass.RequestEnvelope.AuthInfo

    auth = if (token.isBlank()) {
        if (username.contains('@')) {
            GoogleLogin(http).login()
        } else {
            PtcLogin(http).login(username, password)
        }
    } else {
        if (token.contains("pokemon.com")) {
            PtcLogin(http).login(token)
        } else {
            GoogleLogin(http).refreshToken(token)
        }
    }
    var displayToken = auth.token.contents
    if (auth.provider.equals("google")) {
        displayToken = GoogleLoginSecrets.refresh_token
    }
    Log.normal("Logged in as $username with token ${displayToken}")

    if (token.isBlank()) {
        Log.normal("Setting this token in your config")
        settings.setToken(auth.token.contents)
        settings.writeToken("config.properties")
    }
    val api = PokemonGo(auth, http)

    print("Getting profile data from pogo server")
    while (api.playerProfile == null) {
        print(".")
        Thread.sleep(1000)
    }
    println(".")

    Bot(api, settings).run()
}
