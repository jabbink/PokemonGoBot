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
import ink.abb.pogo.scraper.util.map.canLoot
import ink.abb.pogo.scraper.util.map.getCatchablePokemon

class Walk(val sortedPokestops: List<Pokestop>, val lootTimeouts: Map<String, Long>) : Task {
    override fun run(bot: Bot, ctx: Context, settings: Settings) {
        if (!ctx.walking.compareAndSet(false, true)) {
            return
        }

        if (ctx.server.coordinatesToGoTo.size > 0) {
            val coordinates = ctx.server.coordinatesToGoTo.first()
            ctx.server.coordinatesToGoTo.removeAt(0)
            Log.normal("Walking to ${coordinates.latRadians()}, ${coordinates.lngRadians()}")

            if (settings.shouldFollowStreets) {
                walkRoute(bot, ctx, settings, S2LatLng.fromDegrees(coordinates.latRadians(), coordinates.lngRadians()), settings.speed, true)
            } else {
                walk(bot, ctx, settings, S2LatLng.fromDegrees(coordinates.latRadians(), coordinates.lngRadians()), settings.speed, true)
            }
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
                val chosenPokestop = selectRandom(nearestUnused.take(settings.randomNextPokestop), ctx)

                ctx.server.sendPokestop(chosenPokestop)

                if (settings.shouldDisplayPokestopName)
                    Log.normal("Walking to pokestop \"${chosenPokestop.details.name}\"")

                if (settings.shouldFollowStreets) {
                    walkRoute(bot, ctx, settings, S2LatLng.fromDegrees(chosenPokestop.latitude, chosenPokestop.longitude), settings.speed, false)
                } else {
                    walk(bot, ctx, settings, S2LatLng.fromDegrees(chosenPokestop.latitude, chosenPokestop.longitude), settings.speed, false)
                }
            }
        }
    }

    fun walk(bot: Bot, ctx: Context, settings: Settings, end: S2LatLng, speed: Double, sendDone: Boolean) {
        val start = S2LatLng.fromDegrees(ctx.lat.get(), ctx.lng.get())
        val diff = end.sub(start)
        val distance = start.getEarthDistance(end)
        val timeout = 200L
        // prevent division by 0
        if (speed.equals(0)) {
            return
        }
        val timeRequired = distance / speed
        val stepsRequired = timeRequired / (timeout.toDouble() / 1000.toDouble())
        // prevent division by 0
        if (stepsRequired.equals(0)) {
            return
        }
        val deltaLat = diff.latDegrees() / stepsRequired
        val deltaLng = diff.lngDegrees() / stepsRequired

        Log.normal("Walking to ${end.toStringDegrees()} in $stepsRequired steps.")
        var remainingSteps = stepsRequired

        var walking = true
        bot.runLoop(timeout, "WalkingLoop") { cancel ->
            // don't run away when there are still Pokemon around
            if (walking) {
                if (bot.api.map.getCatchablePokemon(ctx.blacklistedEncounters).size > 0 && settings.shouldCatchPokemons) {
                    // Stop walking
                    walking = false
                    Log.normal("Pausing to catch pokemon...")
                } // Else continue walking.
            } else {
                if (bot.api.map.getCatchablePokemon(ctx.blacklistedEncounters).size <= 0) {
                    walking = true
                    Log.normal("Resuming walk.")
                } // Else continue waiting.
            }

            if (!walking) {
                return@runLoop
            }

            ctx.lat.addAndGet(deltaLat)
            ctx.lng.addAndGet(deltaLng)

            ctx.server.setLocation(ctx.lat.get(), ctx.lng.get())

            remainingSteps--
            if (remainingSteps <= 0) {
                Log.normal("Destination reached.")

                if (sendDone) {
                    ctx.server.sendGotoDone()
                }

                ctx.walking.set(false)
                cancel()
            }
        }
    }

    fun walkRoute(bot: Bot, ctx: Context, settings: Settings, end: S2LatLng, speed: Double, sendDone: Boolean) {
        if (speed.equals(0)) {
            return
        }
        val timeout = 200L
        val coordinatesList = getRouteCoordinates(S2LatLng.fromDegrees(ctx.lat.get(), ctx.lng.get()), end)
        if (coordinatesList.size <= 0) {
            walk(bot, ctx, settings, end, speed, sendDone)
        } else {
            var walking = true
            bot.runLoop(timeout, "WalkingLoop") { cancel ->
                if (walking) {
                    if (bot.api.map.getCatchablePokemon(ctx.blacklistedEncounters).size > 0 && settings.shouldCatchPokemons) {
                        // Stop walking
                        walking = false
                        Log.normal("Pausing to catch pokemon...")
                    } // Else continue walking.
                } else {
                    if (bot.api.map.getCatchablePokemon(ctx.blacklistedEncounters).size <= 0) {
                        walking = true
                        Log.normal("Resuming walk.")
                    } // Else continue waiting.
                }

                if (!walking) {
                    return@runLoop
                }


                val start = S2LatLng.fromDegrees(ctx.lat.get(), ctx.lng.get())
                val step = coordinatesList.first()
                coordinatesList.removeAt(0)
                val diff = step.sub(start)
                val distance = start.getEarthDistance(step)
                val timeRequired = distance / speed
                val stepsRequired = timeRequired / (timeout.toDouble() / 1000.toDouble())
                if (stepsRequired.equals(0)) {
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
                    Log.normal("Destination reached.")
                    if (sendDone) {
                        ctx.server.sendGotoDone()
                    }
                    ctx.walking.set(false)
                    cancel()

                }
            }
        }
    }

    fun walkAndComeBack(bot: Bot, ctx: Context, settings: Settings, end: S2LatLng, speed: Double, sendDone: Boolean) {
        val start = S2LatLng.fromDegrees(ctx.lat.get(), ctx.lng.get())
        val diff = end.sub(start)
        val distance = start.getEarthDistance(end)
        val timeout = 200L
        // prevent division by 0
        if (speed.equals(0)) {
            return
        }
        val timeRequired = distance / speed
        val stepsRequired = timeRequired / (timeout.toDouble() / 1000.toDouble())
        // prevent division by 0
        if (stepsRequired.equals(0)) {
            return
        }
        val deltaLat = diff.latDegrees() / stepsRequired
        val deltaLng = diff.lngDegrees() / stepsRequired
        val deltaLat2 = -deltaLat
        val deltaLng2 = -deltaLng

        Log.normal("Walking to ${end.toStringDegrees()} in $stepsRequired steps.")
        var remainingStepsGoing = stepsRequired
        var remainingStepsComing = stepsRequired
        var walking = true
        bot.runLoop(timeout, "WalkingLoop") { cancel ->
            // don't run away when there are still Pokemon around
            if (walking) {
                if (bot.api.map.getCatchablePokemon(ctx.blacklistedEncounters).size > 0 && settings.shouldCatchPokemons) {
                    // Stop walking
                    walking = false
                    Log.normal("Pausing to catch pokemon...")
                } // Else continue walking.
            } else {
                if (bot.api.map.getCatchablePokemon(ctx.blacklistedEncounters).size <= 0) {
                    walking = true
                    Log.normal("Resuming walk.")
                } // Else continue waiting.
            }

            if (!walking) {
                return@runLoop
            }
            if (remainingStepsGoing > 0) {
                ctx.lat.addAndGet(deltaLat)
                ctx.lng.addAndGet(deltaLng)

                ctx.server.setLocation(ctx.lat.get(), ctx.lng.get())
                remainingStepsGoing--
            } else if (remainingStepsGoing <= 0) {
                ctx.lat.addAndGet(deltaLat2)
                ctx.lng.addAndGet(deltaLng2)

                ctx.server.setLocation(ctx.lat.get(), ctx.lng.get())

                remainingStepsComing--
            }

            if (remainingStepsComing <= 0) {
                Log.normal("Destination reached.")

                if (sendDone) {
                    ctx.server.sendGotoDone()
                }

                ctx.walking.set(false)
                cancel()
            }
        }
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
        var cumulativeProbability = 0.0;

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
