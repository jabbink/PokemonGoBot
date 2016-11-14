/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper

import ink.abb.pogo.scraper.services.BotService
import ink.abb.pogo.scraper.util.ApiAuthProvider
import okhttp3.OkHttpClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread


@SpringBootApplication
open class PokemonGoBotApplication {

    @Autowired
    lateinit var authProvider: ApiAuthProvider

    @Bean
    open fun httpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
        return builder.build()
    }

    @Bean
    open fun corsConfigurer(): WebMvcConfigurer {
        return object : WebMvcConfigurerAdapter() {
            override fun addCorsMappings(registry: CorsRegistry) {
                registry.addMapping("/**")
                        .allowedOrigins("*")
                        .allowedMethods("GET", "POST", "PUT", "DELETE")
            }
        }
    }

    @Bean
    open fun interceptorConfigurer(): WebMvcConfigurer {
        return object : WebMvcConfigurerAdapter() {
            override fun addInterceptors(registry: InterceptorRegistry) {
                registry.addInterceptor(authProvider)
                        .addPathPatterns("/api/bot/**")
                        .excludePathPatterns("/api/bot/*/auth")
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
            val JSONConfigBotNames = botRunService.getJSONConfigBotNames()

            if (JSONConfigBotNames.size < 1) {
                thread(name = "default") {
                    startDefaultBot(http, botRunService)
                }
            } else {
                JSONConfigBotNames.forEach {
                    thread(name = it) {
                        botRunService.submitBot(it)
                    }
                }
            }
        }
    }
}
