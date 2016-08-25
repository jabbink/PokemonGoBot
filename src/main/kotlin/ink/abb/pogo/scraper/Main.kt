/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper

import POGOProtos.Enums.TutorialStateOuterClass
import POGOProtos.Networking.Requests.Messages.GetPlayerMessageOuterClass
import POGOProtos.Networking.Requests.Messages.MarkTutorialCompleteMessageOuterClass
import POGOProtos.Networking.Requests.RequestTypeOuterClass
import com.pokegoapi.api.PokemonGo
import com.pokegoapi.api.device.DeviceInfo
import com.pokegoapi.auth.CredentialProvider
import com.pokegoapi.auth.GoogleAutoCredentialProvider
import com.pokegoapi.auth.GoogleUserCredentialProvider
import com.pokegoapi.auth.PtcCredentialProvider
import com.pokegoapi.exceptions.LoginFailedException
import com.pokegoapi.exceptions.RemoteServerException
import com.pokegoapi.main.ServerRequest
import com.pokegoapi.util.SystemTimeImpl
import ink.abb.pogo.scraper.controllers.ProgramController
import ink.abb.pogo.scraper.services.BotService
import ink.abb.pogo.scraper.util.Log
import ink.abb.pogo.scraper.util.credentials.GoogleAutoCredentials
import ink.abb.pogo.scraper.util.credentials.GoogleCredentials
import ink.abb.pogo.scraper.util.credentials.PtcCredentials
import ink.abb.pogo.scraper.util.toHexString
import okhttp3.Credentials
import okhttp3.OkHttpClient
import org.springframework.boot.SpringApplication
import org.springframework.context.ApplicationContext
import org.springframework.context.ConfigurableApplicationContext
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

fun getAuth(settings: Settings, http: OkHttpClient, writeToken: (String) -> Unit): CredentialProvider {
    val credentials = settings.credentials
    val auth = if (credentials is GoogleCredentials) {
        if (credentials.token.isBlank()) {
            val provider = GoogleUserCredentialProvider(http, time)

            println("Please go to " + GoogleUserCredentialProvider.LOGIN_URL)
            println("Enter authorisation code:")

            val access = readLine()

            // we should be able to login with this token
            provider.login(access)
            println("Refresh token:" + provider.refreshToken)
            Log.normal("Setting Google refresh token in your config")
            credentials.token = provider.refreshToken
            writeToken(credentials.token)

            provider
        } else {
            GoogleUserCredentialProvider(http, credentials.token, time)
        }
    } else if (credentials is GoogleAutoCredentials) {
        GoogleAutoCredentialProvider(http, credentials.username, credentials.password, time)
    } else if (credentials is PtcCredentials) {
        try {
            PtcCredentialProvider(http, credentials.username, credentials.password, time)
        } catch (e: LoginFailedException) {
            throw e
        } catch (e: RemoteServerException) {
            throw e
        } catch (e: Exception) {
            // sometimes throws ArrayIndexOutOfBoundsException or other RTE's
            throw RemoteServerException(e)
        }
    } else {
        throw IllegalStateException("Unknown credentials: ${credentials.javaClass}")
    }

    return auth
}

fun main(args: Array<String>) {
    LogManager.getLogManager().reset()
    com.pokegoapi.util.Log.setLevel(com.pokegoapi.util.Log.Level.NONE)
    val pokemonGoBotApplication: ConfigurableApplicationContext = SpringApplication.run(PokemonGoBotApplication::class.java, *args)
    ProgramController.addApplication(pokemonGoBotApplication)
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
        service.addBot(startBot(settings, http, {
            settings.writeProperty(filename, "token", it)
        }))
    }
}


fun startBot(settings: Settings, http: OkHttpClient, writeToken: (String) -> Unit = {}): Bot {

    var proxyHttp: OkHttpClient? = null

    if (!settings.proxyServer.equals("") && settings.proxyPort > 0) {
        Log.normal("Setting up proxy server for bot " + settings.name + ": " + settings.proxyServer + ":" + settings.proxyPort)

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

    val retryCount = 3
    val errorTimeout = 1000L

    var retries = retryCount

    var auth: CredentialProvider? = null
    do {
        try {
            if (proxyHttp == null)
                auth = getAuth(settings, http, writeToken)
            else
                auth = getAuth(settings, proxyHttp, writeToken)
        } catch (e: LoginFailedException) {
            throw IllegalStateException("Server refused your login credentials. Are they correct?")
        } catch (e: RemoteServerException) {
            Log.red("Server returned unexpected error: ${e.message}")
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
            if (proxyHttp == null)
                api = PokemonGo(auth, http, time)
            else
                api = PokemonGo(auth, proxyHttp, time)
        } catch (e: LoginFailedException) {
            throw IllegalStateException("Server refused your login credentials. Are they correct?")
        } catch (e: RemoteServerException) {
            Log.red("Server returned unexpected error: ${e.message}")
            if (retries-- > 0) {
                Log.normal("Retrying...")
                Thread.sleep(errorTimeout)
            }
        }
    } while (api == null && retries >= 0)

    if (api == null) {
        throw IllegalStateException("Failed to login. Stopping")
    }

    Log.normal("Logged in successfully")

    print("Getting profile data from pogo server")
    while (api.playerProfile == null) {
        print(".")
        Thread.sleep(1000)
    }
    println(".")
    Thread.sleep(1000)

    val stats = try {
        api.playerProfile.stats
    } catch (e: Exception) {
        null
    }

    if (stats == null) {
        Log.yellow("Accepting ToS")
        // apparently the account didn't except the ToS yet
        val getPlayerMessageBuilder = GetPlayerMessageOuterClass.GetPlayerMessage.newBuilder()

        val tosBuilder = MarkTutorialCompleteMessageOuterClass.MarkTutorialCompleteMessage.newBuilder()
                .addTutorialsCompleted(TutorialStateOuterClass.TutorialState.LEGAL_SCREEN)
                .setSendMarketingEmails(false)
                .setSendPushNotifications(false)

        val serverRequestsPlayer = ServerRequest(RequestTypeOuterClass.RequestType.GET_PLAYER, getPlayerMessageBuilder.build())
        val serverRequestsTutorial = ServerRequest(RequestTypeOuterClass.RequestType.MARK_TUTORIAL_COMPLETE, tosBuilder.build())

        api.requestHandler.sendServerRequests(serverRequestsPlayer, serverRequestsTutorial)
        Thread.sleep(1000)
        // set stats
        api.inventories.updateInventories(true)
    }

    val devices = arrayOf(
            Triple("iPad3,1", "iPad", "J1AP"),
            Triple("iPad3,2", "iPad", "J2AP"),
            Triple("iPad3,3", "iPad", "J2AAP"),
            Triple("iPad3,4", "iPad", "P101AP"),
            Triple("iPad3,5", "iPad", "P102AP"),
            Triple("iPad3,6", "iPad", "P103AP"),

            Triple("iPad4,1", "iPad", "J71AP"),
            Triple("iPad4,2", "iPad", "J72AP"),
            Triple("iPad4,3", "iPad", "J73AP"),
            Triple("iPad4,4", "iPad", "J85AP"),
            Triple("iPad4,5", "iPad", "J86AP"),
            Triple("iPad4,6", "iPad", "J87AP"),
            Triple("iPad4,7", "iPad", "J85mAP"),
            Triple("iPad4,8", "iPad", "J86mAP"),
            Triple("iPad4,9", "iPad", "J87mAP"),

            Triple("iPad5,1", "iPad", "J96AP"),
            Triple("iPad5,2", "iPad", "J97AP"),
            Triple("iPad5,3", "iPad", "J81AP"),
            Triple("iPad5,4", "iPad", "J82AP"),

            Triple("iPad6,7", "iPad", "J98aAP"),
            Triple("iPad6,8", "iPad", "J99aAP"),

            Triple("iPhone5,1", "iPhone", "N41AP"),
            Triple("iPhone5,2", "iPhone", "N42AP"),
            Triple("iPhone5,3", "iPhone", "N48AP"),
            Triple("iPhone5,4", "iPhone", "N49AP"),

            Triple("iPhone6,1", "iPhone", "N51AP"),
            Triple("iPhone6,2", "iPhone", "N53AP"),

            Triple("iPhone7,1", "iPhone", "N56AP"),
            Triple("iPhone7,2", "iPhone", "N61AP"),

            Triple("iPhone8,1", "iPhone", "N71AP")
    )

    val osVersions = arrayOf("8.1.1", "8.1.2", "8.1.3", "8.2", "8.3", "8.4", "8.4.1",
            "9.0", "9.0.1", "9.0.2", "9.1", "9.2", "9.2.1", "9.3", "9.3.1", "9.3.2", "9.3.3", "9.3.4")

    // try to create unique identifier
    val random = Random("PokemonGoBot-${settings.credentials.hashCode().toLong()}".hashCode().toLong())
    val deviceInfo = DeviceInfo()

    val deviceId = ByteArray(16)
    random.nextBytes(deviceId)

    deviceInfo.setDeviceId(deviceId.toHexString())
    deviceInfo.setDeviceBrand("Apple")

    val device = devices[random.nextInt(devices.size)]
    deviceInfo.setDeviceModel(device.second)
    deviceInfo.setDeviceModelBoot("${device.first}${0.toChar()}")
    deviceInfo.setHardwareManufacturer("Apple")
    deviceInfo.setHardwareModel("${device.third}${0.toChar()}")
    deviceInfo.setFirmwareBrand("iPhone OS")
    deviceInfo.setFirmwareType(osVersions[random.nextInt(osVersions.size)])

    api.setDeviceInfo(deviceInfo)

    val bot = Bot(api, settings)

    bot.start()

    return bot
}
