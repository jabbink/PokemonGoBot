package ink.abb.pogo.scraper.tasks

import ink.abb.pogo.scraper.Bot
import ink.abb.pogo.scraper.Context
import ink.abb.pogo.scraper.Settings
import ink.abb.pogo.scraper.Task
import ink.abb.pogo.scraper.evolve.EvolutionStrategy
import ink.abb.pogo.scraper.evolve.IvMaximizingStrategy
import ink.abb.pogo.scraper.evolve.XpBatchStrategy
import ink.abb.pogo.scraper.util.Log

class Evolve : Task {

    private val DEFAULT_EVOLUTION_STRATEGY = "xp_batch"

    private val EVOLVE_STRATEGY_MAPPER = mapOf<String, EvolutionStrategy>(
            Pair("xp_batch", XpBatchStrategy()),
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