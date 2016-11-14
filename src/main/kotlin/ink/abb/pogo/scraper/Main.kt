/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper

import com.google.common.util.concurrent.AtomicDouble
import ink.abb.pogo.api.PoGoApiImpl
import ink.abb.pogo.api.auth.CredentialProvider
import ink.abb.pogo.api.auth.GoogleAutoCredentialProvider
import ink.abb.pogo.api.auth.PtcCredentialProvider
import ink.abb.pogo.api.util.SystemTimeImpl
import ink.abb.pogo.scraper.services.BotService
import ink.abb.pogo.scraper.util.Log
import ink.abb.pogo.scraper.util.credentials.GoogleAutoCredentials
import ink.abb.pogo.scraper.util.credentials.GoogleCredentials
import ink.abb.pogo.scraper.util.credentials.PtcCredentials
import ink.abb.pogo.scraper.util.directions.getAltitude
import okhttp3.Credentials
import okhttp3.OkHttpClient
import org.springframework.boot.SpringApplication
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.net.InetSocketAddress
import java.net.Proxy
import java.nio.file.Paths
import java.util.*
import java.util.logging.LogManager
import javax.swing.text.rtf.RTFEditorKit

val time = SystemTimeImpl()

fun getAuth(settings: Settings, http: OkHttpClient): CredentialProvider {
    val credentials = settings.credentials
    val auth = if (credentials is GoogleCredentials) {
        Log.red("Google User Credential Provider is deprecated; Use google-auto")
        System.exit(1)
        null
    } else if (credentials is GoogleAutoCredentials) {
        GoogleAutoCredentialProvider(http, credentials.username, credentials.password, time)
    } else if (credentials is PtcCredentials) {
        PtcCredentialProvider(http, credentials.username, credentials.password, time)
    } else {
        throw IllegalStateException("Unknown credentials: ${credentials.javaClass}")
    }

    return auth!!
}

fun main(args: Array<String>) {
    LogManager.getLogManager().reset()
    SpringApplication.run(PokemonGoBotApplication::class.java, *args)
}

fun loadProperties(filename: String): Properties {
    val properties = Properties()
    Log.green("Trying to read ${Paths.get(filename).toAbsolutePath()}")
    var failed = false
    try {
        FileInputStream(filename).use {
            try {
                properties.load(it)
            } catch (e: Exception) {
                failed = true
            }
        }
    } catch (e: FileNotFoundException) {
        throw e
    }

    if (failed) {
        FileInputStream(filename).use {
            val rtfParser = RTFEditorKit()
            val document = rtfParser.createDefaultDocument()
            rtfParser.read(it.reader(), document, 0)
            val text = document.getText(0, document.length)
            properties.load(text.byteInputStream())
            Log.red("Config file encoded as Rich Text Format (RTF)!")
        }
    }
    return properties
}

fun startDefaultBot(http: OkHttpClient, service: BotService) {
    var properties: Properties? = null

    val attemptFilenames = arrayOf("config.properties", "config.properties.txt", "config.properties.rtf")

    val dir = File(System.getProperty("java.class.path")).absoluteFile.parentFile

    var filename = ""

    fileLoop@ for (path in arrayOf(Paths.get("").toAbsolutePath(), dir)) {
        for (attemptFilename in attemptFilenames) {
            try {
                filename = attemptFilename
                properties = loadProperties("${path.toString()}/$filename")
                break@fileLoop
            } catch (e: FileNotFoundException) {
                Log.red("$filename file not found")
            }
        }
    }

    if (properties == null) {
        Log.red("No config files found. Exiting.")
        System.exit(1)
        return
    } else {
        val settings = SettingsParser(properties).createSettingsFromProperties()
        service.addBot(startBot(settings, http))
    }
}


fun startBot(settings: Settings, http: OkHttpClient): Bot {

    var proxyHttp: OkHttpClient? = null

    if (!settings.proxyServer.equals("") && settings.proxyPort > 0) {
        Log.normal("Setting up proxy server for bot ${settings.name}: ${settings.proxyServer}:${settings.proxyPort}")

        val proxyType: Proxy.Type
        if (settings.proxyType.equals("HTTP"))
            proxyType = Proxy.Type.HTTP
        else if (settings.proxyType.equals("SOCKS"))
            proxyType = Proxy.Type.SOCKS
        else
            proxyType = Proxy.Type.DIRECT

        proxyHttp = http.newBuilder()
                .proxy(Proxy(proxyType, InetSocketAddress(settings.proxyServer, settings.proxyPort)))
                .proxyAuthenticator { route, response ->
                    response.request().newBuilder()
                            .header("Proxy-Authorization", Credentials.basic(settings.proxyUsername, settings.proxyPassword))
                            .build()
                }
                .build()
    }


    Log.normal("Logging in to game server...")

    val auth =
            if (proxyHttp == null) {
                getAuth(settings, http)
            } else {
                getAuth(settings, proxyHttp)
            }

    val api =
            if (proxyHttp == null)
                PoGoApiImpl(http, auth, time)
            else
                PoGoApiImpl(proxyHttp, auth, time)


    val lat = AtomicDouble(settings.latitude)
    val lng = AtomicDouble(settings.longitude)

    if (settings.saveLocationOnShutdown && settings.savedLatitude != 0.0 && settings.savedLongitude != 0.0) {
        lat.set(settings.savedLatitude)
        lng.set(settings.savedLongitude)
        Log.normal("Loaded last saved location (${settings.savedLatitude}, ${settings.savedLongitude})")
    }

    api.setLocation(lat.get(), lng.get(), 0.0)

    api.start()

    Log.normal("Logged in successfully")

    print("Getting profile data from pogo server")
    while (!api.initialized) {
        print(".")
        Thread.sleep(1000)
    }
    println(".")
    Thread.sleep(1000)

    val bot = Bot(api, settings)

    bot.start()

    return bot
}
