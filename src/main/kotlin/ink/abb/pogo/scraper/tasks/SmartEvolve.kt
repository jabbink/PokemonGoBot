package ink.abb.pogo.scraper.tasks

import POGOProtos.Enums.PokemonFamilyIdOuterClass
import POGOProtos.Enums.PokemonIdOuterClass
import com.pokegoapi.api.pokemon.Pokemon
import ink.abb.pogo.scraper.Bot
import ink.abb.pogo.scraper.Context
import ink.abb.pogo.scraper.Settings
import ink.abb.pogo.scraper.Task
import ink.abb.pogo.scraper.util.Log

/**
 * Created by ddcbdevins on 7/26/16.
 */
class SmartEvolve : Task {

    lateinit private var release: ReleasePokemon
    lateinit private var EEVEE_EVOLUTION_DATA: Map<PokemonIdOuterClass.PokemonId, String>

    constructor(release: ReleasePokemon) {
        this.release = release

        EEVEE_EVOLUTION_DATA = mapOf(
                Pair(PokemonIdOuterClass.PokemonId.VAPOREON, "Rainer"),
                Pair(PokemonIdOuterClass.PokemonId.FLAREON, "Pyro"),
                Pair(PokemonIdOuterClass.PokemonId.JOLTEON, "Sparky")
        )
    }

    override fun run(bot: Bot, ctx: Context, settings: Settings) {
        val pokebagFillPercent = ctx.api.inventories.pokebank.pokemons.size.toDouble() / ctx.profile.pokemonStorage
        Log.white("Pokebag ${pokebagFillPercent * 100} % full.")
        if (pokebagFillPercent >= 0.8) {
            val pokemonFamilies = ctx.api.inventories.pokebank.pokemons.groupBy { it.pokemonFamily }

            pokemonFamilies.forEach {
                var run = true
                while (run) {
                    val pokemon = nextPokemonToEvolve(ctx, settings, it.key)
                    if (pokemon == null) {
                        run = false
                        continue
                    }

                    Log.green("Evolving ${pokemon.pokemonId.name} with IV ${pokemon.ivRatio} and ${pokemon.cp}cp")
                    pokemon.evolve()

                    // This might work. not sure
//                    if (pokemon.pokemonFamily == PokemonFamilyIdOuterClass.PokemonFamilyId.FAMILY_EEVEE) {
//                        pokemon.renamePokemon("")
//                    }
                }
            }

            release.run(bot,ctx,settings)
        }
    }

    /*
     * Prioritize IV over xp farming
     */
    fun nextPokemonToEvolve(ctx: Context, settings: Settings, family: PokemonFamilyIdOuterClass.PokemonFamilyId) : Pokemon? {
        val candies = ctx.api.inventories.candyjar.getCandies(family)
        val pokemonFamily = ctx.api.inventories.pokebank.pokemons.groupBy { it.pokemonFamily }.get(family)

        var evolvePriority = pokemonFamily.orEmpty().sortedByDescending { it.ivRatio }
        var pokemonToEvolve = evolvePriority[0]

        if (pokemonToEvolve.candiesToEvolve == 0) {
            val generations = pokemonFamily.orEmpty().filter { it.candiesToEvolve > 0 }.groupBy { it.candiesToEvolve }
            if (generations.size == 0) {
                Log.white("Only have pokemon that cannot evolve in ${family.name}")
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