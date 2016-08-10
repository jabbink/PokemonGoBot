package ink.abb.pogo.scraper.evolve

import ink.abb.pogo.scraper.Bot
import ink.abb.pogo.scraper.Context
import ink.abb.pogo.scraper.Settings

interface EvolutionStrategy {
    fun evolve(bot: Bot, ctx: Context, settings: Settings)
}