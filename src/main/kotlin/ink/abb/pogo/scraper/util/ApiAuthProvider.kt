package ink.abb.pogo.scraper.util

import ink.abb.pogo.scraper.services.BotService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter
import java.util.regex.Pattern
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
open class ApiAuthProvider : HandlerInterceptorAdapter() {

    @Autowired
    lateinit var service: BotService

    @Throws(Exception::class)
    override fun preHandle(request: HttpServletRequest,
                  response: HttpServletResponse, handler: Any): Boolean {

        val pattern = Pattern.compile("\\/api/bot/([A-Za-z0-9\\-_]*)")
        val matcher = pattern.matcher(request.requestURI)
        if(matcher.find()) {
            val token = service.getBotContext(matcher.group(1)).apiToken

            // If the token is invalid or isn't in the request, nothing will be done
            return request.getHeader("X-PGB-ACCESS-TOKEN").equals(token)
        }

        return false
    }
}