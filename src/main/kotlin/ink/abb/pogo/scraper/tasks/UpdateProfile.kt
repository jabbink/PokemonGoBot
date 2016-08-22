/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper.tasks

import com.pokegoapi.api.player.PlayerLevelUpRewards
import com.pokegoapi.api.player.PlayerProfile
import ink.abb.pogo.scraper.*
import ink.abb.pogo.scraper.util.Log
import ink.abb.pogo.scraper.util.cachedInventories
import ink.abb.pogo.scraper.util.inventory.size
import java.text.DecimalFormat
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*

class UpdateProfile : Task {
    var lastLevelCheck: Int = 1

    override fun run(bot: Bot, ctx: Context, settings: Settings) {
        val player = ctx.api.playerProfile
        ctx.api.inventories.updateInventories(true)

        for (i in lastLevelCheck..player.stats.level)
        {
            val msg = ctx.api.playerProfile.acceptLevelUpRewards(i)

            if (msg.status == PlayerLevelUpRewards.Status.ALREADY_ACCEPTED)
            {
                continue
            }

            var message = "Accepting rewards for level $i"

            val sb_rewards = StringJoiner(", ")
            for (reward in msg.rewards) {
                sb_rewards.add("${reward.itemCount}x ${reward.itemId.name}")
            }
            message += "; Rewards: [$sb_rewards]"

            if (msg.unlockedItems.size > 0) {
                val sb_unlocks = StringJoiner(", ")
                for (item in msg.unlockedItems) {
                    sb_unlocks.add("${item.name}")
                }
                message += "; Unlocks: [$sb_unlocks]"
            }

            Log.magenta(message)

            lastLevelCheck = i

            if (lastLevelCheck != player.stats.level) Thread.sleep(500)
        }

        // No messages to show? Booo!
        try {
            ctx.api.playerProfile.checkAndEquipBadges()
        } catch (e: Exception) {
            Log.red("Failed to check and equip badges")
        }

        try {
            // update km walked, mainly
            val inventories = ctx.api.cachedInventories
            player.updateProfile()
            val curLevelXP = player.stats.experience - requiredXp[player.stats.level - 1]
            val nextXP = if (player.stats.level == requiredXp.size) {
                curLevelXP
            } else {
                (requiredXp[player.stats.level] - requiredXp[player.stats.level - 1]).toLong()
            }
            val ratio = DecimalFormat("#0.00").format(curLevelXP.toDouble() / nextXP.toDouble() * 100.0)
            val timeDiff = ChronoUnit.MINUTES.between(ctx.startTime, LocalDateTime.now())
            val xpPerHour: Long = if (timeDiff != 0L) {
                (player.stats.experience - ctx.startXp.get()) / timeDiff * 60
            } else {
                0
            }
            val nextLevel: String = if (xpPerHour != 0L) {
				"${DecimalFormat("#0").format((nextXP.toDouble() - curLevelXP.toDouble()) / xpPerHour.toDouble())}h${Math.round(((nextXP.toDouble() - curLevelXP.toDouble()) / xpPerHour.toDouble())%1*60)}m"
            } else {
                "Unknown"
            }

            Log.magenta("Profile update: ${player.stats.experience} XP on LVL ${player.stats.level}; $curLevelXP/$nextXP ($ratio%) to LVL ${player.stats.level + 1}")
            Log.magenta("XP gain: ${NumberFormat.getInstance().format(player.stats.experience - ctx.startXp.get())} XP in ${ChronoUnit.MINUTES.between(ctx.startTime, LocalDateTime.now())} mins; " +
                    "XP rate: ${NumberFormat.getInstance().format(xpPerHour)}/hr; Next level in: $nextLevel")
            Log.magenta("Pokemon caught/transferred: ${ctx.pokemonStats.first.get()}/${ctx.pokemonStats.second.get()}; " +
                    "Pokemon caught from lures: ${ctx.luredPokemonStats.get()}; " +
                    "Items caught/dropped: ${ctx.itemStats.first.get()}/${ctx.itemStats.second.get()};")
            Log.magenta("Pokebank ${inventories.pokebank.pokemons.size + inventories.hatchery.eggs.size}/${ctx.profile.playerData.maxPokemonStorage}; " +
                    "Stardust ${ctx.profile.currencies[PlayerProfile.Currency.STARDUST]}; " +
                    "Inventory ${inventories.itemBag.size()}/${ctx.profile.playerData.maxItemStorage}"
            )
            if (inventories.pokebank.pokemons.size + inventories.hatchery.eggs.size < ctx.profile.playerData.maxPokemonStorage && ctx.pokemonInventoryFullStatus.get())
                ctx.pokemonInventoryFullStatus.set(false)
            else if (inventories.pokebank.pokemons.size + inventories.hatchery.eggs.size >= ctx.profile.playerData.maxPokemonStorage && !ctx.pokemonInventoryFullStatus.get())
                ctx.pokemonInventoryFullStatus.set(true)

            if (settings.catchPokemon && ctx.pokemonInventoryFullStatus.get())
                Log.red("Pokemon inventory is full, not catching!")

            ctx.server.sendProfile()
        } catch (e: Exception) {
            Log.red("Failed to update profile and inventories")
        }
    }
}
