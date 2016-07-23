/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper.tasks

import ink.abb.pogo.scraper.util.Log
import ink.abb.pogo.scraper.Bot
import ink.abb.pogo.scraper.Context
import ink.abb.pogo.scraper.Settings
import ink.abb.pogo.scraper.Task
import ink.abb.pogo.scraper.util.pokemon.*
import com.pokegoapi.api.player.PlayerProfile

class ReleasePokemon : Task {
	override fun run(bot: Bot, ctx: Context, settings: Settings) {
		if (!settings.shouldAutoTransfer) {
			return
		}
		val groupedPokemon = ctx.api.inventories.pokebank.pokemons.groupBy { it.pokemonId }
		val ignoredPokemon = settings.ignoredPokemon
		val obligatoryTransfer = settings.obligatoryTransfer
		val obligatoryEvolve = settings.obligatoryEvolve
		val minIVPercentage = settings.transferIVThreshold
		val minCP = settings.transferCPThreshold

		groupedPokemon.forEach {
			var shouldEvolveThisPokemonId = true
			val sorted = it.value.sortedByDescending { it.cp }
			for ((index, pokemon) in sorted.withIndex()) {
				// don't drop favourited or nicknamed pokemon
				val isFavourite = pokemon.nickname.isNotBlank() || pokemon.favorite
				if (!isFavourite) {
					val iv = pokemon.getIv()
					val ivPercentage = pokemon.getIvPercentage()
					// never transfer highest rated Pokemon
					if (index > 0) {
						// TODO : Where can I get the cost of evolution to check if it's possible ? (in candy)
						//if (ctx.api.inventories.candyjar.getCandies(pokemon.pokemonFamily)>=???) {...}

						// stop evolving when pokemon is set in ignoredPokemon
						var shouldEvolve = obligatoryEvolve.contains(pokemon.pokemonId.name)
						if (shouldEvolveThisPokemonId && shouldEvolve) {
							var result = pokemon.evolve()
							if (result.isSuccessful) {
								ctx.pokemonStats.third.andIncrement
								var evolved = result.evolvedPokemon
								Log.green("Evolved ${pokemon.pokemonId.name} with CP ${pokemon.cp} into ${evolved.pokemonId.name} with ${evolved.cp} CP : +${result.expAwarded}XP")
							} else {
								var transfertResult = pokemon.transferPokemon()
								Log.yellow("Evolving failed for ${pokemon.pokemonId.name} with CP ${pokemon.cp} : ${result.result} : Transfered instead (${transfertResult})")
								if (transfertResult.toString() == "SUCCESS")
									ctx.pokemonStats.second.andIncrement
								// Cancel evolution of the same kind of pokemon (not enough candy)
								shouldEvolveThisPokemonId = false
							}
						} else {
							// stop releasing when pokemon is set in ignoredPokemon
							if (!ignoredPokemon.contains(pokemon.pokemonId.name)) {
								var shouldRelease = obligatoryTransfer.contains(pokemon.pokemonId.name)
								var reason = ""
								if (shouldRelease) {
									reason = "Obligatory release"
								} else {
									var ivTooLow = false
									var cpTooLow = false

									// never transfer > min IV percentage (unless set to -1)
									if (ivPercentage < minIVPercentage || minIVPercentage == -1) {
										ivTooLow = true
									}
									// never transfer > min CP  (unless set to -1)
									if (pokemon.cp < minCP || minCP == -1) {
										cpTooLow = true
									}
									reason = "CP < $minCP and IV < $minIVPercentage"
									shouldRelease = ivTooLow && cpTooLow
								}
								if (shouldRelease) {
									ctx.pokemonStats.second.andIncrement
									Log.yellow("Going to transfer ${pokemon.pokemonId.name} with " +
											"CP ${pokemon.cp} and IV $iv%; reason: $reason")
									pokemon.transferPokemon()
								}
							}
						}
					}
				}
			}
		}
	}
}
