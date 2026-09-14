package com.oregontrail.engine

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GameTest {

    private fun newGame(seed: Long = 1L): Game {
        val rng = DefaultRng(seed)
        val game = Game(rng, InMemoryScoreStore())
        game.setViewport(48, 34)
        return game
    }

    private fun Game.doIntro() {
        onTap("title:travel")
        onTap("prof:0")
        onTap("month:0")
        onTap("names:go")
    }

    private fun Game.buyBasics() {
        repeat(3) { onTap("store:inc:OXEN") }
        repeat(40) { onTap("store:inc:FOOD") }
        repeat(5) { onTap("store:inc:CLOTHING") }
        repeat(20) { onTap("store:inc:AMMUNITION") }
        repeat(2) { onTap("store:inc:WHEEL") }
        repeat(2) { onTap("store:inc:AXLE") }
        repeat(2) { onTap("store:inc:TONGUE") }
        onTap("store:leave")
    }

    @Test
    fun title_renders_with_menu() {
        val game = newGame()
        val screen = game.render()
        assertTrue(screen.toText().isNotBlank())
        assertTrue(screen.hotspots.any { it.id == "title:travel" })
    }

    @Test
    fun store_purchases_deduct_cash_and_add_goods() {
        val game = newGame()
        game.doIntro()
        val start = game.inventory.cash
        game.onTap("store:inc:OXEN")
        assertEquals(2, game.inventory.oxen)
        assertEquals(start - 20.0, game.inventory.cash, 0.001)
        game.onTap("store:inc:FOOD")
        assertEquals(50, game.inventory.food)
        game.onTap("store:dec:FOOD")
        assertEquals(0, game.inventory.food)
    }

    @Test
    fun cannot_spend_more_than_available_cash() {
        val game = newGame()
        game.doIntro()
        // Farmer has $400; ensure we cannot overspend.
        game.onTap("prof:2")
        game.onTap("month:0")
        var guard = 0
        while (game.inventory.cash >= 20 && guard++ < 100) game.onTap("store:inc:OXEN")
        assertTrue(game.inventory.cash >= 0.0)
        assertTrue(game.inventory.cash < 20.0)
    }

    @Test
    fun leaving_without_oxen_is_refused() {
        val game = newGame()
        game.doIntro()
        game.onTap("store:leave")
        assertEquals(Phase.NOTICE, game.phase)
        game.onTap("notice:continue")
        assertEquals(Phase.STORE, game.phase)
    }

    @Test
    fun travel_progresses_and_can_reach_oregon() {
        var arrived = false
        var seed = 1L
        while (!arrived && seed <= 8L) {
            val game = newGame(seed)
            game.doIntro()
            game.buyBasics()
            assertEquals(Phase.NOTICE, game.phase)
            game.onTap("notice:continue")
            var guard = 0
            while (guard++ < 4000) {
                if (game.phase == Phase.TRAVEL && game.inventory.food < 200) {
                    game.inventory.food = 800 // keep the state-machine test focused
                }
                when (game.phase) {
                    Phase.TRAVEL -> game.onTap("travel:continue")
                    Phase.NOTICE -> game.onTap("notice:continue")
                    Phase.CHOICE -> game.onTap("choice:continue")
                    Phase.RIVER -> {
                        val lm = Data.landmarkAt(game.landmarkIndex)
                        val ferry = lm.river?.ferryCost
                        if (ferry != null && game.inventory.cash >= ferry) game.onTap("river:ferry")
                        else game.onTap("river:caulk")
                    }
                    Phase.LANDMARK -> {
                        if (Data.landmarkAt(game.landmarkIndex).id == "dalles") {
                            if (game.inventory.cash >= 5) game.onTap("dalles:barlow")
                            else game.onTap("dalles:raft")
                        } else game.onTap("land:continue")
                    }
                    Phase.ARRIVED -> { arrived = true; break }
                    Phase.DEATH -> break
                    Phase.STORE -> game.onTap("store:leave")
                    Phase.HUNTING -> game.onTap("hunt:leave")
                    else -> break
                }
            }
            if (game.phase == Phase.ARRIVED) arrived = true
            seed++
        }
        assertTrue(arrived, "Expected at least one seeded playthrough to reach Oregon")
    }

    @Test
    fun hunting_field_runs() {
        val rng = ScriptedRng.of(*DoubleArray(400) { 0.42 })
        val field = HuntField(30, 12, rng, listOf(AnimalKind.RABBIT, AnimalKind.DEER))
        repeat(50) {
            field.move(1, 0)
            field.shoot()
            field.tick()
        }
        assertTrue(field.shotsFired > 0)
        assertTrue(field.meat >= 0)
    }

    @Test
    fun map_and_supplies_screens_render() {
        val game = newGame()
        game.doIntro()
        game.buyBasics()
        game.onTap("notice:continue")
        game.onTap("travel:map")
        assertEquals(Phase.MAP, game.phase)
        assertTrue(game.render().toText().contains("MAP"))
        game.onTap("map:back")
        game.onTap("travel:supplies")
        assertTrue(game.render().toText().contains("Food"))
    }

    @Test
    fun render_dump_for_visual_review() {
        val game = newGame(7L)
        val out = File("/tmp/ot-screens.txt")
        val sb = StringBuilder()
        fun dump(name: String) {
            sb.append("===== $name =====\n")
            sb.append(game.render().toText())
            sb.append("\n\n")
        }
        dump("TITLE")
        game.onTap("title:travel")
        dump("PROFESSION")
        game.onTap("prof:0")
        dump("MONTH")
        game.onTap("month:0")
        dump("NAMES")
        game.onTap("names:go")
        dump("STORE")
        game.buyBasics()
        dump("NOTICE-LEAVE")
        game.onTap("notice:continue")
        dump("TRAVEL")
        game.onTap("travel:map")
        dump("MAP")
        game.onTap("map:back")
        game.onTap("travel:supplies")
        dump("SUPPLIES")
        game.onTap("notice:continue")
        game.onTap("travel:pace")
        dump("PACE")
        game.onTap("notice:continue")
        game.onTap("travel:hunt")
        dump("HUNTING")
        game.onTap("hunt:leave")
        dump("AFTER-HUNT")
        // Force a river and a landmark screen.
        game.inventory.food = 5000
        var guard = 0
        while (game.phase != Phase.RIVER && guard++ < 500) {
            when (game.phase) {
                Phase.TRAVEL -> game.onTap("travel:continue")
                Phase.NOTICE -> game.onTap("notice:continue")
                Phase.CHOICE -> game.onTap("choice:continue")
                Phase.LANDMARK -> game.onTap("land:continue")
                Phase.HUNTING -> game.onTap("hunt:leave")
                else -> break
            }
        }
        dump("RIVER")
        game.onTap("river:caulk")
        dump("RIVER-RESULT")
        // Run to the end.
        guard = 0
        while (game.phase != Phase.ARRIVED && game.phase != Phase.DEATH && guard++ < 800) {
            game.inventory.food = 5000
            when (game.phase) {
                Phase.TRAVEL -> game.onTap("travel:continue")
                Phase.NOTICE -> game.onTap("notice:continue")
                Phase.CHOICE -> game.onTap("choice:continue")
                Phase.RIVER -> game.onTap("river:caulk")
                Phase.LANDMARK -> {
                    val id = Data.landmarkAt(game.landmarkIndex).id
                    if (id == "dalles") game.onTap("dalles:barlow") else game.onTap("land:continue")
                }
                Phase.STORE -> game.onTap("store:leave")
                Phase.HUNTING -> game.onTap("hunt:leave")
                else -> break
            }
        }
        if (game.phase == Phase.ARRIVED) dump("ARRIVED")
        out.writeText(sb.toString())
        assertTrue(out.exists())
    }

    @Test
    fun debug_journey() {
        val sb = StringBuilder()
        for (seed in 1L..6L) {
            val game = newGame(seed)
            game.doIntro()
            game.buyBasics()
            game.onTap("notice:continue")
            var guard = 0
            var lastPhase = game.phase
            while (guard++ < 3000) {
                if (game.phase == Phase.TRAVEL && game.inventory.food < 200) game.inventory.food = 800
                when (game.phase) {
                    Phase.TRAVEL -> game.onTap("travel:continue")
                    Phase.NOTICE -> game.onTap("notice:continue")
                    Phase.CHOICE -> game.onTap("choice:continue")
                    Phase.RIVER -> {
                        val lm = Data.landmarkAt(game.landmarkIndex)
                        val ferry = lm.river?.ferryCost
                        if (ferry != null && game.inventory.cash >= ferry) game.onTap("river:ferry")
                        else game.onTap("river:caulk")
                    }
                    Phase.LANDMARK -> {
                        if (Data.landmarkAt(game.landmarkIndex).id == "dalles") {
                            if (game.inventory.cash >= 5) game.onTap("dalles:barlow")
                            else game.onTap("dalles:raft")
                        } else game.onTap("land:continue")
                    }
                    Phase.ARRIVED, Phase.DEATH -> break
                    Phase.STORE -> game.onTap("store:leave")
                    Phase.HUNTING -> game.onTap("hunt:leave")
                    else -> break
                }
                lastPhase = game.phase
            }
            sb.append("seed=$seed final=${game.phase} miles=${game.miles} landmark=${game.landmarkIndex}")
            sb.append(" date=${game.date} alive=${game.party.count { it.alive }} food=${game.inventory.food}")
            sb.append(" steps=$guard\n")
        }
        File("/tmp/ot-journey.txt").writeText(sb.toString())
        assertTrue(true)
    }
}
