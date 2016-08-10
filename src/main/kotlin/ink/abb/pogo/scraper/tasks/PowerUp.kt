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

class PowerUp : Task {
    override fun run(bot: Bot, ctx: Context, settings: Settings) {
        var eligiblePokemon = ctx.api.cachedInventories.pokebank.pokemons.filter {
            it.getIvPercentage() >= settings.powerUpIvThreshold &&
                    it.candiesToEvolve == 0 &&
                    it.level < ctx.profile.stats.level + 2
        }.sortedByDescending {
            it.ivRatio
        }

        if (settings.powerUpOnlyBest) {
            eligiblePokemon = eligiblePokemon.groupBy {
                it.pokemonId
            }.flatMap {
                listOf(it.value[0])
            }
        }

        eligiblePokemon.forEach {
            powerUp(it, ctx)
        }
    }

    fun powerUp(pokemon: Pokemon, ctx: Context) {
        while (pokemon.level < ctx.profile.stats.level + 2) {
            if (pokemon.candyCostsForPowerup <= ctx.api.cachedInventories.candyjar.getCandies(pokemon.pokemonFamily) &&
                    pokemon.stardustCostsForPowerup <= ctx.profile.currencies.get(PlayerProfile.Currency.STARDUST)!!) {
                Log.red("${ctx.api.cachedInventories.candyjar.getCandies(pokemon.pokemonFamily)}/${pokemon.candyCostsForPowerup} candy")
                Log.red("${ctx.profile.currencies.get(PlayerProfile.Currency.STARDUST)!!}/${pokemon.stardustCostsForPowerup} stardust")
                Log.blue("PowerUp ${pokemon.pokemonId.name} IV ${pokemon.getIvPercentage()}% ${pokemon.cp} cp -> ${pokemon.cpAfterPowerup}")
                val powerUpCostCandies = pokemon.candyCostsForPowerup
                pokemon.powerUp()
                ctx.api.cachedInventories.candyjar.removeCandy(pokemon.pokemonFamily, powerUpCostCandies)
            } else {
                return
            }
        }
    }
}