package ink.abb.pogo.scraper

import POGOProtos.Networking.Envelopes.RequestEnvelopeOuterClass.RequestEnvelope.AuthInfo
import com.pokegoapi.api.PokemonGo
import com.pokegoapi.auth.GoogleLogin
import com.pokegoapi.auth.PtcLogin
import okhttp3.OkHttpClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.FileInputStream
import java.net.InetSocketAddress
import java.net.Proxy
import java.security.cert.X509Certificate
import java.util.*
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.security.cert.CertificateException

/**
 * @author Andrew Potter (ddcapotter)
 */
@Configuration
open class BotConfiguration {

    @Bean
    open fun settings(): Settings {
        val properties = Properties()
        FileInputStream("config.properties").use {
            properties.load(it)
        }

        return Settings(properties)
    }

    @Bean
    open fun httpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
        // allowProxy(builder)
        builder.connectTimeout(60, TimeUnit.SECONDS)
        builder.readTimeout(60, TimeUnit.SECONDS)
        builder.writeTimeout(60, TimeUnit.SECONDS)
        return builder.build()
    }

    @Bean
    open fun auth(http: OkHttpClient, settings: Settings): AuthInfo {
        val username = settings.username
        val password = settings.password
        val token = settings.token

        println("Logging in to game server...")
        val auth = if (token.isBlank()) {
            if (username.contains('@')) {
                GoogleLogin(http).login(username, password)
            } else {
                PtcLogin(http).login(username, password)
            }
        } else {
            if (token.contains("pokemon.com")) {
                PtcLogin(http).login(token)
            } else {
                GoogleLogin(http).login(token)
            }
        }

        println("Logged in as $username with token ${auth.token.contents}")

        if (token.isBlank()) {
            println("Set this token in your config to log in directly")
        }

        return auth
    }

    @Bean
    open fun api(http: OkHttpClient, auth: AuthInfo): PokemonGo {
        return PokemonGo(auth, http)
    }

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
}