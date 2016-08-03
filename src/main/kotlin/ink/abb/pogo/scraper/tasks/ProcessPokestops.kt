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
import ink.abb.pogo.scraper.util.map.inRange
import java.util.*

/**
 * Task that handles catching pokemon, activating stops, and walking to a new target.
 */
class ProcessPokestops(var pokestops: MutableCollection<Pokestop>) : Task {

    private val lootTimeouts = HashMap<String, Long>()
    var startPokestop: Pokestop? = null
    override fun run(bot: Bot, ctx: Context, settings: Settings) {
        if (settings.allowLeaveStartArea) {
            try {
                val newStops = ctx.api.map.mapObjects.pokestops
                if (newStops.size > 0) {
                    pokestops = newStops
                }
            } catch (e: Exception) {
                // ignored failed request
            }
        }
        val sortedPokestops = pokestops.sortedWith(Comparator { a, b ->
            val locationA = S2LatLng.fromDegrees(a.latitude, a.longitude)
            val locationB = S2LatLng.fromDegrees(b.latitude, b.longitude)
            val self = S2LatLng.fromDegrees(ctx.lat.get(), ctx.lng.get())
            val distanceA = self.getEarthDistance(locationA)
            val distanceB = self.getEarthDistance(locationB)
            distanceA.compareTo(distanceB)
        })
        if (startPokestop == null)
            startPokestop = sortedPokestops.first()

        if (settings.lootPokestop) {
            val loot = LootOneNearbyPokestop(sortedPokestops, lootTimeouts)
            bot.task(loot)
        }
        if (settings.campLurePokestop > 0) {
            // TODO: grab that 70 from DOWNLOAD_SETTINGS
            val luresInRange = sortedPokestops.filter { it.inRange(ctx.api, 70.0) && it.hasLure() }.size
            if (luresInRange >= settings.campLurePokestop) {
                Log.green("$luresInRange lures in range, pausing")
                ctx.api.map.getMapObjects(3)
                return
            }
        }
        val walk = Walk(sortedPokestops, lootTimeouts)

        bot.task(walk)
    }
}