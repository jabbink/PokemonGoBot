package ink.abb.pogo.scraper

import ink.abb.pogo.scraper.services.BotService
import okhttp3.OkHttpClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * @author Andrew Potter (apottere)
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

    @Bean
    open fun corsConfigurer(): WebMvcConfigurer {
        return object : WebMvcConfigurerAdapter() {
            override fun addCorsMappings(registry: CorsRegistry) {
                registry.addMapping("/**").allowedOrigins("*")
            }
        }
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
