package com.oregontrail.engine

import kotlin.test.Test
import kotlin.test.assertTrue

class WatchLayoutTest {

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
    fun watch_sized_screens_stay_playable() {
        val sizes = listOf(16 to 10, 20 to 12, 24 to 14, 18 to 13)
        for ((w, h) in sizes) {
            val g = Game(DefaultRng(21L), InMemoryScoreStore())
            g.setViewport(w, h)
            // Title
            assertTrue(g.render().hotspots.any { it.id == "title:travel" }, "title@${w}x$h")
            g.onTap("title:travel")
            g.onTap("prof:0")
            g.onTap("month:0")
            // Name list + start
            val names = g.render()
            assertTrue(names.hotspots.any { it.id == "names:go" }, "names@${w}x$h")
            assertHotspotsInside(names, w, h, "names@${w}x$h")
            g.onTap("names:go")
            // Store: buy one ox, then leave
            val store = g.render()
            assertTrue(store.hotspots.any { it.id == "store:inc:OXEN" }, "store buy@${w}x$h")
            assertTrue(store.hotspots.any { it.id == "store:leave" }, "store leave@${w}x$h")
            assertHotspotsInside(store, w, h, "store@${w}x$h")
            g.onTap("store:inc:OXEN")
            g.onTap("store:leave")
            g.onTap("notice:continue")
            // Travel screen keeps its core actions
            val travel = g.render()
            assertTrue(travel.hotspots.any { it.id == "travel:continue" }, "travel go@${w}x$h")
            assertHotspotsInside(travel, w, h, "travel@${w}x$h")
        }
    }
}
