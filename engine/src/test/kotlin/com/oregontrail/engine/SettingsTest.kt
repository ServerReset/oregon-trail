package com.oregontrail.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsTest {

private fun newGame(seed: Long = 1L, store: ScoreStore = InMemoryScoreStore()): Game {
        val g = Game(DefaultRng(seed), store)
        g.setViewport(48, 34)
        return g
    }

    private fun Game.intro(buy: Boolean = true) {
        onTap("title:travel")
        onTap("prof:0")          // Banker
        onTap("month:0")
        onTap("names:go")
        if (buy) {
            onTap("store:inc:OXEN")
            onTap("store:inc:FOOD")
            onTap("store:leave")
            onTap("notice:continue")
        }
    }

    private fun arrivalSave(): String = buildString {
        append("v=1\nocc=CARPENTER\nmonth=MAY\ndate=1848,8,1\nweather=CLEAR,72\n")
        append("miles=2040\nlandmark=16\npace=STEADY\nrations=FILLING\n")
        append("storeAtFort=false\nsound=true\ncash=900.0\noxen=4\nfood=100\n")
        append("clothing=3\nammo=60\nwheels=1\naxles=0\ntongues=1\nphase=LANDMARK\n")
        for (i in 0..4) append("p$i=Person$i,90,true,\n")
    }

    @Test
    fun management_toggles_presentation_settings() {
        class TestSettings : UiSettings {
            override var textScaleIndex: Int = 1
            override var highContrast: Boolean = false
            override var scanlines: Boolean = true
        }
        val settings = TestSettings()
        val g = newGame()
        g.uiSettings = settings
        g.onTap("title:manage")
        g.onTap("manage:textsize")
        assertEquals(2, settings.textScaleIndex)
        g.onTap("manage:textsize")
        assertEquals(0, settings.textScaleIndex)
        g.onTap("manage:contrast")
        assertTrue(settings.highContrast)
        g.onTap("manage:scanlines")
        assertFalse(settings.scanlines)
        // The management screen must advertise the settings.
        val text = g.render().toText()
        assertTrue(text.contains("Text size"))
        assertTrue(text.contains("High contrast"))
    }

    @Test
    fun settings_are_easy_to_find_and_audible() {
        class TestSettings : UiSettings {
            override var textScaleIndex: Int = 1
            override var highContrast: Boolean = false
            override var scanlines: Boolean = true
            override var haptics: Boolean = true
            override var soundEnabled: Boolean = true
            override var themeIndex: Int = 0
        }
        val g = newGame()
        g.uiSettings = TestSettings()
        // The title advertises settings by name.
        assertTrue(g.render().toText().contains("Settings and options"))

        g.onTap("title:manage")
        val settings = g.render()
        val text = settings.toText()
        assertTrue(text.contains("SETTINGS") )
        assertTrue(text.contains("Sound is ON") )
        assertTrue(text.contains("Theme:"))
        assertTrue(settings.hotspots.any { it.id == "manage:testsound" })

        // The test-sound action queues a cue.
        g.onTap("manage:testsound")
        assertEquals(Sound.GOOD, g.pendingSound)
    }

    @Test
    fun the_sound_toggle_persists_through_ui_settings() {
        class TestSettings : UiSettings {
            override var textScaleIndex: Int = 1
            override var highContrast: Boolean = false
            override var scanlines: Boolean = true
            override var haptics: Boolean = true
            override var soundEnabled: Boolean = true
            override var themeIndex: Int = 0
        }
        val ui = TestSettings()
        val g = newGame()
        g.uiSettings = ui
        g.onTap("title:manage")
        g.onTap("manage:sound")
        assertFalse(g.soundEnabled)
        assertFalse(ui.soundEnabled, "the sound choice should reach the persisted settings")
        g.onTap("manage:sound")
        assertTrue(ui.soundEnabled)
    }
}
