package ink.abb.pogo.scraper.tasks

import POGOProtos.Networking.Responses.FortSearchResponseOuterClass.FortSearchResponse.Result
import com.pokegoapi.api.map.fort.Pokestop
import com.pokegoapi.google.common.geometry.S2LatLng
import ink.abb.pogo.scraper.Bot
import ink.abb.pogo.scraper.Context
import ink.abb.pogo.scraper.Settings
import ink.abb.pogo.scraper.Task

/**
 * @author Andrew Potter (apottere)
 */
class LootOneNearbyPokestop(val sortedPokestops: List<Pokestop>) : Task {
    override fun run(bot: Bot, ctx: Context, settings: Settings) {
        val nearbyPokestops = sortedPokestops.filter {
            it.canLoot()
        }

        if (nearbyPokestops.size > 0) {
            println("Found nearby pokestop")
            val closest = nearbyPokestops.first()
            ctx.api.setLocation(ctx.lat.get(), ctx.lng.get(), 0.0)
            val result = closest.loot()
            when (result.result) {
                Result.SUCCESS -> println("Activated portal ${closest.id}")
                Result.INVENTORY_FULL -> {
                    println("Activated portal ${closest.id}, but inventory is full")
                }
                Result.OUT_OF_RANGE -> {
                    val location = S2LatLng.fromDegrees(closest.latitude, closest.longitude)
                    val self = S2LatLng.fromDegrees(ctx.lat.get(), ctx.lng.get())
                    val distance = self.getEarthDistance(location)
                    println("Portal out of range; distance: $distance")
                }
                else -> println(result.result)
            }
        }
    }
}