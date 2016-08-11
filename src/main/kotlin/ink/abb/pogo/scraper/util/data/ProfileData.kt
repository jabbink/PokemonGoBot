package ink.abb.pogo.scraper.util.data

import com.pokegoapi.api.PokemonGo
import com.pokegoapi.api.player.PlayerProfile

/**
 * Created by Peyphour on 8/11/16.
 */

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