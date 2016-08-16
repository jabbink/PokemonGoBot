/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper.util.data

import com.pokegoapi.api.PokemonGo
import com.pokegoapi.api.player.PlayerProfile

data class ProfileData (

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
    fun buildFromApi(api: PokemonGo) : ProfileData {

        val profile = api.playerProfile

        this.name = profile.playerData.username
        this.level = profile.stats.level
        this.exp = profile.stats.experience
        this.expToNextLevel = profile.stats.nextLevelXp
        this.stardust = profile.currencies.get(PlayerProfile.Currency.STARDUST)
        this.team = profile.playerData.team.name
        this.pokebankLimit = profile.playerData.maxPokemonStorage
        this.pokebankUsage = api.inventories.pokebank.pokemons.size
        this.backpackLimit = profile.playerData.maxItemStorage
        this.backpackUsage = api.inventories.itemBag.itemsCount
        this.coin = profile.currencies.get(PlayerProfile.Currency.POKECOIN)

        return this
    }
}