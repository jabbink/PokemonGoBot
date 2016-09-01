/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper.util.logback

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.filter.AbstractMatcherFilter
import ch.qos.logback.core.spi.FilterReply
import org.slf4j.Marker
import org.slf4j.MarkerFactory

class MarkerFilter : AbstractMatcherFilter<ILoggingEvent>() {
    private var markerToMatch: Marker? = null

    override fun start() {
        if (markerToMatch != null)
            super.start()
        else
            addError("no marker configured")
    }

    override fun decide(event: ILoggingEvent): FilterReply {
        val marker = event.marker
        if (!isStarted)
            return FilterReply.NEUTRAL
        if (marker == null)
            return onMismatch
        if (markerToMatch!!.contains(marker))
            return onMatch
        return onMismatch
    }

    fun setMarker(markerStr: String?) {
        if (null != markerStr)
            markerToMatch = MarkerFactory.getMarker(markerStr)
    }
}
