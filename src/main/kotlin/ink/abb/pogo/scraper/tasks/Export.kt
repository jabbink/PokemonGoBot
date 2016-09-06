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
import com.pokegoapi.google.common.geometry.S2CellId
import com.pokegoapi.google.common.geometry.S2LatLng
import ink.abb.pogo.scraper.Bot
import ink.abb.pogo.scraper.Context
import ink.abb.pogo.scraper.Settings
import ink.abb.pogo.scraper.Task
import ink.abb.pogo.scraper.util.Log
import ink.abb.pogo.scraper.util.cachedInventories
import ink.abb.pogo.scraper.util.inventory.size
import ink.abb.pogo.scraper.util.io.ExportCSVWriter
import ink.abb.pogo.scraper.util.io.ExportJSONWriter
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
            if (settings.sortByIv) {
                b.getIv().compareTo(a.getIv())
            } else {
                b.cp.compareTo(a.cp)
            }
        }

        try {
            val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

            // Output player information
            val profile: Map<String, String> = mapOf(
                    Pair("Name", ctx.profile.playerData.username),
                    Pair("Team", ctx.profile.playerData.team.name),
                    Pair("Pokecoin", "${ctx.profile.currencies.get(PlayerProfile.Currency.POKECOIN)}"),
                    Pair("Stardust", "${ctx.profile.currencies.get(PlayerProfile.Currency.STARDUST)}"),
                    Pair("Level", "${ctx.profile.stats.level}"),
                    Pair("Experience", "${ctx.profile.stats.experience}"),
                    Pair("Previous Level Experience", "${ctx.profile.stats.prevLevelXp}"),
                    Pair("Next Level Experience", "${ctx.profile.stats.nextLevelXp}"),
                    Pair("Km walked", ds("${ctx.profile.stats.kmWalked}", settings)),
                    Pair("Pokemons Encountered", "${ctx.profile.stats.pokemonsEncountered}"),
                    Pair("Pokemons Captured", "${ctx.profile.stats.pokemonsCaptured}"),
                    Pair("Unique Pokedex Entries", "${ctx.profile.stats.uniquePokedexEntries}"),
                    Pair("Evolutions", "${ctx.profile.stats.evolutions}"),
                    Pair("Pokestop Visits", "${ctx.profile.stats.pokeStopVisits}"),
                    Pair("Pokeballs Thrown", "${ctx.profile.stats.pokeballsThrown}"),
                    Pair("Eggs Hatched", "${ctx.profile.stats.eggsHatched}"),
                    Pair("Battle Attack Won", "${ctx.profile.stats.battleAttackWon}"),
                    Pair("Battle Attack Total", "${ctx.profile.stats.battleAttackTotal}"),
                    Pair("Battle Defended Won", "${ctx.profile.stats.battleDefendedWon}"),
                    Pair("Battle Training Won", "${ctx.profile.stats.battleTrainingTotal}"),
                    Pair("Battle Training Total", "${ctx.profile.stats.battleTrainingTotal}"),
                    Pair("Prestige Raised Total", "${ctx.profile.stats.prestigeRaisedTotal}"),
                    Pair("Prestige Dropped Total", "${ctx.profile.stats.prestigeDroppedTotal}"),
                    Pair("Pokemon Deployed", "${ctx.profile.stats.pokemonDeployed}"),
                    Pair("Pokebank Size", "${ctx.api.cachedInventories.pokebank.pokemons.size + ctx.api.cachedInventories.hatchery.eggs.size}"),
                    Pair("Maximum Pokebank Storage", "${ctx.profile.playerData.maxPokemonStorage}"),
                    Pair("Inventory Size", "${ctx.api.cachedInventories.itemBag.size()}"),
                    Pair("Maximum Inventory Storage", "${ctx.profile.playerData.maxItemStorage}"),
                    Pair("Last Update", dateFormatter.format(Date())),
                    Pair("Location Latitude", ds("${ctx.lat.get()}", settings)),
                    Pair("Location Longitude", ds("${ctx.lng.get()}", settings))
            )

            // Output Eggs
            val eggs = ArrayList<Map<String, String>>()
            for (egg in ctx.api.cachedInventories.hatchery.eggs)
            {
                val latLng = S2LatLng(S2CellId(egg.capturedCellId).toPoint())

                eggs.add(mapOf(
                        Pair("Walked [km]", ds("${egg.eggKmWalked}", settings)),
                        Pair("Target [km]", ds("${egg.eggKmWalkedTarget}", settings)),
                        Pair("Incubated?", "${egg.isIncubate}"),
                        Pair("Found", dateFormatter.format(Date(egg.creationTimeMs))),
                        Pair("Location Latitude", ds("${latLng.latDegrees()}", settings)),
                        Pair("Location Longitude", ds("${latLng.lngDegrees()}", settings))
                ))
            }

            // Output Items
            val items = ArrayList<Map<String, String>>()
            for (item in ctx.api.cachedInventories.itemBag.items) {
                items.add(mapOf(
                        Pair("Item", item.itemId.name),
                        Pair("Count", "${item.count}")
                ))
            }

            // Output Pokebank
            val pokemons = ArrayList<Map<String, String>>()

            ctx.api.cachedInventories.pokebank.pokemons.sortedWith(compareName.thenComparing(compareIv)).map {
                val latLng = S2LatLng(S2CellId(it.capturedS2CellId).toPoint())

                val pmeta = PokemonMetaRegistry.getMeta(PokemonIdOuterClass.PokemonId.forNumber(it.pokemonId.number))
                val pmmeta1 = PokemonMoveMetaRegistry.getMeta(PokemonMoveOuterClass.PokemonMove.forNumber(it.move1.number))
                val pmmeta2 = PokemonMoveMetaRegistry.getMeta(PokemonMoveOuterClass.PokemonMove.forNumber(it.move2.number))

                mapOf(
                        Pair("Number", "${it.pokemonId.number}"),
                        Pair("Name", it.pokemonId.name),
                        Pair("Nickname", it.nickname),
                        Pair("Favorite?", "${it.isFavorite}"),
                        Pair("CP", "${it.cp}"),
                        Pair("IV [%]", "${it.getIvPercentage()}"),
                        Pair("Stamina (HP)", "${it.stamina}"),
                        Pair("Max Stamina (HP)", "${it.maxStamina}"),
                        Pair("Class", pmeta.pokemonClass.name),
                        Pair("Type", formatType(pmeta.type1.name, pmeta.type2.name)),
                        Pair("Move 1", it.move1.name),
                        Pair("Move 1 Type", pmmeta1.type.name),
                        Pair("Move 1 Power", "${pmmeta1.power}"),
                        Pair("Move 1 Accuracy", "${pmmeta1.accuracy}"),
                        Pair("Move 1 Crit Chance", ds("${pmmeta1.critChance}", settings)),
                        Pair("Move 1 Time", "${pmmeta1.time}"),
                        Pair("Move 1 Energy", "${pmmeta1.energy}"),
                        Pair("Move 2", it.move2.name),
                        Pair("Move 2 Type", pmmeta2.type.name),
                        Pair("Move 2 Power", "${pmmeta2.power}"),
                        Pair("Move 2 Accuracy", "${pmmeta2.accuracy}"),
                        Pair("Move 2 Crit Chance", ds("${pmmeta2.critChance}", settings)),
                        Pair("Move 2 Time", "${pmmeta2.time}"),
                        Pair("Move 2 Energy", "${pmmeta2.energy}"),
                        Pair("iStamina", "${it.individualStamina}"),
                        Pair("iAttack", "${it.individualAttack}"),
                        Pair("iDefense", "${it.individualDefense}"),
                        Pair("cpMultiplier", ds("${it.cpMultiplier}", settings)),
                        Pair("Height [m]", ds("${it.heightM}", settings)),
                        Pair("Weight [kg]", ds("${it.weightKg}", settings)),
                        Pair("Candy", "${it.candy}"),
                        Pair("Candies to evolve", "${it.candiesToEvolve}"),
                        Pair("Candy costs for powerup", "${it.candyCostsForPowerup}"),
                        Pair("Stardust costs for powerup", "${it.stardustCostsForPowerup}"),
                        Pair("Found", dateFormatter.format(Date(it.creationTimeMs))),
                        Pair("Found Latitude", ds("${latLng.latDegrees()}", settings)),
                        Pair("Found Longitude", ds("${latLng.lngDegrees()}", settings)),
                        Pair("Base Capture Rate", ds("${it.baseCaptureRate}", settings)),
                        Pair("Base Flee Rate", ds("${it.baseFleeRate}", settings)),
                        Pair("Battles Attacked", "${it.battlesAttacked}"),
                        Pair("Battles Defended", "${it.battlesDefended}"),
                        Pair("Injured?", "${it.isInjured}"),
                        Pair("Fainted?", "${it.isFainted}"),
                        Pair("Level", ds("${it.level}", settings)),
                        Pair("CP after powerup", "${it.cpAfterPowerup}"),
                        Pair("Max CP", "${it.maxCp}"),
                        Pair("ID", "${it.id}")
                )
            }.forEach { pokemons.add(it) }

            when (settings.export) {
                "CSV" -> {
                    val filename = "export_${settings.name}.csv"
                    val writer = ExportCSVWriter(filename)
                    writer.write(profile, eggs, items, pokemons)

                    Log.normal("Wrote export $filename.")
                }
                "DSV" -> {
                    val filename = "export_${settings.name}.csv"
                    val writer = ExportCSVWriter(filename, ";")
                    writer.write(profile, eggs, items, pokemons)

                    Log.normal("Wrote export $filename.")
                }
                "JSON" -> {
                    val filename = "export_${settings.name}.json"
                    val writer = ExportJSONWriter(filename)
                    writer.write(profile, eggs, items, pokemons)

                    Log.normal("Wrote export $filename.")
                }
                else -> {
                    Log.red("Invalid export configuration!")
                }
            }
        } catch (e: Exception) {
            Log.red("Error writing export: ${e.message}")
        }
    }

    // Detect if the "float" fields need to be forced to use "," instead of "." (DSV export)
    private fun ds(string: String, settings: Settings): String {
        var result = string
        if (settings.export.equals("DSV")) {
            result = result.replace(".", ",")
        }

        return result
    }

    // Don't concat 2nd pokemon type "NONE"
    private fun formatType(type1: String, type2: String): String {
        if (type2.equals("NONE")) return type1
        return "$type1/$type2"
    }
}
