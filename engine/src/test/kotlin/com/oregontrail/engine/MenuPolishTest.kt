package com.oregontrail.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MenuPolishTest {

private fun newGame(seed: Long = 1L): Game {
        val g = Game(DefaultRng(seed), InMemoryScoreStore())
        g.setViewport(48, 34)
        return g
    }

    private fun Game.intro() {
        onTap("title:travel"); onTap("prof:0"); onTap("month:0"); onTap("names:go")
        onTap("store:inc:OXEN"); repeat(20) { onTap("store:inc:FOOD") }
        onTap("store:leave"); onTap("notice:continue")
    }

    @Test
    fun menu_rows_advertise_that_they_are_tappable() {
        val g = newGame()
        g.intro()
        val s = g.render()
        assertTrue(s.toText().contains("> 1. Continue on trail"), "menu rows should show a marker")
        assertTrue(s.hotspots.any { it.id == "travel:continue" })
        // The tap target extends past the label for forgiving touches.
        val hot = s.hotspots.first { it.id == "travel:continue" }
        assertTrue(hot.x1 - hot.x0 >= 20, "tap target should be forgiving")
    }

    @Test
    fun title_menu_rows_are_marked_too() {
        val g = newGame()
        assertTrue(g.render().toText().contains("> 1. Travel the trail"))
    }

    @Test
    fun low_food_blinks_a_warning() {
        val g = newGame()
        g.intro()
        g.inventory.food = 10
        var seen = false
        repeat(4) {
            if (g.render().toText().contains("LOW SUPPLIES")) seen = true
            g.animate()
        }
        assertTrue(seen, "a low-food party should be warned")
    }

    @Test
    fun healthy_party_has_no_warning() {
        val g = newGame()
        g.intro()
        g.inventory.food = 1000
        var seen = false
        repeat(4) {
            if (g.render().toText().contains("LOW SUPPLIES")) seen = true
            g.animate()
        }
        assertTrue(!seen)
    }
}
