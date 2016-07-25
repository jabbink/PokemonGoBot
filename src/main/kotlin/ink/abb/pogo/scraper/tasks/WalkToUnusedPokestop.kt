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
import kotlin.concurrent.fixedRateTimer

class WalkToUnusedPokestop(val sortedPokestops: List<Pokestop>, val lootTimeouts: Map<String, Long>) : Task {

    override fun run(bot: Bot, ctx: Context, settings: Settings) {
        // don't run away when there are still Pokemon around
        val pokemonCount = ctx.api.map?.catchablePokemon?.size
        if (pokemonCount != null && pokemonCount > 0) {
            return
        }
        if (!ctx.walking.compareAndSet(false, true)) {
            return
        }

        val nearestUnused = sortedPokestops.filter {
            it.canLoot(true) && lootTimeouts.getOrElse(it.id, { 0 }) < System.currentTimeMillis()
        }

        if (nearestUnused.size > 0) {
            walk(ctx, nearestUnused.first(), settings.speed, settings)
        }
    }

    fun walk(ctx: Context, pokestop: Pokestop, speed: Double, settings: Settings) {
        val start = S2LatLng.fromDegrees(ctx.lat.get(), ctx.lng.get())
        val end = S2LatLng.fromDegrees(pokestop.latitude, pokestop.longitude)

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

        if (settings.shouldDisplayWalkingToNearestUnused)
            Log.normal("Walking to pokestop \"${pokestop.details.name}\" ${end.toStringDegrees()} in $stepsRequired steps.")
        var remainingSteps = stepsRequired

        fixedRateTimer("Walk", false, 0, timeout, action = {
            ctx.lat.addAndGet(deltaLat)
            ctx.lng.addAndGet(deltaLng)
            remainingSteps--
            if (remainingSteps <= 0) {
                if (settings.shouldDisplayWalkingToNearestUnused)
                    Log.normal("Destination reached.")
                ctx.walking.set(false)
                cancel()
            }
        })
    }

}