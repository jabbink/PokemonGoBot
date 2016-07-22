/**
 * Pokemon Go Bot  Copyright (C) 2016  Jasper Abbink
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper

import POGOProtos.Networking.Envelopes.RequestEnvelopeOuterClass
import com.google.common.util.concurrent.AtomicDouble
import com.pokegoapi.api.PokemonGo
import com.pokegoapi.api.player.PlayerProfile
import com.pokegoapi.auth.GoogleLogin
import com.pokegoapi.auth.PtcLogin
import ink.abb.pogo.scraper.tasks.*
import okhttp3.OkHttpClient
import java.io.FileInputStream
import java.net.InetSocketAddress
import java.net.Proxy
import java.security.cert.X509Certificate
import java.util.*
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.security.cert.CertificateException
import kotlin.concurrent.fixedRateTimer
import kotlin.concurrent.thread

/**
 * Allow all certificate to debug with https://github.com/bettse/mitmdump_decoder
 */
fun allowProxy(builder: OkHttpClient.Builder) {
    builder.proxy(Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved("localhost", 8888)))
    val trustAllCerts = arrayOf<TrustManager>(object : javax.net.ssl.X509TrustManager {
        override fun getAcceptedIssuers(): Array<out X509Certificate> {
            return emptyArray()
        }

        @Throws(CertificateException::class)
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
        }

        @Throws(CertificateException::class)
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
        }
    })

    // Install the all-trusting trust manager
    val sslContext = SSLContext.getInstance("SSL")
    sslContext.init(null, trustAllCerts, java.security.SecureRandom())
    // Create an ssl socket factory with our all-trusting manager
    val sslSocketFactory = sslContext.socketFactory

    builder.sslSocketFactory(sslSocketFactory)
    builder.hostnameVerifier { hostname, session -> true }
}

fun main(args: Array<String>) {
    val builder = OkHttpClient.Builder()
    // allowProxy(builder)
    builder.connectTimeout(60, TimeUnit.SECONDS)
    builder.readTimeout(60, TimeUnit.SECONDS)
    builder.writeTimeout(60, TimeUnit.SECONDS)
    val http = builder.build()

    val properties = Properties()
    FileInputStream("config.properties").use {
        properties.load(it)
    }

    val settings = Settings(properties)

    val username = settings.username
    val password = settings.password

    println("Logging in to game server...")
    val auth: RequestEnvelopeOuterClass.RequestEnvelope.AuthInfo

    if (username.contains('@')) {
        auth = GoogleLogin(http).login(username, password)
    } else {
        auth = PtcLogin(http).login(username, password)
    }
    println("Logged in as $username")
    val api = PokemonGo(auth, http)

    print("Getting profile data from pogo server")
    while (api.playerProfile == null) {
        print(".")
        Thread.sleep(1000)
    }
    println(".")

    Bot(api, settings).run()
}
