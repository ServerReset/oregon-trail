package com.oregontrail.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Every event and every branch must be reachable and safe. */
class EventCoverageTest {

    private fun readyGame(): Game {
        val g = Game(DefaultRng(1L), InMemoryScoreStore())
        g.setViewport(48, 34)
        g.inventory.food = 500
        g.inventory.ammo = 200
        g.inventory.oxen = 4
        g.inventory.clothing = 5
        g.inventory.cash = 200.0
        g.inventory.wheels = 1
        g.inventory.axles = 1
        g.inventory.tongues = 1
        return g
    }

    private fun playable(seed: Long = 1L): Game {
        val g = Game(DefaultRng(seed), InMemoryScoreStore())
        g.setViewport(48, 34)
        g.onTap("title:travel"); g.onTap("prof:0"); g.onTap("month:0"); g.onTap("names:go")
        g.onTap("store:inc:OXEN"); g.onTap("store:inc:FOOD"); g.onTap("store:inc:AMMUNITION")
        g.onTap("store:leave"); g.onTap("notice:continue")
        return g
    }

    @Test
    fun every_event_is_safe_and_reported() {
        val choiceEvents = setOf("riders", "stranded")
        val ids = readyGame().debugEventIds()
        assertTrue(ids.isNotEmpty())
        assertEquals(ids.size, ids.toSet().size, "event ids should be unique")
        for (id in ids) {
            val g = readyGame()
            val msgs = g.debugFireEvent(id)
            assertTrue(
                g.inventory.food >= 0 && g.inventory.ammo >= 0 && g.inventory.oxen >= 0 &&
                    g.inventory.clothing >= 0 && g.inventory.cash >= -0.001,
                "event $id produced negative stock"
            )
            if (id in choiceEvents) {
                assertEquals(Phase.CHOICE, g.phase, "$id should open a choice screen")
            } else {
                assertTrue(msgs.isNotEmpty(), "event $id produced no messages")
            }
        }
    }

    private fun atRiver(): Game {
        val g = playable(9L)
        var guard = 0
        while (guard++ < 120 && g.phase != Phase.RIVER) {
            when (g.phase) {
                Phase.TRAVEL -> g.onTap("travel:continue")
                Phase.NOTICE -> g.onTap("notice:continue")
                Phase.CHOICE -> g.onTap("choice:continue")
                Phase.LANDMARK -> g.onTap("land:continue")
                Phase.HUNTING -> g.onTap("hunt:leave")
                else -> break
            }
        }
        return g
    }

    @Test
    fun every_river_option_resolves() {
        for (option in listOf("river:ford", "river:caulk", "river:ferry", "river:guide", "river:wait")) {
            val g = atRiver()
            if (g.phase != Phase.RIVER) continue
            val river = Data.landmarkAt(g.landmarkIndex).river ?: continue
            if (option == "river:guide" && river.guideCost == null) continue
            g.onTap(option)
            assertTrue(
                g.phase == Phase.NOTICE || g.phase == Phase.RIVER || g.phase == Phase.TRAVEL,
                "$option left phase at ${g.phase}"
            )
        }
    }

    private fun atDalles(): Game {
        val g = Game(DefaultRng(1L), InMemoryScoreStore())
        g.setViewport(48, 34)
        val save = buildString {
            append("v=1\nocc=BANKER\nmonth=MAY\ndate=1848,8,1\nweather=CLEAR,72\n")
            append("miles=2040\nlandmark=16\npace=STEADY\nrations=FILLING\n")
            append("storeAtFort=false\nsound=true\ncash=100.0\noxen=6\nfood=600\n")
            append("clothing=5\nammo=120\nwheels=1\naxles=1\ntongues=1\nphase=LANDMARK\n")
            for (i in 0..4) append("p$i=Person$i,100,true,\n")
        }
        assertTrue(g.load(save))
        return g
    }

    @Test
    fun every_ending_option_resolves() {
        atDalles().let { g -> g.onTap("dalles:barlow"); assertEquals(Phase.BARLOW, g.phase) }
        atDalles().let { g -> g.onTap("dalles:raft"); assertEquals(Phase.RAFTING, g.phase) }
        atDalles().let { g ->
            g.onTap("dalles:portage")
            assertTrue(g.phase != Phase.LANDMARK, "portage should advance the game")
        }
        atDalles().let { g ->
            g.onTap("dalles:wait")
            assertTrue(g.phase == Phase.NOTICE || g.phase == Phase.LANDMARK)
        }
    }

    @Test
    fun every_landmark_menu_option_resolves() {
        val g = playable(4L)
        // Walk to the first landmark (Fort Kearney) via the notice.
        var guard = 0
        while (guard++ < 120 && g.phase != Phase.LANDMARK) {
            when (g.phase) {
                Phase.TRAVEL -> g.onTap("travel:continue")
                Phase.NOTICE -> g.onTap("notice:continue")
                Phase.CHOICE -> g.onTap("choice:continue")
                Phase.RIVER -> g.onTap("river:caulk")
                Phase.HUNTING -> g.onTap("hunt:leave")
                else -> break
            }
        }
        if (g.phase == Phase.LANDMARK) {
            for (option in listOf(
                "land:supplies", "land:map", "land:rest", "land:talk", "land:fact", "land:hunt"
            )) {
                val fresh = playable(4L)
                var gr = 0
                while (gr++ < 120 && fresh.phase != Phase.LANDMARK) {
                    when (fresh.phase) {
                        Phase.TRAVEL -> fresh.onTap("travel:continue")
                        Phase.NOTICE -> fresh.onTap("notice:continue")
                        Phase.CHOICE -> fresh.onTap("choice:continue")
                        Phase.RIVER -> fresh.onTap("river:caulk")
                        else -> break
                    }
                }
                if (fresh.phase != Phase.LANDMARK) continue
                fresh.onTap(option)
                assertTrue(fresh.phase != Phase.LANDMARK || option == "land:cutoff", "$option did nothing")
            }
        }
    }
}
