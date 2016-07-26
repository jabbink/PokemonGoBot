/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper

import com.pokegoapi.api.PokemonGo
import com.pokegoapi.auth.*
import com.pokegoapi.exceptions.LoginFailedException
import com.pokegoapi.exceptions.RemoteServerException
import ink.abb.pogo.scraper.util.Log
import okhttp3.OkHttpClient
import java.io.FileInputStream
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

fun getAuth(settings: Settings, http: OkHttpClient): CredentialProvider {
    val username = settings.username
    val password = settings.password

    val token = settings.token()

    val auth = if (username.contains('@')) {
        if (token.isBlank()) {
            GoogleCredentialProvider(http, object : GoogleCredentialProvider.OnGoogleLoginOAuthCompleteListener {
                override fun onInitialOAuthComplete(googleAuthJson: GoogleAuthJson?) {
                }

                override fun onTokenIdReceived(googleAuthTokenJson: GoogleAuthTokenJson) {
                    Log.normal("Setting Google refresh token in your config")
                    settings.setToken(googleAuthTokenJson.refreshToken)
                    settings.writeToken("config.properties")
                }
            })
        } else {
            GoogleCredentialProvider(http, token)
        }
    } else {
        PtcCredentialProvider(http, username, password)
    }

    return auth
}

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

    Log.normal("Logging in to game server...")

    val retryCount = 3
    val errorTimeout = 1000L

    var retries = retryCount

    var auth: CredentialProvider? = null
    do {
        try {
            auth = getAuth(settings, http)
        } catch (e: LoginFailedException) {
            Log.red("Server refused your login credentials. Are they correct?")
            System.exit(1)
            return
        } catch (e: RemoteServerException) {
            Log.red("Server returned unexpected error")
            if (retries-- > 0) {
                Log.normal("Retrying...")
                Thread.sleep(errorTimeout)
            }
        }
    } while (auth == null && retries >= 0)

    retries = retryCount

    var api: PokemonGo? = null
    do {
        try {
            api = PokemonGo(auth, http)
        } catch (e: LoginFailedException) {
            Log.red("Server refused your login credentials. Are they correct?")
            System.exit(1)
            return
        } catch (e: RemoteServerException) {
            Log.red("Server returned unexpected error")
            if (retries-- > 0) {
                Log.normal("Retrying...")
                Thread.sleep(errorTimeout)
            }
        }
    } while (api == null && retries >= 0)

    if (api == null) {
        Log.red("Failed to login. Stopping")
        System.exit(1)
        return
    }

    Log.normal("Logged in as ${settings.username}")

    print("Getting profile data from pogo server")
    while (api.playerProfile == null) {
        print(".")
        Thread.sleep(1000)
    }
    println(".")

    val bot = Bot(api, settings)
    Runtime.getRuntime().addShutdownHook(thread(start = false) { bot.stop() })

    bot.start()
}
