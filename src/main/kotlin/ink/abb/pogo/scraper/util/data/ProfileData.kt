/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper.util.data

import ink.abb.pogo.api.PoGoApi
import java.util.concurrent.atomic.AtomicInteger

data class ProfileData(

        var name: String? = null,
        var level: Int? = null,
        var exp: Long? = null,
        var expToNextLevel: Long? = null,
        var stardust: Int? = null,
        var team: String? = null,
        var pokebankLimit: Int? = null,
        var pokebankUsage: Int? = null,
        var backpackLimit: Int? = null,
        var backpackUsage: Int? = null,
        var coin: Int? = null

) {
    fun buildFromApi(api: PoGoApi): ProfileData {
        this.name = api.playerData.username
        this.level = api.inventory.playerStats.level
        this.exp = api.inventory.playerStats.experience
        this.expToNextLevel = api.inventory.playerStats.nextLevelXp
        this.stardust = api.inventory.currencies.getOrPut("STARDUST", { AtomicInteger(0) }).get()
        this.team = api.playerData.team.name
        this.pokebankLimit = api.playerData.maxPokemonStorage
        this.pokebankUsage = api.inventory.pokemon.size
        this.backpackLimit = api.playerData.maxItemStorage
        this.backpackUsage = api.inventory.size
        this.coin = api.inventory.currencies.getOrPut("POKECOIN", { AtomicInteger(0) }).get()

        return this
    }
}
