package com.oregontrail.engine

import kotlin.test.Test
import kotlin.test.assertTrue

class SmallScreenLayoutTest {

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
    fun cover_screens_and_watches_render_safely() {
        // Folded-phone cover displays (tall and narrow) and a watch-like square.
        val coverSizes = listOf(20 to 40, 22 to 44, 24 to 36, 26 to 48, 30 to 24, 96 to 20)
        for ((w, h) in coverSizes) {
            val g = Game(DefaultRng(31L * w + h), InMemoryScoreStore())
            g.setViewport(w, h)
            val title = g.render()
            assertTrue(title.hotspots.any { it.id == "title:travel" }, "title@${w}x$h")
            assertHotspotsInside(title, w, h, "title@${w}x$h")

            g.onTap("title:travel"); g.onTap("prof:0"); g.onTap("month:0"); g.onTap("names:go")
            assertHotspotsInside(g.render(), w, h, "store@${w}x$h")
            g.onTap("store:inc:OXEN"); g.onTap("store:leave"); g.onTap("notice:continue")
            val travel = g.render()
            assertTrue(travel.hotspots.any { it.id == "travel:continue" }, "travel@${w}x$h")
            assertHotspotsInside(travel, w, h, "travel@${w}x$h")

            g.onTap("travel:journal")
            assertHotspotsInside(g.render(), w, h, "journal@${w}x$h")
            g.onTap("journal:back")
            g.onTap("travel:map")
            assertHotspotsInside(g.render(), w, h, "map@${w}x$h")
            g.onTap("map:back")

            g.onTap("pause:open")
            assertHotspotsInside(g.render(), w, h, "pause@${w}x$h")
        }
    }

    @Test
    fun compact_notice_text_does_not_overlap_the_prompt() {
        val g = Game(DefaultRng(1L), InMemoryScoreStore())
        g.setViewport(20, 10)
        g.onTap("title:travel"); g.onTap("prof:0"); g.onTap("month:0"); g.onTap("names:go")
        g.onTap("store:inc:OXEN"); g.onTap("store:leave")
        assertTrue(g.phase == Phase.NOTICE, "expected the heading-out notice")
        val lines = g.render().toLines()
        assertTrue(lines.size >= 10)
        assertTrue(
            lines[9].trim() == "[>]",
            "the continue prompt row should hold only the marker, was '${lines[9]}'"
        )
    }

    @Test
    fun journal_and_management_render_on_small_screens() {
        for (w in widths) {
            val g = Game(DefaultRng(11L), InMemoryScoreStore())
            g.setViewport(w, 22)
            g.onTap("title:manage")
            assertHotspotsInside(g.render(), w, 22, "management@$w")
            g.onTap("manage:back")

            g.onTap("title:travel"); g.onTap("prof:0"); g.onTap("month:0"); g.onTap("names:go")
            g.onTap("store:inc:OXEN"); g.onTap("store:leave"); g.onTap("notice:continue")
            var guard = 0
            while (guard++ < 60 && g.phase != Phase.TRAVEL) {
                when (g.phase) {
                    Phase.NOTICE -> g.onTap("notice:continue")
                    Phase.CHOICE -> g.onTap("choice:continue")
                    Phase.RIVER -> g.onTap("river:caulk")
                    Phase.LANDMARK -> g.onTap("land:continue")
                    Phase.HUNTING -> g.onTap("hunt:leave")
                    else -> break
                }
            }
            g.onTap("travel:journal")
            assertTrue(g.phase == Phase.JOURNAL, "journal should open at $w cols")
            assertHotspotsInside(g.render(), w, 22, "journal@$w")
            g.onTap("journal:back")
            assertTrue(g.phase == Phase.TRAVEL)
        }
    }
}
