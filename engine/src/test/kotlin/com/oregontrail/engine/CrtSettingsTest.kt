package com.oregontrail.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The CRT filter setting shown on the Settings screen. */
class CrtSettingsTest {

    private class Settings : UiSettings {
        override var textScaleIndex: Int = 1
        override var highContrast: Boolean = false
        override var scanlines: Boolean = true
        override var haptics: Boolean = true
        override var soundEnabled: Boolean = true
        override var crtMode: Int = 1
        override var themeIndex: Int = 1
    }

    private fun game(): Pair<Game, Settings> {
        val ui = Settings()
        val g = Game(DefaultRng(1L), InMemoryScoreStore())
        g.setViewport(48, 34)
        g.uiSettings = ui
        return g to ui
    }

    @Test
    fun the_settings_screen_offers_a_crt_filter() {
        val (g, _) = game()
        g.onTap("title:manage")
        val text = g.render().toText()
        assertTrue(text.contains("CRT filter:"), "settings should show the CRT filter")
        assertTrue(text.contains("Low"), "the default level should be Low")
        assertTrue(g.render().hotspots.any { it.id == "manage:crt" })
    }

    @Test
    fun the_crt_level_cycles_and_reaches_persisted_settings() {
        val (g, ui) = game()
        g.onTap("title:manage")
        assertEquals(1, ui.crtMode)
        g.onTap("manage:crt")
        assertEquals(2, ui.crtMode)
        assertTrue(g.render().toText().contains("High"))
        g.onTap("manage:crt")
        assertEquals(0, ui.crtMode)
        assertTrue(g.render().toText().contains("Off"))
        g.onTap("manage:crt")
        assertEquals(1, ui.crtMode)
    }

    @Test
    fun crt_names_are_stable() {
        val (g, _) = game()
        assertEquals("Off", g.crtName(0))
        assertEquals("Low", g.crtName(1))
        assertEquals("High", g.crtName(2))
        assertEquals("High", g.crtName(99))
    }
}
