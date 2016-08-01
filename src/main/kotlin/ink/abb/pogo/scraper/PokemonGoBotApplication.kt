package ink.abb.pogo.scraper

import ink.abb.pogo.scraper.services.BotService
import okhttp3.OkHttpClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * @author Andrew Potter (ddcapotter)
 */
@SpringBootApplication
open class PokemonGoBotApplication {

    @Bean
    open fun httpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
        builder.connectTimeout(60, TimeUnit.SECONDS)
        builder.readTimeout(60, TimeUnit.SECONDS)
        builder.writeTimeout(60, TimeUnit.SECONDS)
        return builder.build()
    }

    @Component
    open class BotRunner : CommandLineRunner {
        @Autowired
        lateinit var http: OkHttpClient

        @Autowired
        lateinit var botRunService: BotService

        override fun run(vararg args: String?) {
            val names = botRunService.getSaveNames()
            if(names.size < 1) {
                thread(name = "default") {
                    startDefaultBot(http, botRunService)
                }
            } else {
                names.forEach {
                    thread(name = it) {
                        botRunService.submitBot(botRunService.load(it))
                    }
                }
            }
        }
    }
}
