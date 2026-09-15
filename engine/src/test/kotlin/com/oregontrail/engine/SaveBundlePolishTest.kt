package com.oregontrail.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SaveBundlePolishTest {

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
}
