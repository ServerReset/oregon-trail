package com.oregontrail.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TravelFlowTest {

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
                    Phase.RAFTING -> game.raftTick()
                    Phase.BARLOW -> game.barlowTick()
                    else -> break
                }
            }
            if (game.phase == Phase.ARRIVED) arrived = true
            seed++
        }
        assertTrue(arrived, "Expected at least one seeded playthrough to reach Oregon")
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
}
