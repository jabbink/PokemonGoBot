package ink.abb.pogo.scraper.controllers

import ink.abb.pogo.scraper.Settings
import ink.abb.pogo.scraper.services.BotService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * @author Andrew Potter
 */
@RestController
@RequestMapping("/api")
class BotController {

    @Autowired
    lateinit var service: BotService

    @RequestMapping("/bots")
    fun bots(): List<Settings> {
        return service.getAllBotSettings()
    }
}