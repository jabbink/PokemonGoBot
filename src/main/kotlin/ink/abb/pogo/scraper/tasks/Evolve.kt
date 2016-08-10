package ink.abb.pogo.scraper.tasks

import POGOProtos.Enums.PokemonFamilyIdOuterClass
import POGOProtos.Enums.PokemonIdOuterClass
import com.pokegoapi.api.pokemon.Pokemon
import ink.abb.pogo.scraper.Bot
import ink.abb.pogo.scraper.Context
import ink.abb.pogo.scraper.Settings
import ink.abb.pogo.scraper.Task
import ink.abb.pogo.scraper.evolve.EvolutionStrategy
import ink.abb.pogo.scraper.evolve.IvMaximizingStrategy
import ink.abb.pogo.scraper.util.Log

/**
 * Created by ddcbdevins on 7/26/16.
 */
class Evolve : Task {

    private val DEFAULT_EVOLUTION_STRATEGY = "max_iv"

    private val EVOLVE_STRATEGY_MAPPER = mapOf<String, EvolutionStrategy>(
            Pair("max_iv", IvMaximizingStrategy())
    )

    override fun run(bot: Bot, ctx: Context, settings: Settings) {
        if (EVOLVE_STRATEGY_MAPPER.containsKey(settings.evolutionStrategy)) {
            EVOLVE_STRATEGY_MAPPER.get(settings.evolutionStrategy)?.evolve(bot, ctx, settings)
        } else {
            if (settings.evolutionStrategy.isNotBlank()) {
                Log.red("Evolution strategy ${settings.evolutionStrategy} does not exist. Not running this task")
            } else {
                EVOLVE_STRATEGY_MAPPER.get(DEFAULT_EVOLUTION_STRATEGY)?.evolve(bot, ctx, settings)
            }
        }
    }
}