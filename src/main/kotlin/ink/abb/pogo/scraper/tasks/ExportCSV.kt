/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper.tasks

import com.pokegoapi.api.pokemon.Pokemon
import com.pokegoapi.api.player.PlayerProfile
import ink.abb.pogo.scraper.Bot
import ink.abb.pogo.scraper.Context
import ink.abb.pogo.scraper.Settings
import ink.abb.pogo.scraper.util.inventory.size
import ink.abb.pogo.scraper.Task
import ink.abb.pogo.scraper.util.Log
import ink.abb.pogo.scraper.util.pokemon.getIv
import ink.abb.pogo.scraper.util.pokemon.getIvPercentage
import ink.abb.pogo.scraper.util.pokemon.getStatsFormatted
import java.io.PrintWriter;
import java.util.*

class ExportCSV : Task {
    override fun run(bot: Bot, ctx: Context, settings: Settings) {
        val compareName = Comparator<Pokemon> { a, b ->
            a.pokemonId.name.compareTo(b.pokemonId.name)
        }
        val compareIv = Comparator<Pokemon> { a, b ->
            // compare b to a to get it descending
            if (settings.sortByIV) {
                b.getIv().compareTo(a.getIv())
            } else {
                b.cp.compareTo(a.cp)
            }
        }

        val writer = PrintWriter("export.csv", "UTF-8")

        writer.println("\"Name\",\"${ctx.profile.username}\"")
        writer.println("\"Team\",\"${ctx.profile.team}\"")
        writer.println("\"Pokecoin\",\"${ctx.profile.currencies.get(PlayerProfile.Currency.POKECOIN)}\"")
        writer.println("\"Stardust\",\"${ctx.profile.currencies.get(PlayerProfile.Currency.STARDUST)}\"")
        writer.println("\"Level\",\"${ctx.profile.stats.level}\"")
        writer.println("\"Experience\",\"${ctx.profile.stats.experience}\"")
        writer.println("\"Pokebank\",\"${ctx.api.inventories.pokebank.pokemons.size + ctx.api.inventories.hatchery.eggs.size}/${ctx.profile.pokemonStorage}\"")
        writer.println("\"Inventory\",\"${ctx.api.inventories.itemBag.size()}/${ctx.profile.itemStorage}\"")
        writer.println()

        // Pokebank
/*
        [x] Pokemon Name
        [ ] Pokemon Id (type)
        [ ] Favorited Boolean
        [x] CP
        [x] IV
        [ ] Stamina (HP)
        [ ] Move 1
        [ ] Move 2
        [ ] iAttack
        [ ] iDefense
        [ ] iStamina
        [ ] cpMultiplier
        [ ] Location acquired
        [ ] Height
        [ ] Weight
*/
        writer.println("\"Pokebank overview\"")
        writer.println("\"Name\",\"Nickname\",\"CP\",\"IV\",\"Stats\"")

        ctx.api.inventories.pokebank.pokemons.sortedWith(compareName.thenComparing(compareIv)).map {
            "\"${it.pokemonId.name}\",\"${it.nickname}\",\"${it.cp}\",\"${it.getIvPercentage()}\",\"${it.getStatsFormatted()}\""
        }.forEach { writer.println(it) }

        Log.normal("Wrote export CSV");

        writer.close()
    }
}