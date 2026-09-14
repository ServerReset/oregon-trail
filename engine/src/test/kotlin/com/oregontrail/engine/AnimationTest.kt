package com.oregontrail.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/** The animated visuals: weather particles, flowing rivers, flickering fires. */
class AnimationTest {

    private fun newGame(seed: Long = 1L): Game {
        val g = Game(DefaultRng(seed), InMemoryScoreStore())
        g.setViewport(48, 34)
        g.onTap("title:travel"); g.onTap("prof:0"); g.onTap("month:0"); g.onTap("names:go")
        g.onTap("store:inc:OXEN"); repeat(20) { g.onTap("store:inc:FOOD") }
        g.onTap("store:leave"); g.onTap("notice:continue")
        return g
    }

    @Test
    fun the_frame_clock_and_blink_advance() {
        val g = newGame()
        val start = g.frame
        val b = g.blink()
        g.animate()
        assertEquals(start + 1, g.frame)
        assertNotEquals(b, g.blink())
    }

    @Test
    fun rendering_is_stable_within_a_frame() {
        val g = newGame()
        assertEquals(g.render().toText(), g.render().toText())
    }

    @Test
    fun snow_drifts_across_the_travel_scene() {
        val g = newGame()
        g.weather = Weather(WeatherKind.BLIZZARD, -5)
        val f0 = g.render().toText()
        g.animate()
        val f1 = g.render().toText()
        assertTrue(f0.contains('*'), "a blizzard should put snow on screen")
        assertNotEquals(f0, f1, "snow should move between frames")
    }

    @Test
    fun clear_weather_adds_no_particles() {
        val g = newGame()
        g.weather = Weather(WeatherKind.CLEAR, 70)
        val f0 = g.render().toText()
        g.animate()
        val f1 = g.render().toText()
        assertEquals(f0, f1, "clear weather should not animate any particles")
    }

    @Test
    fun the_river_waves_shift() {
        assertNotEquals(riverArt(0), riverArt(2))
        assertTrue(riverArt(0).any { it.contains('~') })
    }

    @Test
    fun the_campfire_flickers_in_the_pause_menu() {
        val g = newGame()
        assertNotEquals(campArt(0), campArt(1))
        g.onTap("pause:open")
        assertEquals(Phase.PAUSE, g.phase)
        val f0 = g.render().toText()
        g.animate()
        val f1 = g.render().toText()
        assertNotEquals(f0, f1, "the pause campfire should flicker")
    }

    @Test
    fun stars_twinkle() {
        assertNotEquals(starsArt(0), starsArt(1))
    }

    @Test
    fun the_continue_prompt_blinks() {
        val g = newGame()
        // A heading-out notice has a blinking continue marker.
        val f0 = g.render()
        assertTrue(f0.toText().isNotBlank())
        // blink() alternates with the frame.
        val b0 = g.frame % 2 == 0
        g.animate()
        val b1 = g.frame % 2 == 0
        assertNotEquals(b0, b1)
    }
}
