package ink.abb.pogo.scraper.tasks

import POGOProtos.Enums.PokemonIdOuterClass
import com.pokegoapi.api.pokemon.PokemonMetaRegistry
import ink.abb.pogo.scraper.Bot
import ink.abb.pogo.scraper.Context
import ink.abb.pogo.scraper.Settings
import ink.abb.pogo.scraper.Task
import ink.abb.pogo.scraper.util.Log
import java.util.concurrent.locks.ReentrantLock
import kotlin.comparisons.compareBy

/**
 * Created by ddcbdevins on 7/26/16.
 */
class SmartEvolve : Task {

    lateinit private var release: ReleasePokemon

    constructor(release: ReleasePokemon) {
        this.release = release
    }

    override fun run(bot: Bot, ctx: Context, settings: Settings) {
        val pokebagFillPercent = ctx.api.inventories.pokebank.pokemons.size.toDouble() / ctx.profile.pokemonStorage
        Log.white("Pokebag ${pokebagFillPercent * 100} % full.")
        val groupedPokemon = ctx.api.inventories.pokebank.pokemons.groupBy { it.pokemonId }
        if (pokebagFillPercent >= 0.7) {
            val canEvolve = groupedPokemon.filter {
                PokemonMetaRegistry.getMeta(it.key).candyToEvolve > 0
            }
            if (canEvolve.isEmpty()) {
                return
            }
            val evolveSorted = canEvolve.entries.map({ group ->
                val descendant = PokemonMetaRegistry.getHightestForFamily(PokemonMetaRegistry.getMeta(group.key).family)
                if (group.key == PokemonIdOuterClass.PokemonId.EEVEE) {
                    Pair(-1, group)
                } else if (ctx.api.inventories.pokedex.getPokedexEntry(descendant) == null) {
                    Pair(0, group)
                } else {
                    Pair(PokemonMetaRegistry.getMeta(group.key).candyToEvolve, group)
                }
            }).sortedWith(compareBy { it.first })

            evolveSorted.forEach {
                val poke = it.second
                val sorted = poke.value.sortedByDescending {
                    if (settings.sortByIV) {
                        (it.ivRatio * 100).toInt()
                    } else {
                        it.cp
                    }
                }
                val candyNeeded = PokemonMetaRegistry.getMeta(poke.key).candyToEvolve
                for ((index, pokemon) in sorted.withIndex()) {
                    if (ctx.api.inventories.candyjar.getCandies(pokemon.pokemonFamily) < candyNeeded) {
                        break;
                    }
                    if (pokemon.pokemonId == PokemonIdOuterClass.PokemonId.EEVEE) {
                        if (ctx.api.inventories.pokedex.getPokedexEntry(PokemonIdOuterClass.PokemonId.VAPOREON) == null) {
                            pokemon.renamePokemon("Rainer")
                        } else if (ctx.api.inventories.pokedex.getPokedexEntry(PokemonIdOuterClass.PokemonId.FLAREON) == null) {
                            pokemon.renamePokemon("Pyro")
                        } else if (ctx.api.inventories.pokedex.getPokedexEntry(PokemonIdOuterClass.PokemonId.JOLTEON) == null) {
                            pokemon.renamePokemon("Sparky")
                        }
                    }
                    Log.green("Evolving ${pokemon.pokemonId.name} because we have ${pokemon.candy} candy and only need ${candyNeeded}.")
                    pokemon.evolve()
                }
            }

            release.run(bot,ctx,settings)
        }
    }
}