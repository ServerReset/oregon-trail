package com.oregontrail.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class WeatherAnimationTest {

private fun newGame(seed: Long = 1L): Game {
        val g = Game(DefaultRng(seed), InMemoryScoreStore())
        g.setViewport(48, 34)
        g.onTap("title:travel"); g.onTap("prof:0"); g.onTap("month:0"); g.onTap("names:go")
        g.onTap("store:inc:OXEN"); repeat(20) { g.onTap("store:inc:FOOD") }
        g.onTap("store:leave"); g.onTap("notice:continue")
        return g
    }

    @Test
    fun weather_particles_are_coloured() {
        val g = newGame()
        g.weather = Weather(WeatherKind.SNOW, -5)
        val s = g.render()
        var found = false
        for (y in 0 until s.height) {
            for (x in 0 until s.width) {
                val c = s.cell(x, y) ?: continue
                if (c.ch == '*') {
                    assertEquals(Palette.BRIGHT_WHITE, c.fg, "snow should be bright")
                    found = true
                }
            }
        }
        assertTrue(found, "snow should be on screen")
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
    fun clear_weather_has_no_snow_and_a_living_sky() {
        val g = newGame()
        g.weather = Weather(WeatherKind.CLEAR, 70)
        assertFalse(g.render().toText().contains('*'), "clear weather should not snow")
        // The sky (sun rays, birds) still animates even in fair weather.
        val f0 = g.render().toText()
        g.animate()
        assertNotEquals(f0, g.render().toText())
    }

    @Test
    fun the_river_waves_shift() {
        assertNotEquals(riverArt(0), riverArt(2))
        assertTrue(riverArt(0).any { it.contains('~') })
    }
}
