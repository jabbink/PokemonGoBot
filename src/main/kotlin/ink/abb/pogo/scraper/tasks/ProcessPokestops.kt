/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper.tasks

import ink.abb.pogo.api.cache.Pokestop
import ink.abb.pogo.scraper.Bot
import ink.abb.pogo.scraper.Context
import ink.abb.pogo.scraper.Settings
import ink.abb.pogo.scraper.Task
import ink.abb.pogo.scraper.util.Log
import ink.abb.pogo.scraper.util.map.distance
import ink.abb.pogo.scraper.util.pokemon.distance
import ink.abb.pogo.scraper.util.map.inRangeForLuredPokemon
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Task that handles catching pokemon, activating stops, and walking to a new target.
 */
class ProcessPokestops(var pokestops: List<Pokestop>) : Task {

    val refetchTime = TimeUnit.SECONDS.toMillis(30)
    var lastFetch: Long = 0

    private val lootTimeouts = HashMap<String, Long>()
    var startPokestop: Pokestop? = null

    override fun run(bot: Bot, ctx: Context, settings: Settings) {
        var writeCampStatus = false
        if (lastFetch + refetchTime < bot.api.currentTimeMillis()) {
            writeCampStatus = true
            lastFetch = bot.api.currentTimeMillis()
            if (settings.allowLeaveStartArea) {
                try {
                    val newStops = ctx.api.map.getPokestops(ctx.api.latitude, ctx.api.longitude, 9)
                    if (newStops.size > 0) {
                        pokestops = newStops
                    }
                } catch (e: Exception) {
                    // ignored failed request
                }
            }
        }
        val sortedPokestops = pokestops.sortedWith(Comparator { a, b ->
            a.distance.compareTo(b.distance)
        })
        if (startPokestop == null)
            startPokestop = sortedPokestops.first()

        if (settings.lootPokestop) {
            val loot = LootOneNearbyPokestop(sortedPokestops, lootTimeouts)
            try {
                bot.task(loot)
            } catch (e: Exception) {
                ctx.pauseWalking.set(false)
            }
        }

        if (settings.campLurePokestop > 0 && !ctx.pokemonInventoryFullStatus.get() && settings.catchPokemon) {
            val luresInRange = sortedPokestops.filter {
                it.inRangeForLuredPokemon() && it.fortData.hasLureInfo()
            }
            if (luresInRange.size >= settings.campLurePokestop) {
                if (writeCampStatus) {
                    Log.green("${luresInRange.size} lure(s) in range, pausing")
                }
                return
            }
        }
        val walk = Walk(sortedPokestops, lootTimeouts)

        bot.task(walk)
    }
}
