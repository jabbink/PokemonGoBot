/*
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import ink.abb.pogo.scraper.Bot
import ink.abb.pogo.scraper.Settings
import ink.abb.pogo.scraper.util.credentials.*
import ink.abb.pogo.scraper.startBot
import okhttp3.OkHttpClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File
import java.util.concurrent.CountDownLatch
import javax.annotation.PreDestroy
import kotlin.concurrent.thread

@Service
class BotService {

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

    @Synchronized
    fun removeBot(bot: Bot) {
        bots.remove(bot)
    }

    private fun save(settings: Settings) {
        File(root, "${settings.name}.json").bufferedWriter().use {
            it.write(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(settings))
        }
    }

    fun load(name: String): Settings {
        val save = File(root, "$name.json")
        if (!save.isFile) {
            throw IllegalArgumentException("No save file found for name: $name")
        }

        return mapper.readValue(save, Settings::class.java).withName(name)
    }

    fun getSaveNames(): List<String> {
        return root.list().filter { it.endsWith(".json") }.map { it.replace(Regex("\\.json$"), "") }
    }

    @Synchronized
    fun getAllBotSettings(): List<Settings> {
        return bots.map {it.settings.copy(credentials = GoogleAutoCredentials())}
    }

    @Synchronized
    fun doWithBot(name: String, action: (bot: Bot) -> Unit): Boolean {
        val bot = bots.find { it.settings.name == name } ?: return false

        action(bot)
        return true
    }

    @PreDestroy
    @Synchronized
    fun stopAllBots() {
        val latch = CountDownLatch(bots.size)
        bots.forEach {
            thread {
                it.stop()
                latch.countDown()
            }
        }

        latch.await()
    }
}
