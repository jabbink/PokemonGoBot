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
import com.pokegoapi.api.player.PlayerProfile
import com.pokegoapi.api.pokemon.Pokemon
import com.pokegoapi.api.pokemon.PokemonMetaRegistry
import com.pokegoapi.api.pokemon.PokemonMoveMetaRegistry
import ink.abb.pogo.scraper.Bot
import ink.abb.pogo.scraper.Context
import ink.abb.pogo.scraper.Settings
import ink.abb.pogo.scraper.Task
import ink.abb.pogo.scraper.util.Log
import ink.abb.pogo.scraper.util.inventory.size
import ink.abb.pogo.scraper.util.io.CSVWriter
import ink.abb.pogo.scraper.util.pokemon.getIv
import ink.abb.pogo.scraper.util.pokemon.getIvPercentage
import java.text.SimpleDateFormat
import java.util.*

class Export : Task {
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
            val output = ArrayList<Array<String>>()

            // Output player information
            output.add(arrayOf("Name", ctx.profile.playerData.username))
            output.add(arrayOf("Team", ctx.profile.playerData.team.name))
            output.add(arrayOf("Pokecoin", "${ctx.profile.currencies.get(PlayerProfile.Currency.POKECOIN)}"))
            output.add(arrayOf("Stardust", "${ctx.profile.currencies.get(PlayerProfile.Currency.STARDUST)}"))
            output.add(arrayOf("Level", "${ctx.profile.stats.level}"))
            output.add(arrayOf("Experience", "${ctx.profile.stats.experience}", "${ctx.profile.stats.nextLevelXp}"))
            output.add(arrayOf("Km walked", ds("${ctx.profile.stats.kmWalked}", settings)))
            output.add(arrayOf("Pokemons Encountered", "${ctx.profile.stats.pokemonsEncountered}"))
            output.add(arrayOf("Pokemons Captured", "${ctx.profile.stats.pokemonsCaptured}"))
            output.add(arrayOf("Unique Pokedex Entries", "${ctx.profile.stats.uniquePokedexEntries}"))
            output.add(arrayOf("Evolutions", "${ctx.profile.stats.evolutions}"))
            output.add(arrayOf("Pokestop Visits", "${ctx.profile.stats.pokeStopVisits}"))
            output.add(arrayOf("Pokeballs Thrown", "${ctx.profile.stats.pokeballsThrown}"))
            output.add(arrayOf("Eggs Hatched", "${ctx.profile.stats.eggsHatched}"))
            output.add(arrayOf("Battle Attack Won", "${ctx.profile.stats.battleAttackWon}"))
            output.add(arrayOf("Battle Attack Total", "${ctx.profile.stats.battleAttackTotal}"))
            output.add(arrayOf("Battle Defended Won", "${ctx.profile.stats.battleDefendedWon}"))
            output.add(arrayOf("Battle Training Won", "${ctx.profile.stats.battleTrainingTotal}"))
            output.add(arrayOf("Battle Training Total", "${ctx.profile.stats.battleTrainingTotal}"))
            output.add(arrayOf("Prestige Raised Total", "${ctx.profile.stats.prestigeRaisedTotal}"))
            output.add(arrayOf("Prestige Dropped Total", "${ctx.profile.stats.prestigeDroppedTotal}"))
            output.add(arrayOf("Pokemon Deployed", "${ctx.profile.stats.pokemonDeployed}"))
            output.add(arrayOf("Pokebank", "${ctx.api.inventories.pokebank.pokemons.size + ctx.api.inventories.hatchery.eggs.size}", "${ctx.profile.playerData.maxPokemonStorage}"))
            output.add(arrayOf("Inventory", "${ctx.api.inventories.itemBag.size()}", "${ctx.profile.playerData.maxItemStorage}"))
            output.add(arrayOf("Last Update", dateFormatter.format(dateNow)))
            output.add(arrayOf(""))

            // Output Pokebank
            output.add(arrayOf("Pokebank overview"))
            output.add(arrayOf(
                "Number", "Name", "Nickname", "Favorite?", "CP", "IV [%]", "Stamina (HP)", "Max Stamina (HP)", "Class",
                "Type", "Move 1", "Move 1 Type", "Move 1 Power", "Move 1 Accuracy", "Move 1 Crit Chance", "Move 1 Time",
                "Move 1 Energy", "Move 2", "Move 2 Type", "Move 2 Power", "Move 2 Accuracy", "Move 2 Crit Chance",
                "Move 2 Time", "Move 2 Energy", "iStamina", "iAttack", "iDefense", "cpMultiplier", "Height [m]", "Weight [kg]",
                "Candy", "Candies to evolve", "Candy costs for powerup", "Stardust costs for powerup", "Creation Time",
                "Base Capture Rate", "Base Flee Rate", "Battles Attacked", "Battles Defended", "Injured?", "Fainted?",
                "Level", "CP after powerup", "Max CP", "ID"))

            ctx.api.inventories.pokebank.pokemons.sortedWith(compareName.thenComparing(compareIv)).map {
                val date = Date(it.creationTimeMs)
                dateFormatter.format(date)

                val pmeta = PokemonMetaRegistry.getMeta(PokemonIdOuterClass.PokemonId.forNumber(it.pokemonId.number))
                val pmmeta1 = PokemonMoveMetaRegistry.getMeta(PokemonMoveOuterClass.PokemonMove.forNumber(it.move1.number))
                val pmmeta2 = PokemonMoveMetaRegistry.getMeta(PokemonMoveOuterClass.PokemonMove.forNumber(it.move2.number))

                arrayOf(
                    "${it.pokemonId.number}", "${it.pokemonId.name}", "${it.nickname}", "${it.isFavorite}", "${it.cp}",
                    "${it.getIvPercentage()}", "${it.stamina}", "${it.maxStamina}", "${pmeta.pokemonClass.name}",
                    formatType(pmeta.type1.name, pmeta.type2.name), "${it.move1.name}", "${pmmeta1.type.name}",
                    "${pmmeta1.power}", "${pmmeta1.accuracy}", ds("${pmmeta1.critChance}", settings), "${pmmeta1.time}",
                    "${pmmeta1.energy}", "${it.move2.name}", "${pmmeta2.type.name}", "${pmmeta2.power}", "${pmmeta2.accuracy}",
                    ds("${pmmeta2.critChance}", settings), "${pmmeta2.time}", "${pmmeta2.energy}", "${it.individualStamina}",
                    "${it.individualAttack}", "${it.individualDefense}", ds("${it.cpMultiplier}", settings), ds("${it.heightM}",
                    settings), ds("${it.weightKg}", settings), "${it.candy}", "${it.candiesToEvolve}", "${it.candyCostsForPowerup}",
                    "${it.stardustCostsForPowerup}", "${dateFormatter.format(date)}", ds("${it.baseCaptureRate}", settings),
                    ds("${it.baseFleeRate}", settings), "${it.battlesAttacked}", "${it.battlesDefended}", "${it.isInjured}",
                    "${it.isFainted}", ds("${it.level}", settings), "${it.cpAfterPowerup}", "${it.maxCp}", "${it.id}")
            }.forEach { output.add(it) }

            when (settings.export) {
                "CSV" -> {
                    val writer = CSVWriter()
                    writer.write(output)
                }
                "DSV" -> {
                    val writer = CSVWriter(";")
                    writer.write(output)
                }
                else -> {
                    Log.red("Invalid export configuration!")
                }
            }

            Log.normal("Wrote export.")
        } catch (e: Exception) {
            Log.red("Error writing export: " + e.message)
        }
    }

    // Detect if the "float" fields need to be forced to use "," instead of "." (DSV export)
    private fun ds(string: String, settings: Settings): String {
        var result = string
        if (settings.export.equals("DSV"))
        {
            result = result.replace(".", ",")
        }

        return result
    }

    // Don't concat 2nd pokemon type "NONE"
    private fun formatType(type1: String, type2: String): String {
        if (type2.equals("NONE")) return type1
        return type1 + "/" + type2
    }
}