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
import com.google.common.geometry.S2CellId
import com.google.common.geometry.S2LatLng
import ink.abb.pogo.api.cache.BagPokemon
import ink.abb.pogo.api.util.PokemonMetaRegistry
import ink.abb.pogo.api.util.PokemonMoveMetaRegistry
import ink.abb.pogo.scraper.Bot
import ink.abb.pogo.scraper.Context
import ink.abb.pogo.scraper.Settings
import ink.abb.pogo.scraper.Task
import ink.abb.pogo.scraper.util.Log
import ink.abb.pogo.scraper.util.io.ExportCSVWriter
import ink.abb.pogo.scraper.util.io.ExportJSONWriter
import ink.abb.pogo.scraper.util.pokemon.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class Export : Task {
    override fun run(bot: Bot, ctx: Context, settings: Settings) {
        val compareName = Comparator<BagPokemon> { a, b ->
            a.pokemonData.pokemonId.name.compareTo(b.pokemonData.pokemonId.name)
        }
        val compareIv = Comparator<BagPokemon> { a, b ->
            // compare b to a to get it descending
            if (settings.sortByIv) {
                b.pokemonData.getIv().compareTo(a.pokemonData.getIv())
            } else {
                b.pokemonData.cp.compareTo(a.pokemonData.cp)
            }
        }

        try {
            val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

            // Output player information
            val profile: Map<String, String> = mapOf(
                    Pair("Name", ctx.api.playerData.username),
                    Pair("Team", ctx.api.playerData.team.name),
                    Pair("Pokecoin", "${ctx.api.inventory.currencies.getOrPut("POKECOIN", { AtomicInteger(0) }).get()}"),
                    Pair("Stardust", "${ctx.api.inventory.currencies.getOrPut("STARDUST", { AtomicInteger(0) }).get()}"),
                    Pair("Level", "${ctx.api.inventory.playerStats.level}"),
                    Pair("Experience", "${ctx.api.inventory.playerStats.experience}"),
                    Pair("Previous Level Experience", "${ctx.api.inventory.playerStats.prevLevelXp}"),
                    Pair("Next Level Experience", "${ctx.api.inventory.playerStats.nextLevelXp}"),
                    Pair("Km walked", ds("${ctx.api.inventory.playerStats.kmWalked}", settings)),
                    Pair("Pokemons Encountered", "${ctx.api.inventory.playerStats.pokemonsEncountered}"),
                    Pair("Pokemons Captured", "${ctx.api.inventory.playerStats.pokemonsCaptured}"),
                    Pair("Unique Pokedex Entries", "${ctx.api.inventory.playerStats.uniquePokedexEntries}"),
                    Pair("Evolutions", "${ctx.api.inventory.playerStats.evolutions}"),
                    Pair("Pokestop Visits", "${ctx.api.inventory.playerStats.pokeStopVisits}"),
                    Pair("Pokeballs Thrown", "${ctx.api.inventory.playerStats.pokeballsThrown}"),
                    Pair("Eggs Hatched", "${ctx.api.inventory.playerStats.eggsHatched}"),
                    Pair("Battle Attack Won", "${ctx.api.inventory.playerStats.battleAttackWon}"),
                    Pair("Battle Attack Total", "${ctx.api.inventory.playerStats.battleAttackTotal}"),
                    Pair("Battle Defended Won", "${ctx.api.inventory.playerStats.battleDefendedWon}"),
                    Pair("Battle Training Won", "${ctx.api.inventory.playerStats.battleTrainingTotal}"),
                    Pair("Battle Training Total", "${ctx.api.inventory.playerStats.battleTrainingTotal}"),
                    Pair("Prestige Raised Total", "${ctx.api.inventory.playerStats.prestigeRaisedTotal}"),
                    Pair("Prestige Dropped Total", "${ctx.api.inventory.playerStats.prestigeDroppedTotal}"),
                    Pair("Pokemon Deployed", "${ctx.api.inventory.playerStats.pokemonDeployed}"),
                    Pair("Pokebank Size", "${ctx.api.inventory.pokemon.size + ctx.api.inventory.eggs.size}"),
                    Pair("Maximum Pokebank Storage", "${ctx.api.playerData.maxPokemonStorage}"),
                    Pair("Inventory Size", "${ctx.api.inventory.size}"),
                    Pair("Maximum Inventory Storage", "${ctx.api.playerData.maxItemStorage}"),
                    Pair("Last Update", dateFormatter.format(Date())),
                    Pair("Location Latitude", ds("${ctx.lat.get()}", settings)),
                    Pair("Location Longitude", ds("${ctx.lng.get()}", settings))
            )

            // Output Eggs
            val eggs = ArrayList<Map<String, String>>()
            for (egg in ctx.api.inventory.eggs) {
                val latLng = S2LatLng(S2CellId(egg.value.pokemonData.capturedCellId).toPoint())

                eggs.add(mapOf(
                        Pair("Walked [km]", ds("${egg.value.pokemonData.eggKmWalked(ctx.api)}", settings)),
                        Pair("Target [km]", ds("${egg.value.pokemonData.eggKmWalkedTarget}", settings)),
                        Pair("Incubated?", "${egg.value.pokemonData.incubated}"),
                        Pair("Found", dateFormatter.format(Date(egg.value.pokemonData.creationTimeMs))),
                        Pair("Location Latitude", ds("${latLng.latDegrees()}", settings)),
                        Pair("Location Longitude", ds("${latLng.lngDegrees()}", settings))
                ))
            }

            // Output Items
            val items = ArrayList<Map<String, String>>()
            for (item in ctx.api.inventory.items) {
                items.add(mapOf(
                        Pair("Item", item.key.name),
                        Pair("Count", "${item.value.get()}")
                ))
            }

            // Output Pokebank
            val pokemons = ArrayList<Map<String, String>>()

            ctx.api.inventory.pokemon.map { it.value }.sortedWith(compareName.thenComparing(compareIv)).map {
                val latLng = S2LatLng(S2CellId(it.pokemonData.capturedCellId).toPoint())

                val pmeta = PokemonMetaRegistry.getMeta(PokemonIdOuterClass.PokemonId.forNumber(it.pokemonData.pokemonId.number))
                val pmmeta1 = PokemonMoveMetaRegistry.getMeta(PokemonMoveOuterClass.PokemonMove.forNumber(it.pokemonData.move1.number))
                val pmmeta2 = PokemonMoveMetaRegistry.getMeta(PokemonMoveOuterClass.PokemonMove.forNumber(it.pokemonData.move2.number))

                mapOf(
                        Pair("Number", "${it.pokemonData.pokemonId.number}"),
                        Pair("Name", it.pokemonData.pokemonId.name),
                        Pair("Nickname", it.pokemonData.nickname),
                        Pair("Favorite?", "${it.pokemonData.favorite}"),
                        Pair("CP", "${it.pokemonData.cp}"),
                        Pair("IV [%]", "${it.pokemonData.getIvPercentage()}"),
                        Pair("Stamina (HP)", "${it.pokemonData.stamina}"),
                        Pair("Max Stamina (HP)", "${it.pokemonData.staminaMax}"),
                        Pair("Class", pmeta.pokemonClass.name),
                        Pair("Type", formatType(pmeta.type1.name, pmeta.type2.name)),
                        Pair("Move 1", it.pokemonData.move1.name),
                        Pair("Move 1 Type", pmmeta1.type.name),
                        Pair("Move 1 Power", "${pmmeta1.power}"),
                        Pair("Move 1 Accuracy", "${pmmeta1.accuracy}"),
                        Pair("Move 1 Crit Chance", ds("${pmmeta1.critChance}", settings)),
                        Pair("Move 1 Time", "${pmmeta1.time}"),
                        Pair("Move 1 Energy", "${pmmeta1.energy}"),
                        Pair("Move 2", it.pokemonData.move2.name),
                        Pair("Move 2 Type", pmmeta2.type.name),
                        Pair("Move 2 Power", "${pmmeta2.power}"),
                        Pair("Move 2 Accuracy", "${pmmeta2.accuracy}"),
                        Pair("Move 2 Crit Chance", ds("${pmmeta2.critChance}", settings)),
                        Pair("Move 2 Time", "${pmmeta2.time}"),
                        Pair("Move 2 Energy", "${pmmeta2.energy}"),
                        Pair("iStamina", "${it.pokemonData.individualStamina}"),
                        Pair("iAttack", "${it.pokemonData.individualAttack}"),
                        Pair("iDefense", "${it.pokemonData.individualDefense}"),
                        Pair("cpMultiplier", ds("${it.pokemonData.cpMultiplier}", settings)),
                        Pair("Height [m]", ds("${it.pokemonData.heightM}", settings)),
                        Pair("Weight [kg]", ds("${it.pokemonData.weightKg}", settings)),
                        Pair("Candy", "${ctx.api.inventory.candies.getOrPut(pmeta.family, { AtomicInteger(0) }).get()}"),
                        Pair("Candies to evolve", "${pmeta.candyToEvolve}"),
                        Pair("Candy costs for powerup", "${it.pokemonData.candyCostsForPowerup}"),
                        Pair("Stardust costs for powerup", "${it.pokemonData.stardustCostsForPowerup}"),
                        Pair("Found", dateFormatter.format(Date(it.pokemonData.creationTimeMs))),
                        Pair("Found Latitude", ds("${latLng.latDegrees()}", settings)),
                        Pair("Found Longitude", ds("${latLng.lngDegrees()}", settings)),
                        Pair("Base Capture Rate", ds("${pmeta.baseCaptureRate}", settings)),
                        Pair("Base Flee Rate", ds("${pmeta.baseFleeRate}", settings)),
                        Pair("Battles Attacked", "${it.pokemonData.battlesAttacked}"),
                        Pair("Battles Defended", "${it.pokemonData.battlesDefended}"),
                        Pair("Injured?", "${it.pokemonData.injured}"),
                        Pair("Fainted?", "${it.pokemonData.fainted}"),
                        Pair("Level", ds("${it.pokemonData.level}", settings)),
                        Pair("CP after powerup", "${it.pokemonData.cpAfterPowerup}"),
                        Pair("Max CP", "${it.pokemonData.maxCp}"),
                        Pair("ID", "${it.pokemonData.id}")
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
