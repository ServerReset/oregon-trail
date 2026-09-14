package com.oregontrail.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Saving and loading must round-trip the whole run at every resumable phase. */
class SaveLoadTest {

    private fun newGame(seed: Long = 1L, store: ScoreStore = InMemoryScoreStore()): Game {
        val g = Game(DefaultRng(seed), store)
        g.setViewport(48, 34)
        return g
    }

    private fun Game.intro(ammo: Boolean = false) {
        onTap("title:travel")
        onTap("prof:1")
        onTap("month:1")
        onTap("names:go")
        onTap("store:inc:OXEN")
        repeat(30) { onTap("store:inc:FOOD") }
        if (ammo) onTap("store:inc:AMMUNITION")
        onTap("store:leave")
        onTap("notice:continue")
    }

    private fun reload(source: Game, store: ScoreStore = InMemoryScoreStore()): Game {
        val fresh = Game(DefaultRng(999L), store)
        fresh.setViewport(48, 34)
        assertTrue(fresh.load(source.save()), "save should load")
        return fresh
    }

    private fun assertSameRun(a: Game, b: Game) {
        assertEquals(a.miles, b.miles)
        assertEquals(a.date.toString(), b.date.toString())
        assertEquals(a.landmarkIndex, b.landmarkIndex)
        assertEquals(a.pace, b.pace)
        assertEquals(a.rations, b.rations)
        assertEquals(a.oxHealth, b.oxHealth)
        assertEquals(a.inventory.cash, b.inventory.cash, 0.001)
        assertEquals(a.inventory.food, b.inventory.food)
        assertEquals(a.inventory.oxen, b.inventory.oxen)
        assertEquals(a.inventory.ammo, b.inventory.ammo)
        assertEquals(a.party.map { it.name to it.health }, b.party.map { it.name to it.health })
        assertEquals(a.journal.size, b.journal.size)
    }

    @Test
    fun round_trip_on_the_trail() {
        val g = newGame(3L)
        g.intro()
        repeat(6) {
            if (g.phase == Phase.TRAVEL) g.onTap("travel:continue")
            if (g.phase == Phase.NOTICE) g.onTap("notice:continue")
        }
        val h = reload(g)
        assertSameRun(g, h)
        // The restored run must be playable.
        if (h.phase == Phase.TRAVEL) h.onTap("travel:continue")
        assertTrue(h.phase != Phase.TITLE)
    }

    @Test
    fun round_trip_at_a_landmark() {
        val g = newGame(5L)
        g.intro()
        var guard = 0
        while (guard++ < 200 && g.phase != Phase.LANDMARK) {
            when (g.phase) {
                Phase.TRAVEL -> g.onTap("travel:continue")
                Phase.NOTICE -> g.onTap("notice:continue")
                Phase.CHOICE -> g.onTap("choice:continue")
                Phase.RIVER -> g.onTap("river:caulk")
                Phase.HUNTING -> g.onTap("hunt:leave")
                else -> break
            }
        }
        assertTrue(g.phase == Phase.LANDMARK || g.phase == Phase.RIVER, "ended at ${g.phase}")
        val h = reload(g)
        assertSameRun(g, h)
        assertEquals(g.phase, h.phase)
    }

    @Test
    fun round_trip_mid_hunt_falls_back_to_travel() {
        val g = newGame(7L)
        g.intro(ammo = true)
        g.onTap("travel:hunt")
        assertEquals(Phase.HUNTING, g.phase)
        g.onTap("hunt:shoot")
        repeat(3) { g.huntTick() }
        val h = reload(g)
        // A hunt cannot be resumed, so it rewinds to the trail safely.
        assertEquals(Phase.TRAVEL, h.phase)
        assertEquals(g.miles, h.miles)
    }

    @Test
    fun round_trip_mid_rafting_falls_back_to_the_dalles() {
        val g = newGame()
        val save = buildString {
            append("v=1\nocc=BANKER\nmonth=MAY\ndate=1848,8,1\nweather=CLEAR,72\n")
            append("miles=2040\nlandmark=16\npace=STEADY\nrations=FILLING\n")
            append("storeAtFort=false\nsound=true\ncash=100.0\noxen=6\nfood=600\n")
            append("clothing=5\nammo=120\nwheels=1\naxles=1\ntongues=1\nphase=LANDMARK\n")
            for (i in 0..4) append("p$i=Person$i,100,true,\n")
        }
        assertTrue(g.load(save))
        g.onTap("dalles:raft")
        assertEquals(Phase.RAFTING, g.phase)
        repeat(4) { g.raftTick() }
        val h = reload(g)
        assertEquals(Phase.LANDMARK, h.phase)
        assertEquals(16, h.landmarkIndex)
    }

    @Test
    fun extended_fields_survive_a_round_trip() {
        val store = InMemoryScoreStore()
        val g = newGame(11L, store)
        g.uiSettings = object : UiSettings {
            override var textScaleIndex = 2
            override var highContrast = true
            override var scanlines = false
        }
        g.difficulty = Difficulty.HARD
        g.intro()
        g.onTap("travel:rest"); g.onTap("rest:3")
        g.onTap("notice:continue")
        g.onTap("travel:save")   // requests a save; state itself is what we test
        g.clearSaveRequest()
        val h = reload(g, store)
        assertSameRun(g, h)
        assertEquals(Difficulty.HARD, h.difficulty)
        assertTrue(h.journal.any { it.text.contains("Rested") })
    }
}
