/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper.tasks

import POGOProtos.Networking.Responses.FortSearchResponseOuterClass.FortSearchResponse.Result
import com.pokegoapi.api.map.fort.Pokestop
import com.pokegoapi.api.map.fort.PokestopLootResult
import com.pokegoapi.google.common.geometry.S2LatLng
import ink.abb.pogo.scraper.Bot
import ink.abb.pogo.scraper.Context
import ink.abb.pogo.scraper.Settings
import ink.abb.pogo.scraper.Task
import ink.abb.pogo.scraper.util.Log
import ink.abb.pogo.scraper.util.map.canLoot
import java.util.*

class LootOneNearbyPokestop(val sortedPokestops: List<Pokestop>, val lootTimeouts: HashMap<String, Long>) : Task {

    private var cooldownPeriod = 5

    override fun run(bot: Bot, ctx: Context, settings: Settings) {
        val nearbyPokestops = sortedPokestops.filter {
            it.canLoot(lootTimeouts = lootTimeouts, api = ctx.api)
        }

        if (nearbyPokestops.isNotEmpty()) {
            val closest = nearbyPokestops.first()
            var pokestopID = closest.id
            if (settings.shouldDisplayPokestopName)
                pokestopID = "\"${closest.details.name}\""
            Log.normal("Looting nearby pokestop $pokestopID")
            ctx.api.setLocation(ctx.lat.get(), ctx.lng.get(), 0.0)
            val result = closest.loot()

            if (result?.itemsAwarded != null) {
                ctx.itemStats.first.getAndAdd(result.itemsAwarded.size)
            }

            if (result.experience > 0) {
                ctx.server.sendProfile()
            }

            when (result.result) {
                Result.SUCCESS -> {
                    ctx.server.sendPokestop(closest)
                    ctx.server.sendProfile()
                    var message = "Looted pokestop $pokestopID; +${result.experience} XP"
                    if (settings.shouldDisplayPokestopSpinRewards)
                        message += ": ${result.itemsAwarded.groupBy { it.itemId.name }.map { "${it.value.size}x${it.key}" }}"
                    Log.green(message)
                    lootTimeouts.put(closest.id, closest.cooldownCompleteTimestampMs)
                    checkForBan(result, closest, bot, settings)
                }
                Result.INVENTORY_FULL -> {
                    ctx.server.sendPokestop(closest)
                    ctx.server.sendProfile()
                    var message = "Looted pokestop $pokestopID; +${result.experience} XP, but inventory is full"
                    if (settings.shouldDisplayPokestopSpinRewards)
                        message += ": ${result.itemsAwarded.groupBy { it.itemId.name }.map { "${it.value.size}x${it.key}" }}"

                    Log.red(message)
                    lootTimeouts.put(closest.id, closest.cooldownCompleteTimestampMs)
                }
                Result.OUT_OF_RANGE -> {
                    val location = S2LatLng.fromDegrees(closest.latitude, closest.longitude)
                    val self = S2LatLng.fromDegrees(ctx.lat.get(), ctx.lng.get())
                    val distance = self.getEarthDistance(location)
                    Log.red("Pokestop out of range; distance: $distance")
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
    }

    private fun checkForBan(result: PokestopLootResult, pokestop: Pokestop, bot: Bot, settings: Settings) {
        if (settings.banSpinCount > 0 && result.experience == 0 && result.itemsAwarded.isEmpty()) {
            Log.red("Looks like a ban. Trying to bypass softban by repeatedly spinning the pokestop.")
            bot.task(BypassSoftban(pokestop))
            Log.yellow("Finished softban bypass attempt. Continuing.")
            // Add pokestop to cooldown list to prevent immediate retry in the next loop
            lootTimeouts.put(pokestop.id, bot.ctx.api.currentTimeMillis() + cooldownPeriod * 60 * 1000)
        }
    }
}

