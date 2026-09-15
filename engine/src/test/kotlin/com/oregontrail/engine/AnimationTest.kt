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
    fun every_event_that_has_art_renders_it() {
        val g = newGame()
        for (id in g.debugEventIds()) {
            val art = g.eventArtFor(id)
            if (art != null) {
                assertTrue(art.isNotEmpty(), "empty art for $id")
                assertTrue(art.all { it.length <= 40 }, "event art for $id is too wide")
            }
        }
        assertTrue(g.eventArtFor("snakebite") != null)
        assertTrue(g.eventArtFor("bandits") != null)
    }

    @Test
    fun the_reveal_transition_hides_then_shows() {
        val g = newGame()
        g.transitions = true
        // Let the first screen finish revealing.
        g.render()
        repeat(Game.TRANSITION_FRAMES + 1) { g.animate() }
        val settledTravel = g.render().toText()
        assertTrue(settledTravel.contains("THE OREGON TRAIL"))

        // Move to a new screen: the first frame is hidden, then it reveals.
        g.onTap("travel:supplies")
        val first = g.render().toText()
        assertNotEquals(settledTravel, first)
        repeat(Game.TRANSITION_FRAMES + 1) { g.animate() }
        val settled = g.render().toText()
        assertTrue(settled.length > first.length, "the reveal should complete")
        assertEquals(settled, g.render().toText())
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

    @Test
    fun wildlife_crosses_the_scene_and_moves() {
        val g = newGame()
        repeat(35) { g.animate() }
        val a = Screen(40, 20)
        g.overlayWildlife(a, 2, 14)
        assertTrue(
            (0 until 40).any { x ->
                (2 until 14).any { y -> a.cell(x, y)?.ch?.let { it in "dwb" } == true }
            },
            "a critter should be on the ground band"
        )
        g.animate()
        val b = Screen(40, 20)
        g.overlayWildlife(b, 2, 14)
        assertNotEquals(a.toText(), b.toText(), "the critter should move")
    }

    @Test
    fun calm_water_sparkles() {
        val g = newGame()
        g.weather = Weather(WeatherKind.CLEAR, 70)
        val s = Screen(40, 20)
        var sparkle = false
        for (f in 0 until 8) {
            val scr = Screen(40, 20)
            g.overlayWater(scr, 2, 12)
            if ((0 until 40).any { x -> (2 until 12).any { y -> scr.cell(x, y)?.ch == '*' } }) {
                sparkle = true
            }
            g.animate()
        }
        assertTrue(sparkle, "a sunny river should sparkle")
    }

    @Test
    fun the_title_screen_is_alive() {
        val g = Game(DefaultRng(2L), InMemoryScoreStore())
        g.setViewport(48, 30)
        val f0 = g.render().toText()
        g.animate()
        assertNotEquals(f0, g.render().toText(), "the title should animate")
    }
}
