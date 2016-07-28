package ink.abb.pogo.scraper.tasks

import com.pokegoapi.api.map.fort.Pokestop
import com.pokegoapi.google.common.geometry.S2LatLng
import ink.abb.pogo.scraper.Bot
import ink.abb.pogo.scraper.Context
import ink.abb.pogo.scraper.Settings
import ink.abb.pogo.scraper.Task
import ink.abb.pogo.scraper.util.Log

/**
 * Created by Home on 27.07.2016.
 */
class WalkToStartPokeStop(val startPokeStop: Pokestop) : Task {
    override fun run(bot: Bot, ctx: Context, settings: Settings) {
        ctx.walking.set(true)
        val end = S2LatLng.fromDegrees(startPokeStop.latitude, startPokeStop.longitude);
        val start = S2LatLng.fromDegrees(ctx.lat.get(), ctx.lng.get())
        val diff = end.sub(start)
        val distance = start.getEarthDistance(end)
        val timeout = 200L
        // prevent division by 0
        if (settings.speed.equals(0)) {
            ctx.walking.set(false)
            return
        }
        val timeRequired = distance / settings.speed
        val stepsRequired = timeRequired / (timeout.toDouble() / 1000.toDouble())
        // prevent division by 0
        if (stepsRequired.equals(0)) {
            ctx.walking.set(false)
            return
        }
        val deltaLat = diff.latDegrees() / stepsRequired
        val deltaLng = diff.lngDegrees() / stepsRequired

        Log.cyan("Walking to starting Pokestop ${startPokeStop.details.name} in ${stepsRequired.toInt()} steps.")
        var remainingSteps = stepsRequired

        bot.runLoop(timeout, "WalkingLoop") { cancel ->
            ctx.lat.addAndGet(deltaLat)
            ctx.lng.addAndGet(deltaLng)
            ctx.server.setLocation(ctx.lat.get(), ctx.lng.get())
            remainingSteps--
            if(remainingSteps.toInt().mod(20) == 0) Log.cyan("Starting Pokestop reached in ${remainingSteps.toInt()} steps.")
            if (remainingSteps <= 0) {
                Log.normal("Destination reached.")
                ctx.walking.set(false)
                bot.prepareWalkBack.set(false)
                bot.walkBackLock.set(false)
                ctx.server.sendGotoDone()
                cancel()
            }
        }

    }
}
