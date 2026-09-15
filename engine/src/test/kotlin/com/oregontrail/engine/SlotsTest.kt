package com.oregontrail.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SlotsTest {

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
    fun save_slots_can_be_listed_and_selected() {
        val g = newGame()  // TITLE
        g.saveSlots = listOf(
            SaveSlot("s1", "Wagon - Mar 1", "Mar 1 1848, 0 mi, Banker", 1L, g.save())
        )
        assertTrue(g.render().hotspots.any { it.id == "title:load" }, "title should offer to load")
        g.onTap("title:load")
        assertEquals(Phase.LOAD, g.phase)
        val screen = g.render()
        assertTrue(screen.toText().contains("Wagon"))
        assertTrue(screen.hotspots.any { it.id == "slot:load:s1" })
        assertTrue(screen.hotspots.any { it.id == "slot:del:s1" })
        g.onTap("slot:load:s1")
        assertEquals("s1", g.requestedLoadId)
        g.clearLoadRequest()
        assertNull(g.requestedLoadId)
    }

    @Test
    fun travel_menu_offers_saving() {
        val g = newGame(3L)
        g.intro()
        g.onTap("travel:save")
        assertTrue(g.requestedSave)
        g.clearSaveRequest()
        assertFalse(g.requestedSave)
    }

    @Test
    fun journal_records_key_events_and_survives_save() {
        val g = newGame(3L)
        g.intro()
        assertTrue(
            g.journal.any { it.text.contains("left Independence") },
            "leaving Independence should be journaled"
        )
        // Travel until at least one more event/landmark happens.
        var guard = 0
        while (g.journal.size < 2 && guard++ < 200) {
            when (g.phase) {
                Phase.TRAVEL -> g.onTap("travel:continue")
                Phase.NOTICE -> g.onTap("notice:continue")
                Phase.CHOICE -> g.onTap("choice:continue")
                Phase.RIVER -> g.onTap("river:caulk")
                Phase.LANDMARK -> g.onTap("land:continue")
                Phase.HUNTING -> g.onTap("hunt:leave")
                else -> break
            }
        }
        assertTrue(g.journal.size >= 2)

        val restored = newGame(99L)
        assertTrue(restored.load(g.save()))
        assertEquals(g.journal.size, restored.journal.size)
        assertEquals(g.journal.last().text, restored.journal.last().text)
    }

    @Test
    fun journal_paginates_when_it_is_long() {
        val save = buildString {
            append("v=1\nocc=BANKER\nmonth=MAY\ndate=1848,6,1\nweather=CLEAR,72\n")
            append("miles=300\nlandmark=3\npace=STEADY\nrations=FILLING\n")
            append("storeAtFort=false\nsound=true\ncash=100.0\noxen=4\nfood=500\n")
            append("clothing=3\nammo=80\nwheels=1\naxles=1\ntongues=1\nphase=TRAVEL\n")
            for (i in 0..4) append("p$i=Person$i,100,true,\n")
            for (i in 0 until 30) append("j$i=May ${1 + (i % 28)}|Entry number $i on the trail.\n")
        }
        val g = newGame()
        assertTrue(g.load(save))
        g.onTap("travel:journal")
        assertTrue(g.phase == Phase.JOURNAL)
        val page1 = g.render().toText()
        assertTrue(page1.contains("Page 1 of"))
        g.onTap("journal:next")
        val page2 = g.render().toText()
        assertTrue(page2 != page1, "next page should differ")
        assertTrue(page2.contains("Page 2 of"))
    }
}
