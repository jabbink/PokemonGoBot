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
import java.util.*
import kotlin.concurrent.fixedRateTimer

class Walk(val sortedPokestops: List<Pokestop>, val lootTimeouts: Map<String, Long>) : Task {
    override fun run(bot: Bot, ctx: Context, settings: Settings) {
        // don't run away when there are still Pokemon around
        val pokemonCount = ctx.api.map?.catchablePokemon?.size
        if (pokemonCount != null && pokemonCount > 0) {
            return
        }
        if (!ctx.walking.compareAndSet(false, true)) {
            return
        }

        if(ctx.server.coordinatesToGoTo.size > 0){
            val coordinates = ctx.server.coordinatesToGoTo.first()
            ctx.server.coordinatesToGoTo.removeAt(0)
            Log.normal("Walking to ${coordinates.latRadians()}, ${coordinates.lngRadians()}")
            walk(ctx, S2LatLng.fromDegrees(coordinates.latRadians(), coordinates.lngRadians()), settings.speed, true)
        } else {
            val nearestUnused = sortedPokestops.filter {
                it.canLoot(true)
            }

            if (nearestUnused.size > 0) {
                ctx.server.sendPokestop(nearestUnused.first())

                if (settings.shouldDisplayPokestopName)
                    Log.normal("Walking to pokestop \"${nearestUnused.first().details.name}\"")
                walk(ctx, S2LatLng.fromDegrees(nearestUnused.first().latitude, nearestUnused.first().longitude), settings.speed, false)
            }
        }
    }

    fun walk(ctx: Context, end: S2LatLng, speed: Double, sendDone: Boolean) {
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

        fixedRateTimer("Walk", false, 0, timeout, action = {
            ctx.lat.addAndGet(deltaLat)
            ctx.lng.addAndGet(deltaLng)

            ctx.server.setLocation(ctx.lat.get(), ctx.lng.get())

            remainingSteps--
            if (remainingSteps <= 0) {
                Log.normal("Destination reached.")

                if(sendDone){
                    ctx.server.sendGotoDone()
                }

                ctx.walking.set(false)
                cancel()
            }
        })
    }
}