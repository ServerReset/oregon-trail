package com.oregontrail.engine

import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/** The ASCII scenery and its weather overlays. All checks are deterministic. */
class VisualEffectsTest {

    private fun travelGame(cols: Int = 60, rows: Int = 40): Game {
        val g = Game(DefaultRng(1L), InMemoryScoreStore())
        g.setViewport(cols, rows)
        g.onTap("title:travel"); g.onTap("prof:0"); g.onTap("month:0"); g.onTap("names:go")
        g.onTap("store:inc:OXEN"); g.onTap("store:leave"); g.onTap("notice:continue")
        return g
    }

    @Test
    fun travel_scenery_is_ascii_and_bounded() {
        for (miles in 0..2040 step 100) {
            val rows = Landscape.travelScene(miles)
            assertTrue(rows.size in 5..12, "scene at $miles mi had ${rows.size} rows")
            for (row in rows) {
                for (ch in row.text) {
                    assertTrue(ch.toInt() in 32..126, "non-ASCII char ${ch.toInt()} at $miles mi")
                }
            }
            assertTrue(
                sceneWidth(rows) <= 62,
                "scene at $miles mi is ${sceneWidth(rows)} columns wide"
            )
            assertTrue(
                rows.count { it.text.isNotBlank() } >= 3,
                "scene at $miles mi has fewer than 3 non-blank rows"
            )
        }
    }

    @Test
    fun landmark_scenery_covers_every_kind() {
        for (kind in LandmarkKind.values()) {
            val art = Landscape.forKind(kind, 1000)
            assertTrue(art.isNotEmpty(), "no scenery for landmark kind $kind")
        }
    }

    @Test
    fun the_scenery_changes_as_you_travel() {
        val start = Landscape.travelScene(60).joinToString("\n") { it.text }
        val middle = Landscape.travelScene(900).joinToString("\n") { it.text }
        val end = Landscape.travelScene(1950).joinToString("\n") { it.text }
        assertNotEquals(start, middle, "plains and desert scenery should differ")
        assertNotEquals(middle, end, "desert and cascade scenery should differ")
        assertNotEquals(start, end, "the two ends of the trail should look different")
    }

    @Test
    fun rendering_never_crashes_across_weather_and_viewports() {
        val viewports = listOf(32 to 24, 48 to 34, 60 to 48, 100 to 30)
        for ((cols, rows) in viewports) {
            for (kind in WeatherKind.values()) {
                val g = travelGame(cols, rows)
                g.weather = Weather(kind, 60)
                repeat(40) {
                    g.animate()
                    val text = g.render().toText()
                    assertTrue(
                        text.isNotBlank(),
                        "blank frame for $kind at ${cols}x$rows"
                    )
                }
            }
        }
    }

    @Test
    fun rare_effects_appear_over_time() {
        val g = travelGame(60, 40)
        g.weather = Weather(WeatherKind.THUNDERSTORM, 60)
        // Landmark scenery sits at the top of the screen, so its sky band is rows 0..12.
        g.phase = Phase.LANDMARK
        g.landmarkIndex = 0
        var sawGlyph = false
        var guardOk = false
        repeat(120) {
            g.animate()
            val s = g.render()
            if (s.width >= 10 && s.height >= 3) guardOk = true
            outer@ for (y in 0 until minOf(13, s.height)) {
                for (x in 0 until s.width) {
                    val ch = s.cell(x, y)?.ch ?: continue
                    if (ch == '\\' || ch == '/') {
                        sawGlyph = true
                        break@outer
                    }
                }
            }
        }
        // The sky band only receives weather glyphs once the lightning guard holds.
        if (guardOk) {
            assertTrue(sawGlyph, "a thunderstorm should paint bolt/rain glyphs in the sky band")
        }
    }
}
