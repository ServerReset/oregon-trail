package com.oregontrail.engine

import kotlin.test.Test
import kotlin.test.assertTrue

class AnimationPolishTest {

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
    fun thunderstorm_flashes_the_sky() {
        val g = newGame()
        g.intro()
        g.weather = Weather(WeatherKind.THUNDERSTORM, 70)
        var flash = false
        var normal = false
        repeat(9) {
            val a = g.render().ambient
            if (a == Palette.BRIGHT_WHITE) flash = true else if (a == Palette.BLUE) normal = true
            g.animate()
        }
        assertTrue(flash, "thunderstorms should flash")
        assertTrue(normal, "thunderstorms should also be dark blue")
    }

    @Test
    fun travel_shows_an_animated_progress_bar() {
        val g = newGame()
        g.intro()
        val text = g.render().toText()
        assertTrue(text.contains("[") && text.contains(">") && text.contains("/2040"),
            "the travel screen should show a progress bar")
    }
}
