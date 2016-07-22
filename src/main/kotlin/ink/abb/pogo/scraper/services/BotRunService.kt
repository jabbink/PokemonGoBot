package ink.abb.pogo.scraper.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.pokegoapi.api.PokemonGo
import com.pokegoapi.auth.GoogleLogin
import com.pokegoapi.auth.PtcLogin
import ink.abb.pogo.scraper.Bot
import ink.abb.pogo.scraper.Settings
import okhttp3.OkHttpClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File
import kotlin.concurrent.thread

/**
 * @author Andrew Potter (ddcapotter)
 */
@Service
class BotRunService {

    @Autowired
    lateinit var http: OkHttpClient

    val bots: MutableList<Pair<Bot, Thread>> = mutableListOf()
    val mapper: ObjectMapper = ObjectMapper().registerKotlinModule()
    val root = File("./bot-settings").absoluteFile!!

    fun submitBot(settings: Settings) {
        val api = login(settings)
        val bot = Bot(api, settings)
        bot.init()

        bots.add(Pair(bot, startBot(bot)))
    }

    fun startBot(bot: Bot): Thread {
        return thread {
            bot.run()
        }
    }

    private fun login(settings: Settings): PokemonGo {
        println("Logging in to game server...")
        val auth = if (settings.credentials is Settings.GoogleCredentials) {
            val auth = if (settings.credentials.token.isBlank()) {
                GoogleLogin(http).login("", "")
            } else {
                GoogleLogin(http).login(settings.credentials.token)
            }
            settings.credentials.token = auth.token.contents

            auth
        } else if (settings.credentials is Settings.PokemonTrainersClubCredentials) {
            val auth = if (settings.credentials.token.isBlank()) {
                PtcLogin(http).login(settings.credentials.username, settings.credentials.password)
            } else {
                PtcLogin(http).login(settings.credentials.token)
            }
            settings.credentials.token = auth.token.contents

            auth
        } else {
            throw IllegalStateException("Unable to log in with credentials, unknown type!")
        }

        val api = PokemonGo(auth, http)
        if(api.playerProfile == null) {
            throw IllegalStateException("Login failed, please check your credentials or delete your token.")
        }

        println("Successfully logged in.")
        save(settings)

        return api
    }

    private fun save(settings: Settings) {
        root.mkdirs()

        File(root, "${settings.name}-bot.json").bufferedWriter().use {
            it.write(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(settings))
        }
    }

    fun load(name: String): Settings {
        val save = File(root, "$name-bot.json")
        if(!save.isFile) {
            throw IllegalArgumentException("No save file found for name: $name")
        }

        return mapper.readValue(save, Settings::class.java)
    }
}