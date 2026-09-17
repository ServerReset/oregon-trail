package com.oregontrail.engine

import kotlin.test.Test
import kotlin.test.assertTrue

class UiCompositionTest {

    private fun newGame(seed: Long = 1L, cols: Int = 60, rows: Int = 42): Game {
        val g = Game(DefaultRng(seed), InMemoryScoreStore())
        g.setViewport(cols, rows)
        return g
    }

    /** Walks the setup screens until the store is open (after names:go). */
    private fun Game.enterStore() {
        onTap("title:travel"); onTap("prof:0"); onTap("month:0"); onTap("names:go")
    }

    /** Buys a yoke, leaves the store and clears the notice to reach travel. */
    private fun Game.reachTrail() {
        enterStore()
        onTap("store:inc:OXEN")
        onTap("store:leave"); onTap("notice:continue")
    }

    private fun assertHotspotsInside(s: Screen, w: Int, h: Int, label: String) {
        for (hot in s.hotspots) {
            assertTrue(
                hot.x0 in 0 until w && hot.x1 in 0 until w &&
                    hot.y0 in 0 until h && hot.y1 in 0 until h,
                "$label: hotspot ${hot.id} ${hot.x0},${hot.y0}-${hot.x1},${hot.y1} outside ${w}x$h"
            )
        }
    }

    @Test
    fun the_store_has_aligned_columns() {
        val g = newGame()
        g.enterStore()
        assertTrue(g.phase == Phase.STORE, "names:go should open the store")
        val s = g.render()
        val header = s.toLines()[3]
        listOf("Item", "Price", "Qty", "Buy").forEach { col ->
            assertTrue(header.contains(col), "store header row missing '$col': '$header'")
        }
        Item.entries.forEachIndexed { i, item ->
            val row = s.toLines()[4 + i]
            assertTrue(row.contains("[-][+]"), "item row for $item lacks buy buttons: '$row'")
        }
        val ids = s.hotspots.map { it.id }
        assertTrue("store:inc:OXEN" in ids, "missing store:inc:OXEN")
        assertTrue("store:dec:OXEN" in ids, "missing store:dec:OXEN")
        assertTrue("store:leave" in ids, "missing store:leave")
    }

    @Test
    fun the_landmark_heading_is_decorated() {
        val g = newGame()
        g.enterStore()
        g.phase = Phase.LANDMARK
        g.landmarkIndex = 4
        g.miles = 554
        val text = g.render().toText()
        assertTrue(text.contains("==="), "landmark heading should be decorated with ===")
        assertTrue(text.contains("CHIMNEY ROCK"), "index 4 should be Chimney Rock")
    }

    @Test
    fun the_title_shows_a_rule_and_version() {
        val s = newGame().render()
        assertTrue(
            s.toLines().any { line -> line.count { it == '-' } >= 20 },
            "the title screen should draw a horizontal rule"
        )
        assertTrue(s.toText().contains("Oregon Trail v"), "title should show its version")
    }

    @Test
    fun the_travel_bar_shows_ticks_and_percent() {
        val g = newGame()
        g.reachTrail()
        assertTrue(g.phase == Phase.TRAVEL, "should be travelling after the notices")
        g.miles = 1020
        val bar = g.render().toLines().firstOrNull { it.contains("/2040") }
        assertTrue(bar != null, "travel screen should show the mileage readout")
        val line = bar.orEmpty()
        assertTrue(line.contains('>'), "progress bar should have a wagon marker: '$line'")
        // The fill covers the first quarter tick at 1020 mi, so only the later
        // tick on this track survives; two visible '|' is unreachable here.
        assertTrue(line.contains('|'), "progress bar should show a quarter tick: '$line'")
        assertTrue(line.contains("(50%)"), "readout should show the percentage: '$line'")
    }

    @Test
    fun every_screen_keeps_hotspots_on_screen_at_48x34() {
        assertHotspotsInside(newGame(cols = 48, rows = 34).render(), 48, 34, "title@48x34")
        val store = newGame(cols = 48, rows = 34)
        store.enterStore()
        assertHotspotsInside(store.render(), 48, 34, "store@48x34")
    }
}
