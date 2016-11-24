/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper.services

import ink.abb.pogo.scraper.Bot
import ink.abb.pogo.scraper.Context
import ink.abb.pogo.scraper.Settings
import ink.abb.pogo.scraper.startBot
import ink.abb.pogo.scraper.util.Log
import ink.abb.pogo.scraper.util.credentials.GoogleAutoCredentials
import ink.abb.pogo.scraper.util.io.SettingsJSONWriter
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
    val settingsJSONWriter = SettingsJSONWriter()

    fun submitBot(name: String): Settings {
        val settings = settingsJSONWriter.load(name)
        addBot(startBot(settings, http))

        settingsJSONWriter.save(settings) // Is this needed after starting?

        return settings
    }

    @Synchronized
    fun addBot(bot: Bot) {
        bots.add(bot)
    }

    @Synchronized
    fun removeBot(bot: Bot) {
        bots.remove(bot)
    }

    fun getJSONConfigBotNames(): List<String> {
        return settingsJSONWriter.getJSONConfigBotNames()
    }

    fun getBotContext(name: String): Context {
        val bot = bots.find { it.settings.name == name }

        bot ?: throw IllegalArgumentException("Bot $name doesn't exists !")

        return bot.ctx
    }

    @Synchronized
    fun getAllBotSettings(): List<Settings> {
        return bots.map { it.settings.copy(credentials = GoogleAutoCredentials(), restApiPassword = "") }
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
