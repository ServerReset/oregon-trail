package com.oregontrail.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EndingsTest {

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
    fun arriving_unlocks_achievements_and_records_stats() {
        val store = InMemoryScoreStore()
        val g = Game(DefaultRng(1L), store)
        g.setViewport(48, 34)
        assertTrue(g.load(arrivalSave()))
        g.onTap("dalles:barlow")
        var guard = 0
        while (g.phase == Phase.BARLOW && guard++ < 600) g.barlowTick()
        if (g.phase == Phase.NOTICE) g.onTap("notice:continue")
        assertEquals(Phase.ARRIVED, g.phase)
        assertTrue(Achievements.REACHED_OREGON in g.achievements)
        assertTrue(Achievements.ALL_FIVE_ALIVE in g.achievements)
        assertTrue(Achievements.FRUGAL in g.achievements, "arrived with $900")
        assertEquals(1, store.loadStats().arrivals)
        assertTrue(store.loadStats().bestScore >= g.lastScore)
        assertTrue(store.loadAchievements().contains(Achievements.REACHED_OREGON))
    }

    @Test
    fun dying_records_a_death_stat() {
        val store = InMemoryScoreStore()
        val g = Game(DefaultRng(2L), store)
        g.setViewport(40, 30)
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
        assertEquals(1, store.loadStats().deaths)
        assertTrue(store.loadStats().gamesPlayed >= 1)
    }

    @Test
    fun minigames_survive_a_form_factor_change() {
        val g = newGame(6L)
        g.intro()
        g.inventory.ammo = 100
        g.onTap("travel:hunt")
        assertEquals(Phase.HUNTING, g.phase)
        g.onTap("hunt:shoot")
        repeat(4) { g.huntTick() }
        val shots = g.huntField!!.shotsFired
        // Fold the phone closed mid-hunt.
        g.setViewport(16, 10)
        assertEquals(Phase.HUNTING, g.phase)
        assertEquals(shots, g.huntField!!.shotsFired, "hunt progress should survive a resize")
        assertTrue(g.huntField!!.width <= 16)
        // Unfold again.
        g.setViewport(80, 40)
        assertEquals(Phase.HUNTING, g.phase)
        assertEquals(shots, g.huntField!!.shotsFired)
        g.onTap("hunt:leave")
    }

    @Test
    fun rafting_survives_a_form_factor_change() {
        val save = buildString {
            append("v=1\nocc=BANKER\nmonth=MAY\ndate=1848,8,1\nweather=CLEAR,72\n")
            append("miles=2040\nlandmark=16\npace=STEADY\nrations=FILLING\n")
            append("storeAtFort=false\nsound=true\ncash=100.0\noxen=6\nfood=600\n")
            append("clothing=5\nammo=120\nwheels=1\naxles=1\ntongues=1\nphase=LANDMARK\n")
            for (i in 0..4) append("p$i=Person$i,100,true,\n")
        }
        val g = newGame()
        assertTrue(g.load(save))
        g.onTap("dalles:raft")
        assertEquals(Phase.RAFTING, g.phase)
        repeat(5) { g.raftTick() }
        val progress = g.raftField!!.progress
        g.setViewport(18, 10)
        assertEquals(Phase.RAFTING, g.phase)
        assertEquals(progress, g.raftField!!.progress, "raft progress should survive a resize")
        g.setViewport(100, 40)
        assertEquals(Phase.RAFTING, g.phase)
        assertEquals(progress, g.raftField!!.progress)
        var guard = 0
        while (g.phase == Phase.RAFTING && guard++ < 500) g.raftTick()
        assertTrue(g.phase == Phase.NOTICE || g.phase == Phase.ARRIVED)
    }
}
