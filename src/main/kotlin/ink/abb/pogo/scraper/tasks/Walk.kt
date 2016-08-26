/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper.tasks

import com.google.common.geometry.S2LatLng
import com.google.common.util.concurrent.AtomicDouble
import ink.abb.pogo.api.cache.Pokestop
import ink.abb.pogo.scraper.Bot
import ink.abb.pogo.scraper.Context
import ink.abb.pogo.scraper.Settings
import ink.abb.pogo.scraper.Task
import ink.abb.pogo.scraper.util.Log
import ink.abb.pogo.scraper.util.directions.getRouteCoordinates
import ink.abb.pogo.scraper.util.map.canLoot
import ink.abb.pogo.scraper.util.pokemon.inRange
import java.text.DecimalFormat
import java.util.concurrent.atomic.AtomicBoolean

class Walk(val sortedPokestops: List<Pokestop>, val lootTimeouts: Map<String, Long>) : Task {
    override fun run(bot: Bot, ctx: Context, settings: Settings) {
        if (!ctx.walking.compareAndSet(false, true)) {
            return
        }

        if (ctx.server.coordinatesToGoTo.size > 0) {
            val coordinates = ctx.server.coordinatesToGoTo.first()
            ctx.server.coordinatesToGoTo.removeAt(0)
            Log.normal("Walking to ${coordinates.latDegrees()}, ${coordinates.lngDegrees()}")

            walk(bot, ctx, settings, coordinates, settings.speed, true, null)
        } else {
            val nearestUnused: List<Pokestop> = sortedPokestops.filter {
                val canLoot = it.canLoot(ignoreDistance = true, lootTimeouts = lootTimeouts)
                if (settings.spawnRadius == -1) {
                    canLoot
                } else {
                    val distanceToStart = settings.startingLocation.getEarthDistance(S2LatLng.fromDegrees(it.fortData.latitude, it.fortData.longitude))
                    canLoot && distanceToStart < settings.spawnRadius
                }
            }

            if (nearestUnused.isNotEmpty()) {
                // Select random pokestop from the 5 nearest while taking the distance into account
                val chosenPokestop = selectRandom(nearestUnused.take(settings.randomNextPokestopSelection), ctx)

                ctx.server.sendPokestop(chosenPokestop)

                if (settings.displayPokestopName) {
                    Log.normal("Walking to pokestop \"${chosenPokestop.name}\"")
                }

                walk(bot, ctx, settings, S2LatLng.fromDegrees(chosenPokestop.fortData.latitude, chosenPokestop.fortData.longitude), settings.speed, false, chosenPokestop)
            }
        }
    }

    private fun walk(bot: Bot, ctx: Context, settings: Settings, end: S2LatLng, speed: Double, sendDone: Boolean, pokestop: Pokestop?) {
        if (settings.followStreets.isNotEmpty()) {
            walkRoute(bot, ctx, settings, end, speed, sendDone, pokestop)
        } else {
            walkDirectly(bot, ctx, settings, end, speed, sendDone)
        }
    }

    private fun walkDirectly(bot: Bot, ctx: Context, settings: Settings, end: S2LatLng, speed: Double, sendDone: Boolean) {
        walkPath(bot, ctx, settings, mutableListOf(end), speed, sendDone, null)
    }

    private fun walkRoute(bot: Bot, ctx: Context, settings: Settings, end: S2LatLng, speed: Double, sendDone: Boolean, pokestop: Pokestop?) {
        val coordinatesList = getRouteCoordinates(S2LatLng.fromDegrees(ctx.lat.get(), ctx.lng.get()), end, settings, ctx.geoApiContext!!)
        if (coordinatesList.size > 0) {
            walkPath(bot, ctx, settings, coordinatesList, speed, sendDone, pokestop)
        } else {
            walkDirectly(bot, ctx, settings, end, speed, sendDone)
        }
    }

    //all walk functions should call this one
    private fun walkPath(bot: Bot, ctx: Context, settings: Settings, path: MutableList<S2LatLng>, speed: Double, sendDone: Boolean, pokestop: Pokestop?) {
        if (speed.equals(0)) {
            return
        }
        if (path.isEmpty()) {
            return
        }

        //random waiting
        if (Math.random() * 100 < settings.waitChance) {
            val waitTimeMin = settings.waitTimeMin
            val waitTimeMax = settings.waitTimeMax
            if (waitTimeMax > waitTimeMin) {
                val sleepTime: Long = (Math.random() * (waitTimeMax - waitTimeMin) + waitTimeMin).toLong()
                Log.yellow("Trainer grew tired, needs to rest a little (for $sleepTime seconds)")
                Thread.sleep(sleepTime * 1000)
            }
        }

        val randomSpeed = randomizeSpeed(speed, settings.randomSpeedRange, ctx)
        Log.green("Your character now moves at ${DecimalFormat("#0.0").format(randomSpeed)} m/s")
        ctx.walkingSpeed = AtomicDouble(randomSpeed)
        val timeout = 200L

        var remainingSteps = 0.0
        var deltaLat = 0.0
        var deltaLng = 0.0

        val pauseWalk: AtomicBoolean = AtomicBoolean(false)
        var pauseCounter = 2

        bot.runLoop(timeout, "WalkingLoop") { cancel ->

            // check if other task really needs us to stop for a second.
            // other task is responsible of unlocking this!
            if (ctx.pauseWalking.get()) {
                return@runLoop
            }

            if (remainingSteps <= 0) {
                if (path.isEmpty()) {
                    Log.normal("Destination reached.")
                    if (sendDone) {
                        ctx.server.sendGotoDone()
                    }

                    // Destination reached, if we follow streets, but the pokestop is not on a available from street, go directly
                    if (pokestop != null && settings.followStreets.isNotEmpty() && pokestop.canLoot(true, lootTimeouts) && !pokestop.canLoot(false, lootTimeouts)) {
                        Log.normal("Pokestop is too far using street, go directly!")
                        walkDirectly(bot, ctx, settings, S2LatLng.fromDegrees(pokestop.fortData.latitude, pokestop.fortData.longitude), speed, false)
                    } else {
                        ctx.walking.set(false)
                    }

                    cancel()
                } else {
                    //calculate delta lat/long for next step
                    val start = S2LatLng.fromDegrees(ctx.lat.get(), ctx.lng.get())
                    val nextPoint = path.first()
                    path.removeAt(0)
                    val diff = nextPoint.sub(start)
                    val distance = start.getEarthDistance(nextPoint)
                    val timeRequired = distance / randomSpeed
                    val stepsRequired = timeRequired / (timeout.toDouble() / 1000.toDouble())

                    deltaLat = diff.latDegrees() / stepsRequired
                    deltaLng = diff.lngDegrees() / stepsRequired

                    if (settings.displayKeepalive) Log.normal("Walking to ${nextPoint.toStringDegrees()} in ${Math.round(stepsRequired)} steps.")
                    remainingSteps = stepsRequired
                }
            }

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
        }
    }

    private fun selectRandom(pokestops: List<Pokestop>, ctx: Context): Pokestop {
        // Select random pokestop while taking the distance into account
        // E.g. pokestop is closer to the user -> higher probabilty to be chosen

        if (pokestops.size < 2)
            return pokestops.first()

        val currentPosition = S2LatLng.fromDegrees(ctx.lat.get(), ctx.lng.get())

        val distances = pokestops.map {
            val end = S2LatLng.fromDegrees(it.fortData.latitude, it.fortData.longitude)
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

    // The speed changes always in the desired range, meaning if you already have a low speed and it goes lower, it will change less
    private fun randomizeSpeed(speed: Double, speedRange: Double, ctx: Context): Double {
        if (speedRange > speed) {
            return speed
        }
        var speedDiff: Double = 0.0
        val minSpeed = speed - speedRange
        val maxSpeed = speed + speedRange
        // random value between -1 and  +1. There is always a 50:50 chance it will be slower or faster
        // The speedChange is now twice math.random so that it prefers small/slow acceleration, but has still a low chance of abruptly changing (like a human)
        val speedChangeNormalized = (Math.random() * 2 - 1) * Math.random()
        if (speedChangeNormalized > 0) {
            speedDiff = maxSpeed - ctx.walkingSpeed.toDouble()
        } else if (speedChangeNormalized < 0) {
            speedDiff = ctx.walkingSpeed.toDouble() - minSpeed
        }
        return ctx.walkingSpeed.toDouble() + speedChangeNormalized * speedDiff

    }
}
