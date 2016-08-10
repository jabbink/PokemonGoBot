/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper.tasks

import com.pokegoapi.api.map.fort.Pokestop
import ink.abb.pogo.scraper.Bot
import ink.abb.pogo.scraper.Context
import ink.abb.pogo.scraper.Settings
import ink.abb.pogo.scraper.Task
import ink.abb.pogo.scraper.util.Log
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Task that handles catching pokemon, activating stops, and walking to a new target.
 */
class ProcessPokestops(var pokestops: MutableCollection<Pokestop>) : Task {

    val refetchTime = TimeUnit.SECONDS.toMillis(30)
    var lastFetch: Long = 0

    private val lootTimeouts = HashMap<String, Long>()
    var startPokestop: Pokestop? = null
    override fun run(bot: Bot, ctx: Context, settings: Settings) {
        if (lastFetch + refetchTime < bot.api.currentTimeMillis() && settings.allowLeaveStartArea) {
            lastFetch = bot.api.currentTimeMillis()
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
            a.distance.compareTo(b.distance)
        })
        if (startPokestop == null)
            startPokestop = sortedPokestops.first()

        if (settings.lootPokestop) {
            val loot = LootOneNearbyPokestop(sortedPokestops, lootTimeouts)
            bot.task(loot)
        }
        if (settings.campLurePokestop > 0 && settings.catchPokemon) {
            val luresInRange = sortedPokestops.filter {
                it.inRangeForLuredPokemon() && it.fortData.hasLureInfo()
            }.size
            if (luresInRange >= settings.campLurePokestop) {
                Log.green("$luresInRange lure(s) in range, pausing")
                return
            }
        }
        val walk = Walk(sortedPokestops, lootTimeouts)

        bot.task(walk)
    }
}