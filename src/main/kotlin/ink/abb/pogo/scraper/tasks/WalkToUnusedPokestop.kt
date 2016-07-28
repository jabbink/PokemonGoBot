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
import ink.abb.pogo.scraper.util.Helper
import ink.abb.pogo.scraper.util.map.canLoot
import kotlin.concurrent.fixedRateTimer
import kotlin.concurrent.timer
import kotlin.concurrent.thread
import java.util.concurrent.TimeUnit

class WalkToUnusedPokestop(val sortedPokestops: List<Pokestop>, val lootTimeouts: Map<String, Long>) : Task {

    override fun run(bot: Bot, ctx: Context, settings: Settings) {
        // don't run away when there are still Pokemon around
        val pokemonCount = ctx.api.map?.catchablePokemon?.filter { !ctx.blacklistedEncounters.contains(it.encounterId) }?.size

        if (pokemonCount != null && pokemonCount > 0 && settings.shouldCatchPokemons) {
            return
        }

        // still walking to existing route, so let's exit this task.
        if (!ctx.walking.compareAndSet(false, true)) {
            return
        }

        val nearestUnused = sortedPokestops.filter {            

                val canLoot = it.canLoot(ignoreDistance = true, lootTimeouts = lootTimeouts)
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

            if (settings.shouldDisplayPokestopName)
                Log.normal("Walking to pokestop \"${chosenPokestop.details.name}\"")

            walk(ctx, S2LatLng.fromDegrees(chosenPokestop.latitude, chosenPokestop.longitude), settings.speed)
        }
    }

    fun walk(ctx: Context, end: S2LatLng, speed: Double) {
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

        thread(true, false, null, "Walk", 1, block = {
            var threadRun = true

            while(threadRun) {

                // default delay per steps
                var randomTimeout = timeout + (Helper.getRandomNumber(0, timeout.toInt()).toLong() * 2)
                TimeUnit.MILLISECONDS.sleep(randomTimeout)

                // 10% chance to do NOTHING.
                var dummy = Helper.getRandomNumber(0,100)
                if (dummy <= 5) {

                    val sleeptime = Helper.getRandomNumber(1,5)
                    Log.yellow("I'm doing nothing .. Replicate human behavior (sleep for $sleeptime seconds.)")

                    TimeUnit.SECONDS.sleep(sleeptime.toLong())
                }

                else {
                    ctx.lat.addAndGet(deltaLat)
                    ctx.lng.addAndGet(deltaLng)

                    remainingSteps--
                    if (remainingSteps <= 0) {
                        Log.normal("Destination reached.")
                        ctx.walking.set(false)
                        threadRun = false

                        // stop at the pokestop for random seconds
                        ctx.stopAtPoint.getAndSet(true)
                        val randomStopTimeout = Helper.getRandomNumber(30, 600)
                        Log.blue("We are stopping at this Pokestop for $randomStopTimeout seconds.")
                        TimeUnit.SECONDS.sleep(randomStopTimeout.toLong())

                        ctx.stopAtPoint.getAndSet(false)
                        Log.magenta("We are done stopping. Begin to search for our next pokestop!")

                    }
                }
            }

        })
    }

    private fun selectRandom(pokestops: List<Pokestop>, ctx: Context) : Pokestop {
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
            val probability = (1 - distances[index]/totalDistance) / (pokestops.size - 1)         

            cumulativeProbability += probability
            if (random <= cumulativeProbability) {
                return pokestop
            }
        }

        // should not happen
        return pokestops.first()
    }    
}
