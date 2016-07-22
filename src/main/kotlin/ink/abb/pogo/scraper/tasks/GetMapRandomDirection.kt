package ink.abb.pogo.scraper.tasks

import ink.abb.pogo.scraper.Bot
import ink.abb.pogo.scraper.Context
import ink.abb.pogo.scraper.Settings
import ink.abb.pogo.scraper.Task

/**
 * @author Andrew Potter (apottere)
 */
class GetMapRandomDirection : Task {
    override fun run(bot: Bot, ctx: Context, settings: Settings) {
        // query a small area to keep alive
        val lat = ctx.lat.get() + randomLatLng()
        val lng = ctx.lng.get() + randomLatLng()

        if(settings.shouldDisplayKeepalive) println("Getting map of ($lat, $lng)")
        ctx.api.setLocation(lat, lng, 0.0)
    }

    fun randomLatLng(): Double {
        return Math.random() * 0.0001 - 0.00005
    }
}
