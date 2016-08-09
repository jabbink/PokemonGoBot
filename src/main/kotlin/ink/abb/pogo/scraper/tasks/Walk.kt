/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

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
import ink.abb.pogo.scraper.util.map.canLoot
import ink.abb.pogo.scraper.util.map.getCatchablePokemon
import java.util.concurrent.atomic.AtomicBoolean

class Walk(val sortedPokestops: List<Pokestop>, val lootTimeouts: Map<String, Long>) : Task {
    override fun run(bot: Bot, ctx: Context, settings: Settings) {
        if (!ctx.walking.compareAndSet(false, true)) {
            return
        }

        if (ctx.server.coordinatesToGoTo.size > 0) {
            val coordinates = ctx.server.coordinatesToGoTo.first()
            ctx.server.coordinatesToGoTo.removeAt(0)
            Log.normal("Walking to ${coordinates.latRadians()}, ${coordinates.lngRadians()}")

            walk(bot, ctx, settings, S2LatLng.fromDegrees(coordinates.latRadians(), coordinates.lngRadians()), settings.speed, true)
        } else {
            val nearestUnused: List<Pokestop> = sortedPokestops.filter {
                val canLoot = it.canLoot(ignoreDistance = true, lootTimeouts = lootTimeouts, api = ctx.api)
                if (settings.spawnRadius == -1) {
                    canLoot
                } else {
                    val distanceToStart = settings.startingLocation.getEarthDistance(S2LatLng.fromDegrees(it.latitude, it.longitude))
                    canLoot && distanceToStart < settings.spawnRadius
                }
            }

            if (nearestUnused.isNotEmpty()) {
                // Select random pokestop from the 5 nearest while taking the distance into account
                val chosenPokestop = selectRandom(nearestUnused.take(settings.randomNextPokestopSelection), ctx)

                ctx.server.sendPokestop(chosenPokestop)

                if (settings.displayPokestopName)
                    Log.normal("Walking to pokestop \"${chosenPokestop.details.name}\"")

                walk(bot, ctx, settings, S2LatLng.fromDegrees(chosenPokestop.latitude, chosenPokestop.longitude), settings.speed, false)
            }
        }
    }

    private fun walk(bot: Bot, ctx: Context, settings: Settings, end: S2LatLng, speed: Double, sendDone: Boolean) {
        if (settings.followStreets) {
            walkRoute(bot, ctx, settings, end, speed, sendDone)
        } else {
            walkDirectly(bot, ctx, settings, end, speed, sendDone)
        }
    }

    private fun walkDirectly(bot: Bot, ctx: Context, settings: Settings, end: S2LatLng, speed: Double, sendDone: Boolean) {
        walkPath(bot, ctx, settings, mutableListOf(end), speed, sendDone)
    }

    private fun walkRoute(bot: Bot, ctx: Context, settings: Settings, end: S2LatLng, speed: Double, sendDone: Boolean) {
        val coordinatesList = getRouteCoordinates(S2LatLng.fromDegrees(ctx.lat.get(), ctx.lng.get()), end)
        if (coordinatesList.size >= 0) {
            walkPath(bot, ctx, settings, coordinatesList, speed, sendDone)
        } else {
            walkDirectly(bot, ctx, settings, end, speed, sendDone)
        }
    }

    //all walk functions should call this one
    private fun walkPath(bot: Bot, ctx: Context, settings: Settings, path: MutableList<S2LatLng>, speed: Double, sendDone: Boolean) {
        if (speed.equals(0)) {
            return
        }
        if(path.isEmpty()) {
            return
        }

        val timeout = 200L

        var remainingSteps = 0.0
        var deltaLat = 0.0
        var deltaLng = 0.0

        val pauseWalk: AtomicBoolean = AtomicBoolean(false)
        var pauseCounter = 2

        bot.runLoop(timeout, "WalkingLoop") { cancel ->

            if(remainingSteps <= 0) {
                if (path.isEmpty()) {
                    Log.normal("Destination reached.")
                    if (sendDone) {
                        ctx.server.sendGotoDone()
                    }
                    ctx.walking.set(false)
                    cancel()
                } else {
                    //calculate delta lat/long for next step
                    val start = S2LatLng.fromDegrees(ctx.lat.get(), ctx.lng.get())
                    val nextPoint = path.first()
                    path.removeAt(0)
                    val diff = nextPoint.sub(start)
                    val distance = start.getEarthDistance(nextPoint)
                    val timeRequired = distance / speed
                    val stepsRequired = timeRequired / (timeout.toDouble() / 1000.toDouble())

                    deltaLat = diff.latDegrees() / stepsRequired
                    deltaLng = diff.lngDegrees() / stepsRequired

                    Log.normal("Walking to ${nextPoint.toStringDegrees()} in $stepsRequired steps.")
                    remainingSteps = stepsRequired
                }
            }

            if (pauseWalk.get()) {
                Thread.sleep(timeout * 2)
                pauseCounter--
                if (!(ctx.api.inventories.itemBag.hasPokeballs() && bot.api.map.getCatchablePokemon(ctx.blacklistedEncounters).size > 0 && settings.catchPokemon)) {
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
            if (remainingSteps.toInt().mod(20) == 0 && pauseCounter > 0)
                if (ctx.api.inventories.itemBag.hasPokeballs() && bot.api.map.getCatchablePokemon(ctx.blacklistedEncounters).size > 0 && settings.catchPokemon) {
                    // Stop walking
                    Log.normal("Pausing to catch pokemon...")
                    pauseCounter = 2
                    pauseWalk.set(true)
                    return@runLoop
                }

            val lat = ctx.lat.addAndGet(deltaLat)
            val lng = ctx.lng.addAndGet(deltaLng)

            ctx.server.setLocation(lat, lng)

            remainingSteps--
        }
    }

    // TODO not used anymore? remove?
    private fun walkAndComeBack(bot: Bot, ctx: Context, settings: Settings, end: S2LatLng, speed: Double, sendDone: Boolean) {
        val start = S2LatLng.fromDegrees(ctx.lat.get(), ctx.lng.get())
        walkPath(bot, ctx, settings, mutableListOf(end, start), speed, sendDone)
    }

    private fun selectRandom(pokestops: List<Pokestop>, ctx: Context): Pokestop {
        // Select random pokestop while taking the distance into account
        // E.g. pokestop is closer to the user -> higher probabilty to be chosen

        if (pokestops.size < 2)
            return pokestops.first()

        val currentPosition = S2LatLng.fromDegrees(ctx.lat.get(), ctx.lng.get())

        val distances = pokestops.map {
            val end = S2LatLng.fromDegrees(it.latitude, it.longitude)
            currentPosition.getEarthDistance(end)
        }
        val totalDistance = distances.sum()

        // Get random value between 0 and 1
        val random = Math.random()
        var cumulativeProbability = 0.0

        for ((index, pokestop) in pokestops.withIndex()) {
            // Calculate probabilty proportional to the closeness
            val probability = (1 - distances[index] / totalDistance) / (pokestops.size - 1)

            cumulativeProbability += probability
            if (random <= cumulativeProbability) {
                return pokestop
            }
        }

        // should not happen
        return pokestops.first()
    }
}