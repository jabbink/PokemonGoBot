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
class ProcessMapObjects(val pokestops: MutableCollection<Pokestop>) : Task {

    val catch = CatchOneNearbyPokemon()

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

        bot.task(catch)
        bot.task(loot)
        bot.task(walk)
    }
}