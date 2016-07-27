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

    private val lock = ReentrantLock();

    override fun run(bot: Bot, ctx: Context, settings: Settings) {
        try {
            lock.tryLock()

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
                        Log.green("Evolving ${pokemon.pokemonId.name} because we have ${pokemon.candy} candy and only need ${candyNeeded}.")
                        pokemon.evolve()
                    }

                    val currentPokemon = ctx.api.inventories.pokebank.pokemons.groupBy { it.pokemonId }
                    currentPokemon.forEach {
                        if (it.value.size > 0) {
                            val pokes = it.value.sortedBy {
                                if (settings.sortByIV) {
                                    (it.ivRatio * 100).toInt()
                                } else {
                                    it.cp
                                }
                            }
                            for ((index, value) in pokes.withIndex()) {
                                if (index != pokes.size - 1) {
                                    Log.red("Transfering ${value.pokemonId.name} (${value.ivRatio} - ${value.cp}cp) because it is not the best")
                                    value.transferPokemon()
                                } else {
                                    Log.green("The best ${value.pokemonId.name}'s stats are: ${value.ivRatio} & ${value.cp}cp")
                                }
                            }
                        }
                    }
                }
            }
        } finally {
            lock.unlock()
        }
    }
}