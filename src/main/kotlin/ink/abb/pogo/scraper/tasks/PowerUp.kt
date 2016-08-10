package ink.abb.pogo.scraper.tasks

import POGOProtos.Enums.PokemonIdOuterClass
import com.pokegoapi.api.player.PlayerProfile
import com.pokegoapi.api.pokemon.Pokemon
import ink.abb.pogo.scraper.Bot
import ink.abb.pogo.scraper.Context
import ink.abb.pogo.scraper.Settings
import ink.abb.pogo.scraper.Task
import ink.abb.pogo.scraper.util.Log
import ink.abb.pogo.scraper.util.cachedInventories
import ink.abb.pogo.scraper.util.pokemon.getIvPercentage

/**
 * Created by ddcbdevins on 8/10/16.
 */
class PowerUp : Task {
    override fun run(bot: Bot, ctx: Context, settings: Settings) {
        val eligblePokes = ctx.api.cachedInventories.pokebank.pokemons.filter {
            it.getIvPercentage() >= settings.powerUpIvThreshold && it.candiesToEvolve == 0
        }.sortedByDescending {
            it.ivRatio
        }.groupBy {
            it.pokemonId
        }

        eligblePokes.forEach {
            if (settings.powerUpOnlyBest) {
                powerUp(it.value[0], ctx)
            } else {
                it.value.forEach {
                    powerUp(it, ctx)
                }
            }
        }
    }

    fun powerUp(pokemon: Pokemon, ctx: Context) {
        if (pokemon.candyCostsForPowerup <= ctx.api.cachedInventories.candyjar.getCandies(pokemon.pokemonFamily) &&
                pokemon.stardustCostsForPowerup <= ctx.profile.currencies.get(PlayerProfile.Currency.STARDUST)!!) {
            while (pokemon.level < ctx.profile.stats.level + 2) {
                Log.blue("PowerUp ${pokemon.pokemonId.name} IV ${pokemon.getIvPercentage()}% ${pokemon.cp} cp -> ${pokemon.cpAfterPowerup}")
                pokemon.powerUp()
            }
        }
    }
}