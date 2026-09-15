package com.oregontrail.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The day/night cycle used by the sky and the ambient background. */
class DaylightTest {

    private fun travelGame(): Game {
        val g = Game(DefaultRng(1L), InMemoryScoreStore())
        g.setViewport(60, 40)
        g.onTap("title:travel"); g.onTap("prof:0"); g.onTap("month:0"); g.onTap("names:go")
        g.onTap("store:inc:OXEN"); repeat(20) { g.onTap("store:inc:FOOD") }
        g.onTap("store:leave"); g.onTap("notice:continue")
        g.weather = Weather(WeatherKind.CLEAR, 70)
        return g
    }

    @Test
    fun the_day_phase_cycles_through_the_day() {
        val g = travelGame()
        assertEquals(0.0, g.dayPhase(), 0.001)
        repeat(40) { g.animate() }
        assertEquals(0.25, g.dayPhase(), 0.001)
        repeat(40) { g.animate() }
        assertEquals(0.5, g.dayPhase(), 0.001)
        repeat(80) { g.animate() }
        assertEquals(0.0, g.dayPhase(), 0.001, "the day should wrap around")
    }

    @Test
    fun fair_weather_ambience_moves_from_day_to_night() {
        val g = travelGame()
        assertEquals(Palette.GREEN, g.render().ambient, "dawn should be green")
        repeat(150) { g.animate() }
        assertEquals(Palette.BLUE, g.render().ambient, "night should be blue")
    }

    @Test
    fun the_sky_is_alive_deep_into_the_day() {
        val g = travelGame()
        val dawn = g.render().toText()
        repeat(80) { g.animate() }
        val noon = g.render().toText()
        repeat(68) { g.animate() }
        val night = g.render().toText()
        assertTrue(dawn != noon || noon != night, "the sky should change with the day")
    }
}
