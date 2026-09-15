package com.oregontrail.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GravesTest {

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
    fun graves_from_previous_runs_appear_at_landmarks() {
        val store = InMemoryScoreStore()
        store.addGrave(Grave("Old Jed", "cholera", "kansas", "Here lies Old Jed, died of cholera."))
        val g = newGame(1L, store)
        g.onTap("title:travel"); g.onTap("prof:0"); g.onTap("month:0"); g.onTap("names:go")
        g.onTap("store:inc:OXEN"); g.onTap("store:inc:FOOD")
        g.onTap("store:leave"); g.onTap("notice:continue")
        var seen = false
        var guard = 0
        while (guard++ < 60 && !seen) {
            when (g.phase) {
                Phase.TRAVEL -> g.onTap("travel:continue")
                Phase.NOTICE -> {
                    if (g.render().toText().contains("Old Jed")) {
                        seen = true
                        break
                    }
                    g.onTap("notice:continue")
                }
                Phase.CHOICE -> g.onTap("choice:continue")
                Phase.RIVER -> break
                Phase.LANDMARK -> g.onTap("land:continue")
                Phase.HUNTING -> g.onTap("hunt:leave")
                else -> break
            }
        }
        assertTrue(seen, "the Kansas arrival notice should mention the earlier grave")
    }

    @Test
    fun dying_leaves_a_grave_for_future_runs() {
        val store = InMemoryScoreStore()
        val g = Game(DefaultRng(2L), store)
        g.setViewport(40, 30)
        g.onTap("title:travel"); g.onTap("prof:2"); g.onTap("month:0"); g.onTap("names:go")
        g.onTap("store:inc:OXEN")   // no food at all
        g.onTap("store:leave"); g.onTap("notice:continue")
        var guard = 0
        while (guard++ < 300 && g.phase != Phase.DEATH) {
            when (g.phase) {
                Phase.TRAVEL -> g.onTap("travel:continue")
                Phase.NOTICE -> g.onTap("notice:continue")
                Phase.CHOICE -> g.onTap("choice:continue")
                Phase.RIVER -> g.onTap("river:caulk")
                Phase.LANDMARK -> g.onTap("land:continue")
                Phase.HUNTING -> g.onTap("hunt:leave")
                else -> break
            }
        }
        assertEquals(Phase.DEATH, g.phase)
        assertTrue(store.loadGraves().isNotEmpty(), "a death should leave a grave")
        assertEquals(Data.landmarkAt(g.landmarkIndex).id, store.loadGraves().last().landmarkId)
    }
}
