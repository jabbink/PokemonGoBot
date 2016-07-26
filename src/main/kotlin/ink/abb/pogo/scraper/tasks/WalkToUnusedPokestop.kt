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
        val pokemonCount = ctx.api.map?.catchablePokemon?.size
        if (pokemonCount != null && pokemonCount > 0) {
            return
        }

        // still walking to existing route, so let's exit this task.
        if (!ctx.walking.compareAndSet(false, true)) {
            return
        }

        val nearestUnused = sortedPokestops.filter {
            it.canLoot(ignoreDistance = true, lootTimeouts = lootTimeouts)
        }

        if (nearestUnused.isNotEmpty()) {
            if (settings.shouldDisplayPokestopName)
                Log.normal("Walking to pokestop \"${nearestUnused.first().details.name}\"")
            walk(ctx, S2LatLng.fromDegrees(nearestUnused.first().latitude, nearestUnused.first().longitude), settings.speed)
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

/*
        timer("Walk", false, 0, timeout, action = {

            // 10% chance to do NOTHING.
            val dummy = Helper.getRandomNumber(0,100)
            if (dummy <= 10) {

                val sleeptime = Helper.getRandomNumber(1,3)
                Log.yellow("I'm doing nothing .. Replicate human bevahior (sleep for $sleeptime seconds.)")

                TimeUnit.SECONDS.sleep(sleeptime.toLong())
            }

            else {
                ctx.lat.addAndGet(deltaLat)
                ctx.lng.addAndGet(deltaLng)

                remainingSteps--
                if (remainingSteps <= 0) {
                    Log.normal("Destination reached.")
                    ctx.walking.set(false)
                    cancel()
                }
            }
        })
*/

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
                    Log.yellow("I'm doing nothing .. Replicate human bevahior (sleep for $sleeptime seconds.)")

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
                    }
                }
            }

        })
    }
}
