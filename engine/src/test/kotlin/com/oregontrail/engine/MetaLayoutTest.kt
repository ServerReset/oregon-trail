package com.oregontrail.engine

import kotlin.test.Test
import kotlin.test.assertTrue

class MetaLayoutTest {

private val widths = listOf(16, 18, 20, 22, 24, 28, 32, 40, 48, 60, 76, 100)

    private val heights = listOf(10, 11, 12, 14, 16, 18, 22, 30, 40, 60)

    private fun assertHotspotsInside(s: Screen, w: Int, h: Int, label: String) {
        for (hot in s.hotspots) {
            assertTrue(
                hot.x0 in 0 until w && hot.x1 in 0 until w &&
                    hot.y0 in 0 until h && hot.y1 in 0 until h,
                "$label: hotspot ${hot.id} ${hot.x0},${hot.y0}-${hot.x1},${hot.y1} outside ${w}x$h"
            )
        }
    }

    private fun nextAction(g: Game, s: Screen, steps: Int): String? = when (g.phase) {
        Phase.TITLE -> "title:travel"
        Phase.PROFESSION -> "prof:0"
        Phase.MONTH -> "month:0"
        Phase.NAMES -> "names:go"
        Phase.STORE -> if (g.inventory.oxen == 0 && g.inventory.cash >= 20) "store:inc:OXEN" else "store:leave"
        Phase.TRAVEL -> {
            if (g.inventory.food < 300) g.inventory.food = 1200
            when {
                steps % 9 == 0 -> "travel:rest"     // leads to the rest CHOICE
                steps % 13 == 0 -> "travel:trade"   // leads to the trade CHOICE
                else -> "travel:continue"
            }
        }
        Phase.NOTICE -> "notice:continue"
        Phase.MAP -> "map:back"
        Phase.HUNTING -> "hunt:leave"
        Phase.RAFTING -> "raft:right"
        Phase.BARLOW -> "barlow:right"
        Phase.CHOICE -> s.hotspots.firstOrNull {
            it.id.startsWith("riders:") || it.id.startsWith("trade:") || it.id.startsWith("rest:")
        }?.id ?: "choice:continue"
        Phase.RIVER -> s.hotspots.firstOrNull { it.id == "river:ferry" }?.id ?: "river:caulk"
        Phase.LANDMARK -> {
            val dalles = s.hotspots.firstOrNull { it.id.startsWith("dalles:") }
            if (dalles != null) {
                // Exercise both endings: the rafting and Barlow Road minigames.
                if (steps % 2 == 0) {
                    s.hotspots.firstOrNull { it.id == "dalles:raft" }?.id
                        ?: s.hotspots.firstOrNull { it.id == "dalles:barlow" }?.id
                } else {
                    s.hotspots.firstOrNull { it.id == "dalles:barlow" }?.id
                        ?: s.hotspots.firstOrNull { it.id == "dalles:raft" }?.id
                }
            } else "land:continue"
        }
        Phase.DEATH, Phase.ARRIVED -> null
        else -> null
    }

    @Test
    fun meta_screens_render_on_every_width() {
        for (w in widths) {
            val g = Game(DefaultRng(9L), InMemoryScoreStore())
            g.setViewport(w, 22)
            g.saveSlots = listOf(
                SaveSlot("s1", "Wagon - Mar 1", "Mar 1 1848, 0 mi, Banker", 1L, g.save())
            )
            // Title now advertises the save slot.
            assertTrue(g.render().hotspots.any { it.id == "title:load" }, "title@$w")

            g.onTap("title:ach")
            assertTrue(g.phase == Phase.ACHIEVEMENTS)
            assertHotspotsInside(g.render(), w, 22, "achievements@$w")
            g.onTap("ach:back")

            g.onTap("title:stats")
            assertTrue(g.phase == Phase.STATS)
            assertHotspotsInside(g.render(), w, 22, "stats@$w")
            g.onTap("stats:back")

            g.onTap("title:load")
            assertTrue(g.phase == Phase.LOAD)
            val load = g.render()
            assertTrue(load.hotspots.any { it.id == "slot:load:s1" }, "load@$w")
            assertHotspotsInside(load, w, 22, "load@$w")
            g.onTap("slots:back")

            // Also the watch title with a slot.
            g.setViewport(w, 10)
            assertTrue(g.render().hotspots.any { it.id == "title:load" }, "watch title@$w")
        }
    }

    @Test
    fun death_and_top_ten_screens_render() {
        val store = InMemoryScoreStore()
        val g = Game(DefaultRng(3L), store)
        g.setViewport(40, 30)
        g.onTap("title:travel")
        g.onTap("prof:2")
        g.onTap("month:0")
        g.onTap("names:go")
        g.onTap("store:inc:OXEN")
        g.onTap("store:leave")
        g.onTap("notice:continue")
        // Kill everyone to exercise the death path.
        var guard = 0
        while (g.phase != Phase.DEATH && guard++ < 400) {
            g.party.forEach { it.health = 1 }
            g.inventory.food = 0
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
        assertTrue(g.phase == Phase.DEATH, "expected death, got ${g.phase}")
        for (w in widths) {
            g.setViewport(w, 24)
            val s = g.render()
            assertTrue(s.toText().isNotBlank())
            assertHotspotsInside(s, w, 24, "death@$w")
        }
        g.onTap("death:topten")
        assertTrue(g.phase == Phase.TOP_TEN)
        for (w in widths) {
            g.setViewport(w, 24)
            assertHotspotsInside(g.render(), w, 24, "topten@$w")
        }
    }
}
