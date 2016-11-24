/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper.tasks

import POGOProtos.Networking.Responses.LevelUpRewardsResponseOuterClass
import ink.abb.pogo.api.request.CheckAwardedBadges
import ink.abb.pogo.api.request.GetInventory
import ink.abb.pogo.api.request.LevelUpRewards
import ink.abb.pogo.scraper.*
import ink.abb.pogo.scraper.util.Log
import java.text.DecimalFormat
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class UpdateProfile : Task {
    var lastLevelCheck: Int = 0

    override fun run(bot: Bot, ctx: Context, settings: Settings) {
        bot.api.queueRequest(GetInventory().withLastTimestampMs(0)).subscribe {
            val curLevelXP = bot.api.inventory.playerStats.experience - requiredXp[bot.api.inventory.playerStats.level - 1]
            val nextXP = if (bot.api.inventory.playerStats.level == requiredXp.size) {
                curLevelXP
            } else {
                (requiredXp[bot.api.inventory.playerStats.level] - requiredXp[bot.api.inventory.playerStats.level - 1]).toLong()
            }
            val ratio = DecimalFormat("#0.00").format(curLevelXP.toDouble() / nextXP.toDouble() * 100.0)
            val timeDiff = ChronoUnit.MINUTES.between(ctx.startTime, LocalDateTime.now())
            val xpPerHour: Long = if (timeDiff != 0L) {
                (bot.api.inventory.playerStats.experience - ctx.startXp.get()) / timeDiff * 60
            } else {
                0
            }
            val nextLevel: String = if (xpPerHour != 0L) {
                "${DecimalFormat("#0").format((nextXP.toDouble() - curLevelXP.toDouble()) / xpPerHour.toDouble())}h${Math.round(((nextXP.toDouble() - curLevelXP.toDouble()) / xpPerHour.toDouble()) % 1 * 60)}m"
            } else {
                "Unknown"
            }

            Log.magenta("Profile update: ${bot.api.inventory.playerStats.experience} XP on LVL ${bot.api.inventory.playerStats.level}; $curLevelXP/$nextXP ($ratio%) to LVL ${bot.api.inventory.playerStats.level + 1}")
            Log.magenta("XP gain: ${NumberFormat.getInstance().format(bot.api.inventory.playerStats.experience - ctx.startXp.get())} XP in ${ChronoUnit.MINUTES.between(ctx.startTime, LocalDateTime.now())} mins; " +
                    "XP rate: ${NumberFormat.getInstance().format(xpPerHour)}/hr; Next level in: $nextLevel")
            Log.magenta("Pokemon caught/transferred: ${ctx.pokemonStats.first.get()}/${ctx.pokemonStats.second.get()}; " +
                    "Pokemon caught from lures: ${ctx.luredPokemonStats.get()}; " +
                    "Items caught/dropped: ${ctx.itemStats.first.get()}/${ctx.itemStats.second.get()};")
            Log.magenta("Pokebank ${bot.api.inventory.pokemon.size + bot.api.inventory.eggs.size}/${bot.api.playerData.maxPokemonStorage}; " +
                    "Stardust ${bot.api.inventory.currencies.getOrPut("STARDUST", { AtomicInteger(0) }).get()}; " +
                    "Inventory ${bot.api.inventory.size}/${bot.api.playerData.maxItemStorage}"
            )
            if (bot.api.inventory.pokemon.size + bot.api.inventory.eggs.size < bot.api.playerData.maxPokemonStorage && ctx.pokemonInventoryFullStatus.get())
                ctx.pokemonInventoryFullStatus.set(false)
            else if (bot.api.inventory.pokemon.size + bot.api.inventory.eggs.size >= bot.api.playerData.maxPokemonStorage && !ctx.pokemonInventoryFullStatus.get())
                ctx.pokemonInventoryFullStatus.set(true)

            if (settings.catchPokemon && ctx.pokemonInventoryFullStatus.get())
                Log.red("Pokemon inventory is full, not catching!")

            ctx.server.sendProfile()
        }

        for (i in (lastLevelCheck + 1)..bot.api.inventory.playerStats.level) {
            //Log.magenta("Accepting rewards for level $i...")
            bot.api.queueRequest(LevelUpRewards().withLevel(i)).subscribe {
                val result = it.response
                if (result.result == LevelUpRewardsResponseOuterClass.LevelUpRewardsResponse.Result.AWARDED_ALREADY) {
                    if (i > lastLevelCheck) {
                        //Log.magenta("Already accepted awards for level ${i}, updating $lastLevelCheck = $i")
                        lastLevelCheck = i
                    }
                    return@subscribe
                }

                var message = "Accepting rewards for level $i"

                val sb_rewards = StringJoiner(", ")
                for (reward in result.itemsAwardedList) {
                    sb_rewards.add("${reward.itemCount}x ${reward.itemId.name}")
                }
                message += "; Rewards: [$sb_rewards]"

                if (result.itemsUnlockedCount > 0) {
                    val sb_unlocks = StringJoiner(", ")
                    for (item in result.itemsUnlockedList) {
                        sb_unlocks.add("${item.name}")
                    }
                    message += "; Unlocks: [$sb_unlocks]"
                }

                Log.magenta(message)

                if (i > lastLevelCheck) {
                    lastLevelCheck = i
                }
            }
        }

        bot.api.queueRequest(CheckAwardedBadges()).subscribe {
            val result = it.response
            result.awardedBadgesList.forEach {
                // TODO: Does not work?!
                /*bot.api.queueRequest(EquipBadge().withBadgeType(it)).subscribe {
                    println(it.response.toString())
                }*/
            }
        }
    }
}
