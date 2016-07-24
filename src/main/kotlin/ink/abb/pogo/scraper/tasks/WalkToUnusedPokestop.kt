/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper.tasks

import com.lynden.gmapsfx.javascript.`object`.LatLong
import com.lynden.gmapsfx.javascript.`object`.MapOptions
import com.lynden.gmapsfx.javascript.`object`.Marker
import com.lynden.gmapsfx.javascript.`object`.MarkerOptions
import com.pokegoapi.api.map.fort.Pokestop
import com.pokegoapi.google.common.geometry.S2LatLng
import ink.abb.pogo.scraper.Bot
import ink.abb.pogo.scraper.Context
import ink.abb.pogo.scraper.Settings
import ink.abb.pogo.scraper.Task
import ink.abb.pogo.scraper.util.Log
import javafx.application.Platform
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
            it.canLoot(true)
        }

        if (nearestUnused.size > 0) {
            if (settings.shouldDisplayWalkingToNearestUnused)
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

        fixedRateTimer("Walk", false, 0, timeout, action = {
            ctx.lat.addAndGet(deltaLat)
            ctx.lng.addAndGet(deltaLng)
            remainingSteps--
            if (remainingSteps <= 0) {
                Log.normal("Destination reached.")
                Platform.runLater {
                    val mapOptions = MapOptions()
                    mapOptions.center(LatLong(end.latDegrees(), end.lngDegrees())).overviewMapControl(false).panControl(false).rotateControl(false).scaleControl(false).streetViewControl(false).zoomControl(false).zoom(12)
                    ctx.screen.map = ctx.screen.mapView.createMap(mapOptions)

                    val currentLocation = LatLong(end.latDegrees(), end.lngDegrees())
                    val markerOptions1 = MarkerOptions()
                    markerOptions1.position(currentLocation)
                    val currentMarker = Marker(markerOptions1)
                    ctx.screen.map!!.addMarker(currentMarker)
                }
                ctx.walking.set(false)
                cancel()
            }
        })
    }

}