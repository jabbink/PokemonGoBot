package ink.abb.pogo.scraper

import okhttp3.OkHttpClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.net.InetSocketAddress
import java.net.Proxy
import java.security.cert.X509Certificate
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
    open fun httpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
        // allowProxy(builder)
        builder.connectTimeout(60, TimeUnit.SECONDS)
        builder.readTimeout(60, TimeUnit.SECONDS)
        builder.writeTimeout(60, TimeUnit.SECONDS)
        return builder.build()
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