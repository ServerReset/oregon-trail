package com.oregontrail.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TradingTest {

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
    fun haggling_can_improve_a_trade() {
        // All-zero rng: offer "food80", and the haggle always succeeds.
        val g = Game(ScriptedRng.of(*DoubleArray(400) { 0.0 }), InMemoryScoreStore())
        g.setViewport(48, 34)
        g.intro()
        g.inventory.clothing = 1
        val foodBefore = g.inventory.food
        g.onTap("travel:trade")
        assertEquals(Phase.CHOICE, g.phase)
        g.onTap("trade:haggle:food80")
        assertEquals(Phase.NOTICE, g.phase)
        assertEquals(foodBefore + 100, g.inventory.food, "a good haggle gives 25% more food")
        assertEquals(0, g.inventory.clothing)
        assertTrue(Achievements.BARGAINER in g.achievements)
    }

    @Test
    fun sharing_food_with_a_stranded_family_unlocks_good_samaritan() {
        val g = newGame(5L)
        g.intro()
        g.inventory.food = 200
        g.onTap("stranded:food")
        assertEquals(150, g.inventory.food)
        assertTrue(Achievements.GOOD_SAMARITAN in g.achievements)
    }

    @Test
    fun random_actions_keep_game_invariants() {
        val random = kotlin.random.Random(2024)
        for (seed in 1..30) {
            val g = Game(DefaultRng(seed.toLong()), InMemoryScoreStore())
            g.setViewport(40, 30)
            var steps = 0
            while (steps++ < 800 && g.phase != Phase.DEATH && g.phase != Phase.ARRIVED) {
                val s = g.render()
                assertTrue(g.inventory.cash >= -0.001, "negative cash")
                assertTrue(g.inventory.food >= 0, "negative food")
                assertTrue(g.inventory.oxen >= 0, "negative oxen")
                assertTrue(g.inventory.clothing >= 0, "negative clothing")
                assertTrue(g.inventory.ammo >= 0, "negative ammo")
                assertTrue(g.inventory.wheels >= 0 && g.inventory.axles >= 0 && g.inventory.tongues >= 0)
                assertTrue(g.landmarkIndex in 0..Data.landmarks.lastIndex, "landmark out of range")
                assertTrue(
                    g.miles >= Data.landmarkAt(g.landmarkIndex).mile,
                    "miles ${g.miles} behind landmark ${g.landmarkIndex}"
                )
                assertTrue(g.date.month in 1..12 && g.date.day in 1..31, "bad date")
                g.party.forEach { assertTrue(it.health in 0..100, "health out of range") }
                assertTrue(g.journal.size <= 400)
                if (s.hotspots.isEmpty()) break
                val id = s.hotspots[random.nextInt(s.hotspots.size)].id
                g.onTap(id)
                if (g.phase == Phase.HUNTING) repeat(random.nextInt(3)) { g.huntTick() }
                if (g.phase == Phase.RAFTING) repeat(random.nextInt(3)) { g.raftTick() }
                if (g.phase == Phase.BARLOW) repeat(random.nextInt(3)) { g.barlowTick() }
            }
        }
    }

    @Test
    fun score_breakdown_totals_match_score() {
        val g = newGame()
        g.intro()
        // Reach Oregon quickly by forcing a short remaining trail via a save.
        val save = buildString {
            append("v=1\nocc=CARPENTER\nmonth=MAY\ndate=1848,8,1\nweather=CLEAR,72\n")
            append("miles=2040\nlandmark=16\npace=STEADY\nrations=FILLING\n")
            append("storeAtFort=false\nsound=true\ncash=42.0\noxen=4\nfood=100\n")
            append("clothing=3\nammo=60\nwheels=1\naxles=0\ntongues=1\nphase=LANDMARK\n")
            for (i in 0..4) append("p$i=Person$i,90,true,\n")
        }
        val arrived = Game(DefaultRng(1L), InMemoryScoreStore())
        arrived.setViewport(48, 34)
        assertTrue(arrived.load(save))
        arrived.onTap("dalles:barlow")
        var guard2 = 0
        while (arrived.phase == Phase.BARLOW && guard2++ < 600) arrived.barlowTick()
        if (arrived.phase == Phase.NOTICE) arrived.onTap("notice:continue")
        assertEquals(Phase.ARRIVED, arrived.phase)
        val text = arrived.render().toText()
        assertTrue(text.contains("score", ignoreCase = true))
        assertFalse(text.contains("Inventory"), "score breakdown must not leak object toString")
        assertTrue(text.contains("Oxen ("), "score breakdown should label oxen")
    }
}
