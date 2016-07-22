/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper

import com.pokegoapi.api.PokemonGo
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import javax.annotation.PostConstruct

/**
 * Allow all certificate to debug with https://github.com/bettse/mitmdump_decoder
 */
@SpringBootApplication
open class Main {

    @Autowired
    lateinit var settings: Settings

    @Autowired
    lateinit var api: PokemonGo

    @PostConstruct
    fun createBot() {
        print("Getting profile data from pogo server")
        while (api.playerProfile == null) {
            print(".")
            Thread.sleep(1000)
        }
        println(".")

        Bot(api, settings).run()
    }
}

fun main(args: Array<String>) {
    SpringApplication.run(Main::class.java, *args)
}
