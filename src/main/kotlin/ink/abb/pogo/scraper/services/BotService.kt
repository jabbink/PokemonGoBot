package ink.abb.pogo.scraper.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import ink.abb.pogo.scraper.Bot
import ink.abb.pogo.scraper.Settings
import ink.abb.pogo.scraper.startBot
import okhttp3.OkHttpClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File

/**
 * @author Andrew Potter (apottere)
 */
@Service
class BotRunService {

    @Autowired
    lateinit var http: OkHttpClient

    private val bots: MutableList<Bot> = mutableListOf()
    val mapper: ObjectMapper = ObjectMapper().registerKotlinModule()
    val root = File("./bot-settings").absoluteFile!!

    init {
        root.mkdirs()
    }

    fun submitBot(settings: Settings) {
        addBot(startBot(settings, http))
        save(settings)
    }

    @Synchronized
    fun addBot(bot: Bot) {
        bots.add(bot)
    }

    private fun save(settings: Settings) {
        File(root, "${settings.name}.json").bufferedWriter().use {
            it.write(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(settings))
        }
    }

    fun load(name: String): Settings {
        val save = File(root, "$name.json")
        if(!save.isFile) {
            throw IllegalArgumentException("No save file found for name: $name")
        }

        return mapper.readValue(save, Settings::class.java)
    }

    fun getSaveNames(): List<String> {
        return root.list().filter { it.endsWith(".json") }.map { it.replace(Regex("\\.json$"), "") }
    }
}
