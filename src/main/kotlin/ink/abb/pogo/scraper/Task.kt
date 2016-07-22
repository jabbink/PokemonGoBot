package ink.abb.pogo.scraper

/**
 * @author Andrew Potter (apottere)
 */
interface Task {
    fun run(bot: Bot, ctx: Context, settings: Settings)
}
