package ink.abb.pogo.scraper

import com.google.common.util.concurrent.AtomicDouble
import com.pokegoapi.api.PokemonGo
import com.pokegoapi.api.player.PlayerProfile

/**
 * @author Andrew Potter (ddcapotter)
 */

data class Context(
    val api: PokemonGo,
    val profile: PlayerProfile,
    val lat: AtomicDouble,
    val lng: AtomicDouble,

    var walking: Boolean = false
)
