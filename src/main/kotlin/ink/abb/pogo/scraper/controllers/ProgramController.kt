/*
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper.controllers

import org.springframework.boot.SpringApplication
import org.springframework.context.ConfigurableApplicationContext

class ProgramController(){
    companion object {
        var applicationList = arrayListOf<ConfigurableApplicationContext>()

        fun addApplication (configurableApplicationContext: ConfigurableApplicationContext){
            applicationList.add(configurableApplicationContext)
        }
        fun stopAllApplications (){
            applicationList.forEach {
                it.close()
            }
        }
    }
}