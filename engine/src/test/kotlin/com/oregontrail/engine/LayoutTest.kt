package com.oregontrail.engine

import kotlin.test.Test
import kotlin.test.assertTrue

class LayoutTest {

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
    fun every_phase_renders_safely_on_every_viewport() {
        for (w in widths) {
            for (h in heights) {
                val g = Game(DefaultRng(97L * w + h), InMemoryScoreStore())
                g.setViewport(w, h)
                var steps = 0
                while (steps++ < 2000) {
                    val s = g.render()
                    assertHotspotsInside(s, w, h, "phase=${g.phase} viewport=${w}x$h step=$steps")
                    val action = nextAction(g, s, steps) ?: break
                    g.onTap(action)
                    // Minigames advance on a timer in the app; drive them here.
                    if (g.phase == Phase.HUNTING) g.huntTick()
                    if (g.phase == Phase.RAFTING) g.raftTick()
                    if (g.phase == Phase.BARLOW) g.barlowTick()
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
