package ink.abb.pogo.scraper.tasks

import com.pokegoapi.google.common.geometry.S2LatLng
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
            // Invalid file or no file specified : Expected behaviour
            return
        }

        val coords = document.rootElement.getChild("trk", document.rootElement.namespace)
                        .getChild("trkseg", document.rootElement.namespace)

        var i: Int = settings.gpxRepeat

        // gpxRepeat == 0 or -1 : No coordinates will be added
        while(i > 0) {
            for (element in coords.children)
                ctx.coordinatesToGoTo.add(S2LatLng.fromRadians(
                        element.getAttribute("lat").doubleValue,
                        element.getAttribute("lon").doubleValue
                ))
            i--
        }
    }
}
