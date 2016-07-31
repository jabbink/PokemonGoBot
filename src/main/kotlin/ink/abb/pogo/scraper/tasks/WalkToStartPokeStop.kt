package ink.abb.pogo.scraper.tasks

import com.pokegoapi.api.map.fort.Pokestop
import com.pokegoapi.google.common.geometry.S2LatLng
import ink.abb.pogo.scraper.Bot
import ink.abb.pogo.scraper.Context
import ink.abb.pogo.scraper.Settings
import ink.abb.pogo.scraper.Task
import ink.abb.pogo.scraper.util.Log
import ink.abb.pogo.scraper.util.directions.getRouteCoordinates
import ink.abb.pogo.scraper.util.inventory.hasPokeballs
import ink.abb.pogo.scraper.util.map.getCatchablePokemon

/**
 * Created by Home on 27.07.2016.
 */
class WalkToStartPokeStop(val startPokeStop: Pokestop) : Task {
    override fun run(bot: Bot, ctx: Context, settings: Settings) {
        if (settings.shouldFollowStreets) walkRoute(bot, ctx, settings)
        else walk(bot, ctx, settings)

    }

    fun walk(bot: Bot, ctx: Context, settings: Settings) {
        ctx.walking.set(true)
        val end = S2LatLng.fromDegrees(startPokeStop.latitude, startPokeStop.longitude)
        val start = S2LatLng.fromDegrees(ctx.lat.get(), ctx.lng.get())
        val diff = end.sub(start)
        val distance = start.getEarthDistance(end)
        val timeout = 200L
        // prevent division by 0
        if (settings.speed.equals(0)) {
            notifyWalkDone(ctx, bot)
            return
        }
        val timeRequired = distance / settings.speed
        val stepsRequired = timeRequired / (timeout.toDouble() / 1000.toDouble())
        // prevent division by 0
        if (stepsRequired.equals(0)) {
            notifyWalkDone(ctx, bot)
            return
        }
        val deltaLat = diff.latDegrees() / stepsRequired
        val deltaLng = diff.lngDegrees() / stepsRequired

        Log.cyan("Walking to starting Pokestop ${startPokeStop.details.name} in ${stepsRequired.toInt()} steps.")
        var remainingSteps = stepsRequired

        bot.runLoop(timeout, "WalkingLoop") { cancel ->
            // don't run away when there are still Pokemon around
            if (remainingSteps.toInt().mod(20) == 0)
                if (ctx.api.inventories.itemBag.hasPokeballs() && bot.api.map.getCatchablePokemon(ctx.blacklistedEncounters).size > 0 && settings.shouldCatchPokemons) {
                    // Stop walking
                    Log.normal("Pausing to catch pokemon...")
                    // Try to catch once, then wait for next walk loop
                    bot.task(CatchOneNearbyPokemon())

                    return@runLoop
                }

            ctx.lat.addAndGet(deltaLat)
            ctx.lng.addAndGet(deltaLng)
            ctx.server.setLocation(ctx.lat.get(), ctx.lng.get())
            remainingSteps--
            if (remainingSteps.toInt().mod(20) == 0) Log.cyan("Starting Pokestop reached in ${remainingSteps.toInt()} steps.")
            if (remainingSteps <= 0) {
                Log.normal("Destination reached.")
                notifyWalkDone(ctx, bot)
                cancel()
            }
        }
    }

    fun walkRoute(bot: Bot, ctx: Context, settings: Settings) {
        ctx.walking.set(true)
        if (settings.speed.equals(0)) {
            notifyWalkDone(ctx, bot)
            return
        }
        val timeout = 200L
        val coordinatesList = getRouteCoordinates(S2LatLng.fromDegrees(ctx.lat.get(), ctx.lng.get()), S2LatLng.fromDegrees(startPokeStop.latitude, startPokeStop.longitude))
        if (coordinatesList.size <= 0) {
            walk(bot, ctx, settings)
        } else {
            bot.runLoop(timeout, "WalkingLoop") { cancel ->
                // don't run away when there are still Pokemon around
                if (ctx.api.inventories.itemBag.hasPokeballs() && bot.api.map.getCatchablePokemon(ctx.blacklistedEncounters).size > 0 && settings.shouldCatchPokemons) {
                    // Stop walking
                    Log.normal("Pausing to catch pokemon...")
                    // Try to catch once, then wait for next walk loop
                    bot.task(CatchOneNearbyPokemon())
                    return@runLoop
                }
                val start = S2LatLng.fromDegrees(ctx.lat.get(), ctx.lng.get())
                val step = coordinatesList.first()
                coordinatesList.removeAt(0)
                val diff = step.sub(start)
                val distance = start.getEarthDistance(step)
                val timeRequired = distance / settings.speed
                val stepsRequired = timeRequired / (timeout.toDouble() / 1000.toDouble())
                if (stepsRequired.equals(0)) {
                    notifyWalkDone(ctx, bot)
                    cancel()
                }
                val deltaLat = diff.latDegrees() / stepsRequired
                val deltaLng = diff.lngDegrees() / stepsRequired
                var remainingSteps = stepsRequired
                while (remainingSteps > 0) {
                    ctx.lat.addAndGet(deltaLat)
                    ctx.lng.addAndGet(deltaLng)
                    ctx.server.setLocation(ctx.lat.get(), ctx.lng.get())
                    remainingSteps--
                    Thread.sleep(timeout)
                }

                if (coordinatesList.size <= 0) {
                    notifyWalkDone(ctx, bot)
                    cancel()
                }
            }
        }
    }

    fun notifyWalkDone(ctx: Context, bot: Bot) {
        Log.normal("Destination reached.")
        ctx.walking.set(false)
        bot.prepareWalkBack.set(false)
        bot.walkBackLock.set(false)
        ctx.server.sendGotoDone()
    }
}
