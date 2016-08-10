package ink.abb.pogo.scraper.evolve

import ink.abb.pogo.scraper.Bot
import ink.abb.pogo.scraper.Context
import ink.abb.pogo.scraper.Settings

/**
 * Created by ddcbdevins on 8/10/16.
 */
interface EvolutionStrategy {
    fun evolve(bot: Bot, ctx: Context, settings: Settings)
}