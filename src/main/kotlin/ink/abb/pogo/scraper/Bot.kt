/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper

import com.google.common.util.concurrent.AtomicDouble
import com.pokegoapi.api.PokemonGo
import com.pokegoapi.api.player.PlayerProfile
import ink.abb.pogo.scraper.tasks.*
import ink.abb.pogo.scraper.util.pokemon.getIvPercentage
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.StackPane
import tornadofx.getChildList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.fixedRateTimer
import kotlin.concurrent.thread

class Bot(val api: PokemonGo, val settings: Settings, mainScreen: MainScreen) {

    var ctx = Context(
            api,
            api.playerProfile,
            AtomicDouble(settings.startingLatitude),
            AtomicDouble(settings.startingLongitude),
            AtomicLong(api.playerProfile.stats.experience),
            Pair(AtomicInteger(0), AtomicInteger(0)),
            Pair(AtomicInteger(0), AtomicInteger(0)),
            AtomicBoolean(false),
            mainScreen
    )

    fun run() {
        ctx.screen.tfUsername.text = ctx.profile.username
        ctx.screen.tfTeam.text = "${ctx.profile.team}"
        ctx.screen.tfPokecoin.text = "${ctx.profile.currencies.get(PlayerProfile.Currency.POKECOIN)}"
        ctx.screen.tfStardust.text = "${ctx.profile.currencies.get(PlayerProfile.Currency.STARDUST)}"
        ctx.screen.tfLevel.text = "${ctx.profile.stats.level} / 40"
        ctx.screen.tfXP.text = "${ctx.profile.stats.experience}"
        println("Pokebank ${ctx.api.inventories.pokebank.pokemons.size}/${ctx.profile.pokemonStorage}")
        //println("Inventory bag ${ctx.api.bag}")

        api.inventories.pokebank.pokemons.map { it }.forEach {
            val IV = it.getIvPercentage()
            val text = Label("${it.pokemonId.name} (${it.nickname}), ${it.cp} CP, IV $IV%")
            text.style = "-fx-background-color: #FFFFFFCC"

//            val hbox = HBox()
//            hbox.alignment = Pos.BOTTOM_CENTER
//            hbox.children.addAll(button, text) // button will be left of text

            val image = Image(javaClass.getResourceAsStream("images/${it.pokemonId.number}.png"))
            val iv1 = ImageView(image)

            val stackPane = StackPane()
            stackPane.alignment = Pos.BOTTOM_CENTER
            stackPane.children.addAll(iv1, text) // hbox with button and text on top of image view

            ctx.screen.fpPokeImages.getChildList().add(stackPane)
        }

        val keepalive = GetMapRandomDirection()
        val drop = DropUselessItems()
        val profile = UpdateProfile()
        val catch = CatchOneNearbyPokemon()
        val release = ReleasePokemon()
        val hatchEggs = HatchEggs()

        task(keepalive)
        println("Getting initial pokestops...")
        // TODO: Figure out why pokestops are only showing up the first time api.map.mapObjects is called (???)
        val reply = api.map.mapObjects
        val process = ProcessPokestops(reply.pokestops)

        fixedRateTimer("ProfileLoop", false, 0, 60000, action = {
            thread(block = {
                task(profile)
            })
        })

        fixedRateTimer("BotLoop", false, 0, 5000, action = {
            thread(block = {
                task(keepalive)
                task(catch)
                task(drop)
                task(process)
                task(release)
                task(hatchEggs)
            })
        })
    }

    fun task(task: Task) {
        task.run(this, ctx, settings)
    }
}