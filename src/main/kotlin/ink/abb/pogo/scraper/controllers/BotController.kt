package ink.abb.pogo.scraper.controllers

import ink.abb.pogo.scraper.Settings
import ink.abb.pogo.scraper.services.BotService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PathVariable
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

    @RequestMapping("/bot/{name}/load")
    fun loadBot(@PathVariable name: String) {
        service.submitBot(service.load(name))
    }

    @RequestMapping("/bot/{name}/unload")
    fun unloadBot(@PathVariable name: String): Boolean {
        return service.doWithBot(name) {
            it.stop()
            service.removeBot(it)
        }
    }

    @RequestMapping("/bot/{name}/reload")
    fun reloadBot(@PathVariable name: String): Boolean {
        if(!unloadBot(name)) return false
        loadBot(name)
        return true
    }

    @RequestMapping("/bot/{name}/start")
    fun startBot(@PathVariable name: String): Boolean {
        return service.doWithBot(name) { it.start() }
    }

    @RequestMapping("/bot/{name}/stop")
    fun stopBot(@PathVariable name: String): Boolean {
        return service.doWithBot(name) { it.stop() }
    }
}
