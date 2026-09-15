package com.oregontrail.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ThemeSettingsTest {

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
    fun the_theme_setting_cycles_terminal_classic_and_material_you() {
        class Settings : UiSettings {
            override var textScaleIndex: Int = 1
            override var highContrast: Boolean = false
            override var scanlines: Boolean = true
            override var haptics: Boolean = true
            override var themeIndex: Int = 0
        }
        val settings = Settings()
        val g = newGame()
        g.uiSettings = settings
        g.onTap("title:manage")
        val first = g.render().toText()
        assertTrue(first.contains("Theme:"), "management should offer a theme toggle")
        assertTrue(first.contains("Terminal"))

        g.onTap("manage:theme")
        assertEquals(1, settings.themeIndex)
        assertTrue(g.render().toText().contains("Classic"))

        g.onTap("manage:theme")
        assertEquals(2, settings.themeIndex)
        assertTrue(g.render().toText().contains("Material You"))

        g.onTap("manage:theme")
        assertEquals(0, settings.themeIndex)
        assertTrue(g.render().toText().contains("Terminal"))
    }

    @Test
    fun haptics_can_be_toggled() {
        class Settings : UiSettings {
            override var textScaleIndex: Int = 1
            override var highContrast: Boolean = false
            override var scanlines: Boolean = true
            override var haptics: Boolean = true
            override var themeIndex: Int = 0
        }
        val settings = Settings()
        val g = newGame()
        g.uiSettings = settings
        g.onTap("title:manage")
        assertTrue(g.render().toText().contains("Haptics"), "management should offer a haptics toggle")
        g.onTap("manage:haptics")
        assertEquals(false, settings.haptics)
        g.onTap("manage:haptics")
        assertEquals(true, settings.haptics)
    }
}
