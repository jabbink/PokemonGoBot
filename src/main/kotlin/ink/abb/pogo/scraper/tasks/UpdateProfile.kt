/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper.tasks

import com.pokegoapi.api.player.PlayerProfile
import ink.abb.pogo.scraper.*
import ink.abb.pogo.scraper.util.Log
import ink.abb.pogo.scraper.util.inventory.size
import java.text.DecimalFormat

class UpdateProfile : Task {
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
            Log.magenta("Profile update: ${player.stats.experience} XP on LVL ${player.stats.level}; $curLevelXP/$nextXP ($ratio%) to LVL ${player.stats.level + 1}")
            Log.magenta("XP gain: ${player.stats.experience - ctx.startXp.get()} XP; " +
                    "Pokemon caught/transferred: ${ctx.pokemonStats.first.get()}/${ctx.pokemonStats.second.get()}; " +
                    "Pokemon caught from lures: ${ctx.luredPokemonStats.get()}; " +
                    "Items caught/dropped: ${ctx.itemStats.first.get()}/${ctx.itemStats.second.get()};\r\n" +
                    "Pokebank ${ctx.api.inventories.pokebank.pokemons.size + ctx.api.inventories.hatchery.eggs.size}/${ctx.profile.playerData.maxPokemonStorage}; " +
                    "Stardust ${ctx.profile.currencies[PlayerProfile.Currency.STARDUST]}; " +
                    "Inventory ${ctx.api.inventories.itemBag.size()}/${ctx.profile.playerData.maxItemStorage}"

            )
            ctx.server.sendProfile()
        } catch (e: Exception) {
            Log.red("Failed to update profile and inventories")
        }
    }
}
