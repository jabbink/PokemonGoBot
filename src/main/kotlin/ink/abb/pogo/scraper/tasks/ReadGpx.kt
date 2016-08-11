package ink.abb.pogo.scraper.tasks

import ink.abb.pogo.scraper.Bot
import ink.abb.pogo.scraper.Context
import ink.abb.pogo.scraper.Settings
import ink.abb.pogo.scraper.Task
import ink.abb.pogo.scraper.util.Log
import org.jdom2.Document

import org.jdom2.input.SAXBuilder
import java.io.File

/**
 * Created by Peyphour on 8/11/16.
 */

class ReadGpx : Task {
    override fun run(bot: Bot, ctx: Context, settings: Settings) {
        var builder: SAXBuilder = SAXBuilder()
        var document: Document
        try {
            document = builder.build(File(settings.gpxFile))
        } catch(e: Exception) {
            e.printStackTrace()
            return
        }

        val root = document.rootElement
        for(element in root.children) {
            for(element1 in element.children)
                for(element2 in element1.children)
                    Log.magenta(element2.name)
        }

    }
}
