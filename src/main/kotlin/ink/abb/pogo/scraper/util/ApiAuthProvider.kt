/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper.util

import ink.abb.pogo.scraper.services.BotService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter
import java.math.BigInteger
import java.security.SecureRandom
import java.util.regex.Pattern
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
open class ApiAuthProvider : HandlerInterceptorAdapter() {

    @Autowired
    lateinit var service: BotService

    val random: SecureRandom = SecureRandom()

    @Throws(Exception::class)
    override fun preHandle(request: HttpServletRequest,
                           response: HttpServletResponse, handler: Any): Boolean {

        if (request.method.equals("OPTIONS"))
            return true // Allow preflight calls

        val pattern = Pattern.compile("\\/api/bot/([A-Za-z0-9\\-_]*)")
        val matcher = pattern.matcher(request.requestURI)
        if (matcher.find()) {
            val token = service.getBotContext(matcher.group(1)).restApiToken

            // If the token is invalid or isn't in the request, nothing will be done
            return request.getHeader("X-PGB-ACCESS-TOKEN")!!.equals(token)
        }

        return false
    }

    fun generateRandomString(): String {
        return BigInteger(130, random).toString(32)
    }

    fun generateAuthToken(botName: String) {
        val token: String = this.generateRandomString()
        service.getBotContext(botName).restApiToken = token

        Log.cyan("REST API token for bot $botName : $token has been generated")
    }

    fun generateRestPassword(botName: String) {
        val password: String = this.generateRandomString()
        service.getBotContext(botName).restApiPassword = password
        service.doWithBot(botName) {
            it.settings.restApiPassword = password
        }

        Log.red("Generated restApiPassword: $password")
    }
}
