/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper.tasks

import POGOProtos.Enums.PokemonIdOuterClass
import POGOProtos.Enums.PokemonMoveOuterClass
import com.pokegoapi.api.pokemon.Pokemon
import com.pokegoapi.api.player.PlayerProfile
import com.pokegoapi.api.pokemon.PokemonMetaRegistry
import com.pokegoapi.api.pokemon.PokemonMoveMetaRegistry
import ink.abb.pogo.scraper.Bot
import ink.abb.pogo.scraper.Context
import ink.abb.pogo.scraper.Settings
import ink.abb.pogo.scraper.util.inventory.size
import ink.abb.pogo.scraper.Task
import ink.abb.pogo.scraper.util.Log
import ink.abb.pogo.scraper.util.pokemon.getIv
import ink.abb.pogo.scraper.util.pokemon.getIvPercentage
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
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

        try {
            val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            val dateNow = Date()

            // UTF-8 with BOM to fix borked UTF-8 chars in MS Excel (for nickname output)
            // https://en.wikipedia.org/wiki/Byte_order_mark#UTF-8
            val os = FileOutputStream("export.csv")
            os.write(239)
            os.write(187)
            os.write(191)
            val writer = PrintWriter(OutputStreamWriter(os, "UTF-8"))

            // Output player information
            writer.println("Name,${ctx.profile.username}")
            writer.println("Team,${ctx.profile.team}")
            writer.println("Pokecoin,${ctx.profile.currencies.get(PlayerProfile.Currency.POKECOIN)}")
            writer.println("Stardust,${ctx.profile.currencies.get(PlayerProfile.Currency.STARDUST)}")
            writer.println("Level,${ctx.profile.stats.level}")
            writer.println("Experience,${ctx.profile.stats.experience}")
            writer.println("Pokebank,${ctx.api.inventories.pokebank.pokemons.size + ctx.api.inventories.hatchery.eggs.size}/${ctx.profile.pokemonStorage}")
            writer.println("Inventory,${ctx.api.inventories.itemBag.size()}/${ctx.profile.itemStorage}")
            writer.println("Last update,${dateFormatter.format(dateNow)}")
            writer.println()

            // Output Pokebank
            writer.println("Pokebank overview")
            writer.println("Number,Name,Nickname,Favorite?,CP,IV [%],Stamina (HP),Max Stamina (HP),Class,Type,Move 1,Move 1 Power,Move 1 Accuracy,Move 1 Crit Chance,Move 1 Time,Move 1 Energy,Move 2,Move 2 Power,Move 2 Accuracy,Move 2 Crit Chance,Move 2 Time,Move 2 Energy,iStamina,iAttack,iDefense,cpMultiplier,Height [m],Weight [kg],Candy,Candies to evolve,Candy costs for powerup,Stardust costs for powerup,Creation Time,Base Capture Rate,Base Flee Rate,Battles Attacked,Battles Defended,Injured?,Fainted?,Level,CP after powerup,Max CP,ID")

            ctx.api.inventories.pokebank.pokemons.sortedWith(compareName.thenComparing(compareIv)).map {
                val date = Date(it.creationTimeMs)
                dateFormatter.format(date)

                val pmeta = PokemonMetaRegistry.getMeta(PokemonIdOuterClass.PokemonId.forNumber(it.pokemonId.number))
                val pmmeta1 = PokemonMoveMetaRegistry.getMeta(PokemonMoveOuterClass.PokemonMove.forNumber(it.move1.number))
                val pmmeta2 = PokemonMoveMetaRegistry.getMeta(PokemonMoveOuterClass.PokemonMove.forNumber(it.move2.number))

                "${it.pokemonId.number},${it.pokemonId.name},\"${it.nickname}\",${it.isFavorite},${it.cp},${it.getIvPercentage()},${it.stamina},${it.maxStamina},${pmeta.pokemonClass.name},${pmeta.type1.name}/${pmeta.type2.name},${it.move1.name},${pmmeta1.power},${pmmeta1.accuracy},${pmmeta1.critChance},${pmmeta1.time},${pmmeta1.energy},${it.move2.name},${pmmeta2.power},${pmmeta2.accuracy},${pmmeta2.critChance},${pmmeta2.time},${pmmeta2.energy},${it.individualStamina},${it.individualAttack},${it.individualDefense},${it.cpMultiplier},${it.heightM},${it.weightKg},${it.candy},${it.candiesToEvolve},${it.candyCostsForPowerup},${it.stardustCostsForPowerup},${dateFormatter.format(date)},${it.baseCaptureRate},${it.baseFleeRate},${it.battlesAttacked},${it.battlesDefended},${it.isInjured},${it.isFainted},${it.level},${it.cpAfterPowerup},${it.maxCp},${it.id}"
            }.forEach { writer.println(it) }

            Log.normal("Wrote export CSV.")

            writer.close()
            os.close()
        } catch (e: Exception) {
            Log.red("Error writing export CSV: " + e.message)
        }
    }
}