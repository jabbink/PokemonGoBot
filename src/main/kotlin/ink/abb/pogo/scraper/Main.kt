/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper

import POGOProtos.Networking.Envelopes.RequestEnvelopeOuterClass
import POGOProtos.Networking.Envelopes.RequestEnvelopeOuterClass.RequestEnvelope.AuthInfo
import com.pokegoapi.api.PokemonGo
import com.pokegoapi.api.pokemon.Pokemon
import com.pokegoapi.auth.GoogleLogin
import com.pokegoapi.auth.GoogleLoginSecrets
import com.pokegoapi.auth.PtcLogin
import ink.abb.pogo.scraper.util.Log
import ink.abb.pogo.scraper.util.Helper
import okhttp3.OkHttpClient
import java.io.FileInputStream
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

fun getAuth(settings: Settings, http: OkHttpClient, retryCount: Int): AuthInfo? {
    var retries = retryCount

    var auth: AuthInfo?
    do {
        val username = settings.username
        val password = settings.password

        val token = settings.token()

        auth = if (token.isBlank()) {
            try {
                if (username.contains('@')) {
                    GoogleLogin(http).login()
                } else {
                    PtcLogin(http).login(username, password)
                }
            } catch (e: Exception) {
                retries--
                Log.red("Failed to get login token")
                if (retries > 0) {
                    Log.normal("Retrying...")
                    Helper.waitRandomSeconds(10,30)
                } else {
                    Log.red("Giving up")
                }
                null
            }
        } else {
            if (token.contains("pokemon.com")) {
                PtcLogin(http).login(token)
            } else {
                val tempGoogleAuth = GoogleLogin(http).refreshToken(token)
                if (tempGoogleAuth != null) {
                    GoogleLoginSecrets.refresh_token = token
                }
                tempGoogleAuth
            }
        }
    } while (retries > 0 && auth == null)

    if (auth == null) {
        Log.red("Could not get login token")
        System.exit(1)
    }

    return auth
}

fun getPokemonGo(settings: Settings, http: OkHttpClient): Pair<PokemonGo, AuthInfo> {
    val retryCount = 3

    // try to get a token 3 times; in case of stored token, immediately returns
    var auth = getAuth(settings, http, retryCount)

    // did not get AuthInfo? Means no token was set and it failed to login 3 times
    if (auth != null) {
        // Got a token: try to contact Niantic
        var api = try {
            PokemonGo(auth, http)
        } catch (e: Exception) {
            null
        }

        // Failed? Might have used an expired token
        if (api == null) {
            // do we have an (invalid) token?
            if (!settings.token().isBlank()) {
                // remove it
                settings.setToken("")
                // try logging in 3 times without a stored token
                auth = getAuth(settings, http, retryCount)
                // got a new auth?
                if (auth != null) {
                    // great, try to contact niantic again
                    api = try {
                        PokemonGo(auth, http)
                    } catch (e: Exception) {
                        null
                    }
                    if (api != null) {
                        // everything worked; return the api and auth
                        return Pair(api, auth)
                    }
                }
            }
            // even after resetting the token it failed; hopeless situation; exit

            Log.red("Could not login; are the servers online?")
            System.exit(1)
            throw Exception("Failed to log in")
        }
        // everything worked; return the api and auth
        return Pair(api, auth)
    }
    // we did not have a token stored and logging in failed
    Log.red("Could not login; are the servers online?")
    System.exit(1)
    throw Exception("Failed to log in")
}

fun login(): Pair<PokemonGo, AuthInfo> {
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

    Log.normal("Logging in to game server...")

    val (api, auth) = getPokemonGo(settings, http)

    var displayToken = auth.token.contents
    if (auth.provider.equals("google")) {
        displayToken = GoogleLoginSecrets.refresh_token
    }
    Log.normal("Logged in as ${settings.username} with token ${displayToken}")

    Log.normal("Setting this token in your config")
    settings.setToken(displayToken)
    settings.writeToken("config.properties")

    print("Getting profile data from pogo server")
    while (api.playerProfile == null) {
        print(".")
        Thread.sleep(2500)
    }
    println(".")

    return Pair(api, auth)
}

@Suppress("UNUSED_VARIABLE")
fun main(args: Array<String>) {

    val properties = Properties()

    val input = FileInputStream("config.properties")
    input.use {
        properties.load(it)
    }
    input.close()

    val settings = Settings(properties)

    val (api, auth) = login()
    
    val bot = Bot(api, settings)
    Runtime.getRuntime().addShutdownHook(thread(start = false) {bot.stop() })

    bot.start()
}
