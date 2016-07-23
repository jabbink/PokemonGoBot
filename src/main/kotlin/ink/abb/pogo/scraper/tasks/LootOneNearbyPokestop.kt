/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper.tasks

import Log
import POGOProtos.Networking.Responses.FortSearchResponseOuterClass.FortSearchResponse.Result
import com.pokegoapi.api.map.fort.Pokestop
import com.pokegoapi.google.common.geometry.S2LatLng
import ink.abb.pogo.scraper.Bot
import ink.abb.pogo.scraper.Context
import ink.abb.pogo.scraper.Settings
import ink.abb.pogo.scraper.Task

class LootOneNearbyPokestop(val sortedPokestops: List<Pokestop>) : Task {
    override fun run(bot: Bot, ctx: Context, settings: Settings) {
        val nearbyPokestops = sortedPokestops.filter {
            it.canLoot()
        }

        if (nearbyPokestops.size > 0) {
            val closest = nearbyPokestops.first()
            Log.normal("Looting nearby pokestop ${closest.id}")
            ctx.api.setLocation(ctx.lat.get(), ctx.lng.get(), 0.0)
            val result = closest.loot()

            if (result?.itemsAwarded != null) {
                ctx.itemStats.first.getAndAdd(result.itemsAwarded.size)
            }
            when (result.result) {
                Result.SUCCESS -> {
                    var message = "Looted pokestop ${closest.id}"
                    if(settings.shouldDisplayPokestopSpinRewards)
                        message += ": ${result.itemsAwarded.groupBy { it.itemId.name }.map { "${it.value.size}x${it.key}" }}"
                    Log.green(message)
                }
                Result.INVENTORY_FULL -> {

                    var message = "Looted pokestop ${closest.id}, but inventory is full"
                    if(settings.shouldDisplayPokestopSpinRewards)
                        message += ": ${result.itemsAwarded.groupBy { it.itemId.name }.map { "${it.value.size}x${it.key}" }}"

                    Log.red(message)
                }
                Result.OUT_OF_RANGE -> {
                    val location = S2LatLng.fromDegrees(closest.latitude, closest.longitude)
                    val self = S2LatLng.fromDegrees(ctx.lat.get(), ctx.lng.get())
                    val distance = self.getEarthDistance(location)
                    Log.red("Pokestop out of range; distance: $distance")
                }
                else -> println(result.result)
            }
        }
    }
}