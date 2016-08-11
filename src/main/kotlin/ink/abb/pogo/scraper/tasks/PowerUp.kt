package ink.abb.pogo.scraper.tasks

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
        // TODO exclude those where a better poke in the same family exists that be evolved to here
        var eligiblePokemon = ctx.api.cachedInventories.pokebank.pokemons.filter {
            it.getIvPercentage() >= settings.powerUpIvThreshold &&
                    it.candiesToEvolve == 0
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

        val poke = eligiblePokemon.filter { it.level < ctx.profile.stats.level + 2 }[0]
        Log.blue("Attempting to PowerUp ${poke.pokemonId.name} IV ${poke.getIvPercentage()}%")
        // Use stardust on best possible only
        powerUp(poke, ctx)
    }

    fun powerUp(pokemon: Pokemon, ctx: Context) {
        while (pokemon.level < ctx.profile.stats.level + 2) {
            Log.red("${ctx.api.cachedInventories.candyjar.getCandies(pokemon.pokemonFamily)}/${pokemon.candyCostsForPowerup} candy")
            Log.red("${ctx.profile.currencies.get(PlayerProfile.Currency.STARDUST)!!}/${pokemon.stardustCostsForPowerup} stardust")
            if (pokemon.candyCostsForPowerup <= ctx.api.cachedInventories.candyjar.getCandies(pokemon.pokemonFamily) &&
                    pokemon.stardustCostsForPowerup <= ctx.profile.currencies.get(PlayerProfile.Currency.STARDUST)!!) {
                Log.blue("PowerUp ${pokemon.pokemonId.name} IV ${pokemon.getIvPercentage()}% ${pokemon.cp} cp -> ${pokemon.cpAfterPowerup}")
                val powerUpCostCandies = pokemon.candyCostsForPowerup
                val powerUpCostStardust = pokemon.stardustCostsForPowerup
                pokemon.powerUp()
                ctx.api.cachedInventories.candyjar.removeCandy(pokemon.pokemonFamily, powerUpCostCandies)
                ctx.api.playerProfile.addCurrency(PlayerProfile.Currency.STARDUST.name, powerUpCostStardust * -1)
            } else {
                return
            }
        }
    }
}