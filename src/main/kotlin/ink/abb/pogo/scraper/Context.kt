package ink.abb.pogo.scraper

import com.google.common.util.concurrent.AtomicDouble
import com.pokegoapi.api.PokemonGo
import com.pokegoapi.api.player.PlayerProfile
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @author Andrew Potter (apottere)
 */

data class Context(
    val api: PokemonGo,
    val profile: PlayerProfile,
    val lat: AtomicDouble,
    val lng: AtomicDouble,

    var walking: AtomicBoolean = AtomicBoolean(false)
)
