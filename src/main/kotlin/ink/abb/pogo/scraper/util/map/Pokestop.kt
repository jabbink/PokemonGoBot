package ink.abb.pogo.scraper.util.map

import com.pokegoapi.api.map.fort.Pokestop
import java.util.*

fun Pokestop.canLoot(ignoreDistance: Boolean = false, lootTimeouts: Map<String, Long>): Boolean {
    val canLoot = lootTimeouts.getOrElse(id, { 0 }) < System.currentTimeMillis()
    return (ignoreDistance || inRange()) && canLoot
}