package com.oregontrail.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BarlowTest {

private fun newGame(seed: Long = 1L, store: ScoreStore = InMemoryScoreStore()): Game {
        val g = Game(DefaultRng(seed), store)
        g.setViewport(48, 34)
        return g
    }

    private fun Game.intro(buy: Boolean = true) {
        onTap("title:travel")
        onTap("prof:0")          // Banker
        onTap("month:0")
        onTap("names:go")
        if (buy) {
            onTap("store:inc:OXEN")
            onTap("store:inc:FOOD")
            onTap("store:leave")
            onTap("notice:continue")
        }
    }

    private fun arrivalSave(): String = buildString {
        append("v=1\nocc=CARPENTER\nmonth=MAY\ndate=1848,8,1\nweather=CLEAR,72\n")
        append("miles=2040\nlandmark=16\npace=STEADY\nrations=FILLING\n")
        append("storeAtFort=false\nsound=true\ncash=900.0\noxen=4\nfood=100\n")
        append("clothing=3\nammo=60\nwheels=1\naxles=0\ntongues=1\nphase=LANDMARK\n")
        for (i in 0..4) append("p$i=Person$i,90,true,\n")
    }

    @Test
    fun barlow_road_is_reachable_and_completes() {
        val g = newGame()
        assertTrue(g.load(arrivalSave()))
        g.onTap("dalles:barlow")
        assertEquals(Phase.BARLOW, g.phase)
        var guard = 0
        while (g.phase == Phase.BARLOW && guard++ < 600) g.barlowTick()
        assertTrue(g.phase == Phase.NOTICE || g.phase == Phase.ARRIVED)
    }

    @Test
    fun barlow_field_completes_and_can_be_damaged() {
        // 0.99: no rocks, road stays put -> a clean climb.
        val calm = ScriptedRng.of(*DoubleArray(400) { 0.99 })
        val safe = BarlowField(20, 10, calm, totalProgress = 20)
        var guard = 0
        while (!safe.done && guard++ < 200) safe.tick()
        assertTrue(safe.done)
        assertTrue(safe.success)
        assertEquals(0, safe.damage)

        // 0.0: constant rocks; steer off the road to take damage.
        val rough = ScriptedRng.of(*DoubleArray(900) { 0.0 })
        val bad = BarlowField(16, 8, rough, totalProgress = 300)
        guard = 0
        while (!bad.done && guard++ < 500) {
            bad.moveLeft()
            bad.tick()
        }
        assertTrue(bad.damage > 0)
    }
}
