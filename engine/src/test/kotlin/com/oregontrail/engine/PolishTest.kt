package com.oregontrail.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Tap affordances, save-slot paging/renaming and status warnings. */
class PolishTest {

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

    @Test
    fun save_bundle_round_trips_slots_and_autosave() {
        val slots = listOf(
            SaveSlot("a", "Named save", "Wednesday, March 1, 1848, 12 mi, Banker", 123L, "line1\nline2|pipe\\slash"),
            SaveSlot("b", "Weird | name", "d\\etail\n", 456L, "data|with|pipes\nand\nnewlines")
        )
        val text = SaveBundle.encode(slots, "auto\nsave")
        val bundle = SaveBundle.decode(text)
        assertTrue(bundle != null)
        assertEquals(2, bundle.slots.size)
        assertEquals(slots[0], bundle.slots[0])
        assertEquals(slots[1], bundle.slots[1])
        assertEquals("auto\nsave", bundle.autosave)
        assertNull(SaveBundle.decode("this is not a bundle"))
        assertEquals(0, SaveBundle.decode("OREGON-SAVES-1\n")!!.slots.size)
    }

    @Test
    fun load_screen_offers_overwrite() {
        val g = newGame()
        val snapshot = g.save()
        g.saveSlots = listOf(SaveSlot("s1", "Slot one", "detail", 1L, snapshot))
        g.onTap("title:load")
        assertTrue(g.render().hotspots.any { it.id == "slot:overwrite:s1" })
        g.onTap("slot:overwrite:s1")
        assertEquals("s1", g.requestedOverwriteId)
        g.clearOverwriteRequest()
        assertNull(g.requestedOverwriteId)
    }

    @Test
    fun management_offers_export_and_import() {
        val g = newGame()
        g.onTap("title:manage")
        val s = g.render()
        assertTrue(s.hotspots.any { it.id == "manage:export" }, "export option missing")
        assertTrue(s.hotspots.any { it.id == "manage:import" }, "import option missing")
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
    fun save_slots_page_and_rename() {
        val g = newGame()
        val snapshot = g.save()
        g.saveSlots = (1..20).map {
            SaveSlot("s$it", "Slot $it", "Wednesday, March 1, 1848, $it mi, Banker", it.toLong(), snapshot)
        }
        g.onTap("title:load")
        val page1 = g.render()
        assertTrue(page1.toText().contains("Page 1/"), "should show it is paged")
        assertTrue(page1.hotspots.any { it.id == "slots:next" })

        g.onTap("slots:next")
        assertTrue(g.render().toText() != page1.toText(), "next page should differ")
        assertTrue(g.render().hotspots.any { it.id == "slots:prev" })

        // Paging clamps at the start.
        repeat(4) { g.onTap("slots:prev") }
        assertEquals(0, g.loadPage)
        assertTrue(g.render().hotspots.any { it.id == "slot:rename:s1" })

        g.onTap("slot:rename:s3")
        assertEquals("s3", g.requestedRenameId)
        g.clearRenameRequest()
        assertEquals(null, g.requestedRenameId)
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
