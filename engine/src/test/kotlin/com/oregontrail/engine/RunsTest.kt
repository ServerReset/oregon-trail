package com.oregontrail.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RunsTest {

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
    fun resting_for_a_chosen_number_of_days_heals_and_advances_time() {
        val g = newGame(4L)
        g.intro()
        assertTrue(g.phase == Phase.TRAVEL)
        val before = g.date.toString()
        g.onTap("travel:rest")
        assertEquals(Phase.CHOICE, g.phase)
        g.onTap("rest:5")
        assertEquals(Phase.NOTICE, g.phase)
        assertTrue(g.date.toString() != before, "date should advance while resting")
        assertEquals(5, g.journal.last().text.substringAfter("Rested for ").substringBefore(" ").toInt())
    }

    @Test
    fun the_player_can_write_an_epitaph() {
        val store = InMemoryScoreStore()
        val g = Game(DefaultRng(2L), store)
        g.setViewport(48, 34)
        g.onTap("title:travel"); g.onTap("prof:2"); g.onTap("month:0"); g.onTap("names:go")
        g.onTap("store:inc:OXEN")
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
        g.onTap("death:epitaph")
        assertTrue(g.requestedEpitaphEdit)
        g.setEpitaph("Gone to Oregon")
        assertFalse(g.requestedEpitaphEdit)
        assertEquals("Gone to Oregon", g.lastGravestone)
        assertEquals("Gone to Oregon", store.loadGravestone())
    }

    @Test
    fun hard_pace_wears_down_the_oxen() {
        val g = newGame(4L)
        g.intro()
        assertEquals(100, g.oxHealth)
        g.onTap("travel:pace")  // strenuous
        g.onTap("notice:continue")
        g.onTap("travel:pace")  // grueling
        g.onTap("notice:continue")
        var guard = 0
        while (guard++ < 80 && g.phase == Phase.TRAVEL) g.onTap("travel:continue")
        assertTrue(g.oxHealth < 100, "oxen should tire under hard driving, was ${g.oxHealth}")
        assertTrue(g.oxCondition() in listOf("good", "fair", "poor", "failing"))
    }

    @Test
    fun difficulty_cycles_and_persists() {
        val g = newGame()
        g.onTap("title:manage")
        assertEquals(Difficulty.NORMAL, g.difficulty)
        g.onTap("manage:difficulty")
        assertEquals(Difficulty.HARD, g.difficulty)
        g.onTap("notice:continue")
        g.onTap("manage:difficulty")
        assertEquals(Difficulty.EASY, g.difficulty)

        val restored = newGame(7L)
        assertTrue(restored.load(g.save()))
        assertEquals(Difficulty.EASY, restored.difficulty)
    }
}
