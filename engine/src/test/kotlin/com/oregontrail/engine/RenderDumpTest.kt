package com.oregontrail.engine

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RenderDumpTest {

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
                Phase.RAFTING -> game.raftTick()
                Phase.BARLOW -> game.barlowTick()
                else -> break
            }
        }
        if (game.phase == Phase.ARRIVED) dump("ARRIVED")
        out.writeText(sb.toString())
        assertTrue(out.exists())
    }
}
