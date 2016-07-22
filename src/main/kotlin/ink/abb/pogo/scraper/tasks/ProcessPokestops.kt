/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper.tasks

import com.google.common.geometry.S2LatLng
import com.pokegoapi.api.map.fort.Pokestop
import ink.abb.pogo.scraper.Bot
import ink.abb.pogo.scraper.Context
import ink.abb.pogo.scraper.Settings
import ink.abb.pogo.scraper.Task
import java.util.*

/**
 * Task that handles catching pokemon, activating stops, and walking to a new target.
 *
 * @author Andrew Potter (apottere)
 */
class ProcessPokestops(val pokestops: MutableCollection<Pokestop>) : Task {

    override fun run(bot: Bot, ctx: Context, settings: Settings) {
        val sortedPokestops = pokestops.sortedWith(Comparator { a, b ->
            val locationA = S2LatLng.fromDegrees(a.latitude, a.longitude)
            val locationB = S2LatLng.fromDegrees(b.latitude, b.longitude)
            val self = S2LatLng.fromDegrees(ctx.lat.get(), ctx.lng.get())
            val distanceA = self.getEarthDistance(locationA)
            val distanceB = self.getEarthDistance(locationB)
            distanceA.compareTo(distanceB)
        })

        val loot = LootOneNearbyPokestop(sortedPokestops)
        val walk = WalkToUnusedPokestop(sortedPokestops)

        bot.task(loot)
        bot.task(walk)
    }
}