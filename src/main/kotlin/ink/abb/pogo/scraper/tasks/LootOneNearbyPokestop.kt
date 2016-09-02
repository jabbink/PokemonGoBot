/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper.tasks

import POGOProtos.Networking.Responses.FortSearchResponseOuterClass
import POGOProtos.Networking.Responses.FortSearchResponseOuterClass.FortSearchResponse.Result
import ink.abb.pogo.api.cache.Pokestop
import ink.abb.pogo.scraper.Bot
import ink.abb.pogo.scraper.Context
import ink.abb.pogo.scraper.Settings
import ink.abb.pogo.scraper.Task
import ink.abb.pogo.scraper.util.Log
import ink.abb.pogo.scraper.util.directions.getAltitude
import ink.abb.pogo.scraper.util.map.canLoot
import ink.abb.pogo.scraper.util.map.distance
import ink.abb.pogo.scraper.util.map.loot
import java.text.DecimalFormat
import java.util.*

class LootOneNearbyPokestop(val sortedPokestops: List<Pokestop>, val lootTimeouts: HashMap<String, Long>) : Task {

    private var cooldownPeriod = 5

    override fun run(bot: Bot, ctx: Context, settings: Settings) {
        // STOP WALKING! until loot is done
        ctx.pauseWalking.set(true)
        ctx.api.setLocation(ctx.lat.get(), ctx.lng.get(), getAltitude(ctx.lat.get(), ctx.lng.get(), ctx))
        val nearbyPokestops = sortedPokestops.filter {
            it.canLoot(lootTimeouts = lootTimeouts)
        }

        if (nearbyPokestops.isNotEmpty()) {
            val closest = nearbyPokestops.first()
            var pokestopID = closest.id
            if (settings.displayPokestopName) {
                pokestopID = "\"${closest.name}\""
            }
            Log.normal("Looting nearby pokestop $pokestopID")

            val result = closest.loot().toBlocking().first().response

            if (result.itemsAwardedCount != 0) {
                ctx.itemStats.first.getAndAdd(result.itemsAwardedCount)
            }

            if (result.experienceAwarded > 0) {
                ctx.server.sendProfile()
            }

            when (result.result) {
                Result.SUCCESS -> {
                    ctx.server.sendPokestop(closest)
                    ctx.server.sendProfile()
                    var message = "Looted pokestop $pokestopID; +${result.experienceAwarded} XP"
                    if (settings.displayPokestopRewards)
                        message += ": ${result.itemsAwardedList.groupBy { it.itemId.name }.map { "${it.value.size}x${it.key}" }}"
                    Log.green(message)
                    lootTimeouts.put(closest.id, closest.cooldownCompleteTimestampMs)
                    checkForBan(result, closest, bot, settings)
                }
                Result.INVENTORY_FULL -> {
                    ctx.server.sendPokestop(closest)
                    ctx.server.sendProfile()
                    var message = "Looted pokestop $pokestopID; +${result.experienceAwarded} XP, but inventory is full"
                    if (settings.displayPokestopRewards)
                        message += ": ${result.itemsAwardedList.groupBy { it.itemId.name }.map { "${it.value.size}x${it.key}" }}"

                    Log.red(message)
                    lootTimeouts.put(closest.id, closest.cooldownCompleteTimestampMs)
                }
                Result.OUT_OF_RANGE -> {
                    Log.red("Pokestop out of range; our calculated distance: ${DecimalFormat("#0.00").format(closest.distance)}m")
                    if (closest.distance < ctx.api.fortSettings.interactionRangeMeters) {
                        Log.red("Server is lying to us (${Math.round(closest.distance)}m < ${Math.round(ctx.api.fortSettings.interactionRangeMeters)}m!); blacklisting for $cooldownPeriod minutes")
                        lootTimeouts.put(closest.id, ctx.api.currentTimeMillis() + cooldownPeriod * 60 * 1000)
                    }
                }
                Result.IN_COOLDOWN_PERIOD -> {
                    lootTimeouts.put(closest.id, ctx.api.currentTimeMillis() + cooldownPeriod * 60 * 1000)
                    Log.red("Pokestop still in cooldown mode; blacklisting for $cooldownPeriod minutes")
                }
                Result.NO_RESULT_SET -> {
                    lootTimeouts.put(closest.id, ctx.api.currentTimeMillis() + cooldownPeriod * 60 * 1000)
                    Log.red("Server refuses to loot this Pokestop (usually temporary issue); blacklisting for $cooldownPeriod minutes")
                }
                else -> Log.yellow(result.result.toString())
            }
        }
        // unlock walk block
        ctx.pauseWalking.set(false)
    }

    private fun checkForBan(result: FortSearchResponseOuterClass.FortSearchResponse, pokestop: Pokestop, bot: Bot, settings: Settings) {
        if (settings.banSpinCount > 0 && result.experienceAwarded == 0 && result.itemsAwardedCount == 0) {
            Log.red("Looks like a ban. Trying to bypass softban by repeatedly spinning the pokestop.")
            bot.task(BypassSoftban(pokestop))
            Log.yellow("Finished softban bypass attempt. Continuing.")
            // Add pokestop to cooldown list to prevent immediate retry in the next loop
            lootTimeouts.put(pokestop.id, bot.ctx.api.currentTimeMillis() + cooldownPeriod * 60 * 1000)
        }
    }
}

