package ink.abb.pogo.scraper.evolve

import POGOProtos.Enums.PokemonFamilyIdOuterClass
import POGOProtos.Enums.PokemonIdOuterClass
import com.pokegoapi.api.pokemon.Pokemon
import ink.abb.pogo.scraper.Bot
import ink.abb.pogo.scraper.Context
import ink.abb.pogo.scraper.Settings
import ink.abb.pogo.scraper.tasks.ReleasePokemon
import ink.abb.pogo.scraper.util.Log

/**
 * Created by ddcbdevins on 8/10/16.
 */
class IvMaximizingStrategy : EvolutionStrategy {

    lateinit private var EEVEE_EVOLUTION_DATA: Map<PokemonIdOuterClass.PokemonId, String>

    constructor() {
        EEVEE_EVOLUTION_DATA = mapOf(
                Pair(PokemonIdOuterClass.PokemonId.VAPOREON, "Rainer"),
                Pair(PokemonIdOuterClass.PokemonId.FLAREON, "Pyro"),
                Pair(PokemonIdOuterClass.PokemonId.JOLTEON, "Sparky")
        )
    }

    override fun evolve(bot: Bot, ctx: Context, settings: Settings) {
        val pokemonFamilies = ctx.api.inventories.pokebank.pokemons.groupBy { it.pokemonFamily }

        pokemonFamilies.forEach {
            var run = true
            while (run) {
                val pokemon = nextPokemonToEvolve(ctx, it.key)
                if (pokemon == null) {
                    run = false
                    continue
                }

                Log.green("Evolving ${pokemon.pokemonId.name} with IV ${pokemon.ivRatio} and ${pokemon.cp}cp")
                pokemon.evolve()
            }
        }
    }

    fun nextPokemonToEvolve(ctx: Context, family: PokemonFamilyIdOuterClass.PokemonFamilyId) : Pokemon? {
        val candies = ctx.api.inventories.candyjar.getCandies(family)
        val pokemonFamily = ctx.api.inventories.pokebank.pokemons.groupBy { it.pokemonFamily }.get(family)

        var evolvePriority = pokemonFamily.orEmpty().sortedByDescending { it.ivRatio }
        var pokemonToEvolve = evolvePriority[0]

        if (pokemonToEvolve.candiesToEvolve == 0) {
            val generations = pokemonFamily.orEmpty().filter { it.candiesToEvolve > 0 }.groupBy { it.candiesToEvolve }
            if (generations.size == 0) {
                return null
            }

            evolvePriority = generations.getOrElse(generations.keys.sorted()[0]){ listOf() }.sortedByDescending { it.ivRatio }
            pokemonToEvolve = evolvePriority[0]
        }

        if (pokemonToEvolve.candiesToEvolve > candies) {
            return null
        }

        if (pokemonToEvolve.pokemonId == PokemonIdOuterClass.PokemonId.EEVEE) {
            EEVEE_EVOLUTION_DATA.forEach {
                if (ctx.api.inventories.pokedex.getPokedexEntry(it.component1()) == null) {
                    pokemonToEvolve.renamePokemon(it.component2())
                    return pokemonToEvolve
                }

                val current = ctx.api.inventories.pokebank.getPokemonByPokemonId(it.key).sortedByDescending { it.ivRatio }
                if (current[0].ivRatio < pokemonToEvolve.ivRatio) {
                    pokemonToEvolve.renamePokemon(it.component2())
                    return pokemonToEvolve
                }
            }
        }

        return pokemonToEvolve
    }
}