package com.oregontrail.engine

import kotlin.test.Test
import kotlin.test.assertTrue

class LayoutTest {

    private val widths = listOf(28, 30, 32, 36, 40, 45, 54, 68, 76, 82)
    private val heights = listOf(16, 18, 20, 24, 30, 40, 60)

    private fun assertHotspotsInside(s: Screen, w: Int, h: Int, label: String) {
        for (hot in s.hotspots) {
            assertTrue(
                hot.x0 in 0 until w && hot.x1 in 0 until w &&
                    hot.y0 in 0 until h && hot.y1 in 0 until h,
                "$label: hotspot ${hot.id} ${hot.x0},${hot.y0}-${hot.x1},${hot.y1} outside ${w}x$h"
            )
        }
    }

    private fun nextAction(g: Game, s: Screen): String? = when (g.phase) {
        Phase.TITLE -> "title:travel"
        Phase.PROFESSION -> "prof:0"
        Phase.MONTH -> "month:0"
        Phase.NAMES -> "names:go"
        Phase.STORE -> if (g.inventory.oxen == 0 && g.inventory.cash >= 20) "store:inc:OXEN" else "store:leave"
        Phase.TRAVEL -> {
            if (g.inventory.food < 300) g.inventory.food = 1200
            "travel:continue"
        }
        Phase.NOTICE -> "notice:continue"
        Phase.MAP -> "map:back"
        Phase.HUNTING -> "hunt:leave"
        Phase.RAFTING -> "raft:right"
        Phase.CHOICE -> s.hotspots.firstOrNull {
            it.id.startsWith("riders:") || it.id.startsWith("trade:")
        }?.id ?: "choice:continue"
        Phase.RIVER -> s.hotspots.firstOrNull { it.id == "river:ferry" }?.id ?: "river:caulk"
        Phase.LANDMARK -> {
            val dalles = s.hotspots.firstOrNull { it.id.startsWith("dalles:") }
            if (dalles != null) {
                // Exercise the rafting finale so it is rendered/validated everywhere.
                s.hotspots.firstOrNull { it.id == "dalles:raft" }?.id
                    ?: s.hotspots.firstOrNull { it.id == "dalles:barlow" }?.id
            } else "land:continue"
        }
        Phase.DEATH, Phase.ARRIVED -> null
        else -> null
    }

    @Test
    fun every_phase_renders_safely_on_every_viewport() {
        for (w in widths) {
            for (h in heights) {
                val g = Game(DefaultRng(97L * w + h), InMemoryScoreStore())
                g.setViewport(w, h)
                var steps = 0
                while (steps++ < 2000) {
                    val s = g.render()
                    assertHotspotsInside(s, w, h, "phase=${g.phase} viewport=${w}x$h step=$steps")
                    val action = nextAction(g, s) ?: break
                    g.onTap(action)
                    // Minigames advance on a timer in the app; drive them here.
                    if (g.phase == Phase.HUNTING) g.huntTick()
                    if (g.phase == Phase.RAFTING) g.raftTick()
                    if (g.phase == Phase.DEATH || g.phase == Phase.ARRIVED) {
                        val end = g.render()
                        assertHotspotsInside(end, w, h, "end viewport=${w}x$h")
                        break
                    }
                }
                assertTrue(
                    steps < 2000,
                    "did not terminate for viewport ${w}x$h: phase=${g.phase} miles=${g.miles} " +
                        "landmark=${g.landmarkIndex} alive=${g.party.count { it.alive }} " +
                        "oxen=${g.inventory.oxen} food=${g.inventory.food}"
                )
            }
        }
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

    @Test
    fun store_buttons_fit_at_minimum_width() {
        val g = Game(DefaultRng(5L), InMemoryScoreStore())
        g.setViewport(28, 20)
        g.onTap("title:travel")
        g.onTap("prof:0")
        g.onTap("month:0")
        g.onTap("names:go")
        val s = g.render()
        val ids = s.hotspots.map { it.id }
        Item.entries.forEach { item ->
            assertTrue("store:inc:${item.name}" in ids, "missing + button for ${item.name} at 28 cols")
            assertTrue("store:dec:${item.name}" in ids, "missing - button for ${item.name} at 28 cols")
        }
        assertHotspotsInside(s, 28, 20, "store@28")
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
