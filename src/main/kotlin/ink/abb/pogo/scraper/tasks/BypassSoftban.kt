/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper.tasks

import ink.abb.pogo.api.cache.Pokestop
import ink.abb.pogo.scraper.Bot
import ink.abb.pogo.scraper.Context
import ink.abb.pogo.scraper.Settings
import ink.abb.pogo.scraper.Task
import ink.abb.pogo.scraper.util.Log
import ink.abb.pogo.scraper.util.map.loot

class BypassSoftban(val pokestop: Pokestop) : Task {
    override fun run(bot: Bot, ctx: Context, settings: Settings) {
        repeat(settings.banSpinCount) { i ->
            pokestop.loot()

            if ((i + 1) % 10 == 0)
                Log.yellow("${i + 1}/${settings.banSpinCount}")
        }
    }
}
