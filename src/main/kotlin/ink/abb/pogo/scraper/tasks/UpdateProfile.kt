package ink.abb.pogo.scraper.tasks

import ink.abb.pogo.scraper.Bot
import ink.abb.pogo.scraper.Context
import ink.abb.pogo.scraper.Settings
import ink.abb.pogo.scraper.Task
import java.text.DecimalFormat

/**
 * @author Andrew Potter (apottere)
 */
class UpdateProfile : Task {
    override fun run(bot: Bot, ctx: Context, settings: Settings) {
        val player = ctx.api.getPlayerProfile(true)
        val nextXP = player.stats.nextLevelXp - player.stats.prevLevelXp
        val curLevelXP = player.stats.experience - player.stats.prevLevelXp
        val ratio = DecimalFormat("##.00").format(curLevelXP.toDouble() / nextXP.toDouble() * 100.0)
        println("Profile update : ${player.stats.experience} XP on LVL ${player.stats.level}; $curLevelXP/$nextXP ($ratio%) to LVL ${player.stats.level + 1}")
    }
}
