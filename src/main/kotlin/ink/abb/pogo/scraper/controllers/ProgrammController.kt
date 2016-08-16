package ink.abb.pogo.scraper.controllers

import org.springframework.boot.SpringApplication
import org.springframework.context.ConfigurableApplicationContext

/**
 * Created by alex_b on 16.08.16.
 */
class ProgrammController(){
    companion object {
        var applicationList = arrayListOf<ConfigurableApplicationContext>()

        fun addApplication (configurableApplicationContext: ConfigurableApplicationContext){
            applicationList.add(configurableApplicationContext)
        }
        fun stopAllAplications (){
            applicationList.forEach {
                it.close()
            }
        }
    }
}