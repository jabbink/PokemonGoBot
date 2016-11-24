/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper.tasks

import com.google.common.geometry.S2LatLng
import ink.abb.pogo.api.cache.Pokestop
import ink.abb.pogo.scraper.Bot
import ink.abb.pogo.scraper.Context
import ink.abb.pogo.scraper.Settings
import ink.abb.pogo.scraper.Task
import ink.abb.pogo.scraper.util.Log
import ink.abb.pogo.scraper.util.directions.getRouteCoordinates
import ink.abb.pogo.scraper.util.pokemon.inRange
import java.util.concurrent.atomic.AtomicBoolean

class WalkToStartPokestop(val startPokeStop: Pokestop) : Task {
    override fun run(bot: Bot, ctx: Context, settings: Settings) {
        if (settings.followStreets.isNotEmpty()) walkRoute(bot, ctx, settings)
        else walk(bot, ctx, settings)

    }

    fun walk(bot: Bot, ctx: Context, settings: Settings) {
        ctx.walking.set(true)
        val end = S2LatLng.fromDegrees(startPokeStop.fortData.latitude, startPokeStop.fortData.longitude)
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

        Log.cyan("Walking to starting Pokestop ${startPokeStop.name} in ${stepsRequired.toInt()} steps.")
        var remainingSteps = stepsRequired
        val pauseWalk: AtomicBoolean = AtomicBoolean(false)
        var pauseCounter = 2
        bot.runLoop(timeout, "WalkingLoop") { cancel ->
            if (pauseWalk.get()) {
                Thread.sleep(timeout * 2)
                pauseCounter--
                if (!(ctx.api.inventory.hasPokeballs && bot.api.map.getPokemon(bot.api.latitude, bot.api.longitude, 3).filter {
                    !ctx.blacklistedEncounters.contains(it.encounterId) && it.inRange
                }.size > 0 && settings.catchPokemon)) {
                    // api break free
                    pauseWalk.set(false)
                    pauseCounter = 0
                }
                //  fixed tries break free
                if (pauseCounter > 0) {
                    return@runLoop
                } else {
                    pauseWalk.set(false)
                }
            }
            // don't run away when there are still Pokemon around
            if (remainingSteps.toInt().mod(20) == 0 && pauseCounter > 0) {
                if (ctx.api.inventory.hasPokeballs && bot.api.map.getPokemon(bot.api.latitude, bot.api.longitude, 3).filter {
                    !ctx.blacklistedEncounters.contains(it.encounterId) && it.inRange
                }.size > 0 && settings.catchPokemon) {
                    // Stop walking
                    Log.normal("Pausing to catch pokemon...")
                    pauseCounter = 2
                    pauseWalk.set(true)
                    return@runLoop
                }
            }

            pauseCounter = 2
            val lat = ctx.lat.addAndGet(deltaLat)
            val lng = ctx.lng.addAndGet(deltaLng)

            ctx.server.setLocation(lat, lng)

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
        val coordinatesList = getRouteCoordinates(ctx.lat.get(), ctx.lng.get(), startPokeStop.fortData.latitude, startPokeStop.fortData.longitude, settings, ctx.geoApiContext!!)
        if (coordinatesList.size <= 0) {
            walk(bot, ctx, settings)
        } else {
            val pauseWalk: AtomicBoolean = AtomicBoolean(false)
            var pauseCounter = 2
            bot.runLoop(timeout, "WalkingLoop") { cancel ->
                if (pauseWalk.get()) {
                    Thread.sleep(timeout * 2)
                    pauseCounter--
                    if (!(ctx.api.inventory.hasPokeballs && bot.api.map.getPokemon(bot.api.latitude, bot.api.longitude, 3).filter {
                        !ctx.blacklistedEncounters.contains(it.encounterId) && it.inRange
                    }.size > 0 && settings.catchPokemon)) {
                        // api break free
                        pauseWalk.set(false)
                        pauseCounter = 0
                    }
                    //  fixed tries break free
                    if (pauseCounter > 0) {
                        return@runLoop
                    } else {
                        pauseWalk.set(false)
                    }
                }
                // don't run away when there are still Pokemon around
                if (pauseCounter > 0 && ctx.api.inventory.hasPokeballs && bot.api.map.getPokemon(bot.api.latitude, bot.api.longitude, 3).filter {
                    !ctx.blacklistedEncounters.contains(it.encounterId) && it.inRange
                }.size > 0 && settings.catchPokemon) {
                    // Stop walking
                    Log.normal("Pausing to catch pokemon...")
                    pauseCounter = 2
                    pauseWalk.set(true)
                    return@runLoop
                }
                pauseCounter = 2
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
