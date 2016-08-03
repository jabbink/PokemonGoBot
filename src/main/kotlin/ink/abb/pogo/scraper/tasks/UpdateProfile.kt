/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper.tasks

import com.pokegoapi.api.player.PlayerProfile
import ink.abb.pogo.scraper.Bot
import ink.abb.pogo.scraper.Context
import ink.abb.pogo.scraper.Settings
import ink.abb.pogo.scraper.Task
import ink.abb.pogo.scraper.util.Log
import ink.abb.pogo.scraper.util.inventory.size
import java.text.DecimalFormat
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class UpdateProfile : Task {

    val requiredXp = arrayOf(0, 1000, 3000, 6000, 10000, 15000, 21000, 28000, 36000, 45000, 55000, 65000, 75000,
            85000, 100000, 120000, 140000, 160000, 185000, 210000, 260000, 335000, 435000, 560000, 710000, 900000, 1100000,
            1350000, 1650000, 2000000, 2500000, 3000000, 3750000, 4750000, 6000000, 7500000, 9500000, 12000000, 15000000, 20000000)

    override fun run(bot: Bot, ctx: Context, settings: Settings) {
        val player = ctx.api.playerProfile
        val inventories = ctx.api.inventories
        try {
            // update km walked, mainly
            inventories.updateInventories(true)
            player.updateProfile()
            val nextXP = requiredXp[player.stats.level] - requiredXp[player.stats.level - 1]
            val curLevelXP = player.stats.experience - requiredXp[player.stats.level - 1]
            val ratio = DecimalFormat("#0.00").format(curLevelXP.toDouble() / nextXP.toDouble() * 100.0)

            val xpPerHour: Long = (player.stats.experience - ctx.startXp.get() / ChronoUnit.MINUTES.between(ctx.startTime, LocalDateTime.now())) * 60

            Log.magenta("Profile update: ${NumberFormat.getInstance().format(player.stats.experience)} XP on LVL ${player.stats.level}; $curLevelXP/$nextXP ($ratio%) to LVL ${player.stats.level + 1}; " +
                    "XP gain: ${NumberFormat.getInstance().format(player.stats.experience - ctx.startXp.get())} XP in ${ChronoUnit.MINUTES.between(ctx.startTime, LocalDateTime.now())} mins; " +
                    "XP/Hour: ${NumberFormat.getInstance().format(xpPerHour)}")
            Log.magenta(
                    "Pokemon caught/transferred: ${ctx.pokemonStats.first.get()}/${ctx.pokemonStats.second.get()}; " +
                    "Pokebank ${ctx.api.inventories.pokebank.pokemons.size + ctx.api.inventories.hatchery.eggs.size}/${ctx.profile.playerData.maxPokemonStorage}; " +
                    "Stardust ${ctx.profile.currencies[PlayerProfile.Currency.STARDUST]}; " +
                    "Items caught/dropped: ${ctx.itemStats.first.get()}/${ctx.itemStats.second.get()}; " +
                    "Inventory ${ctx.api.inventories.itemBag.size()}/${ctx.profile.playerData.maxItemStorage}"

            )
            ctx.server.sendProfile()
        } catch (e: Exception) {
            Log.red("Failed to update profile and inventories")
        }
    }
}
