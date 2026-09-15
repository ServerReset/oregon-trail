package com.oregontrail.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The shareable ASCII postcard. */
class PostcardTest {

    private fun travelGame(): Game {
        val g = Game(DefaultRng(1L), InMemoryScoreStore())
        g.setViewport(48, 34)
        g.onTap("title:travel"); g.onTap("prof:0"); g.onTap("month:0"); g.onTap("names:go")
        g.onTap("store:inc:OXEN"); repeat(20) { g.onTap("store:inc:FOOD") }
        g.onTap("store:leave"); g.onTap("notice:continue")
        return g
    }

    @Test
    fun the_postcard_is_a_bordered_card_with_journey_details() {
        val g = travelGame()
        val card = g.postcard()
        assertTrue(card.size >= 12, "the card should have a body")
        val width = card.first().length
        assertTrue(width in 30..60, "card width = $width")
        assertTrue(card.all { it.length == width }, "all rows should share the width")
        assertTrue(card.first().startsWith("+") && card.last().startsWith("+"))
        val text = card.joinToString("\n")
        assertTrue(text.contains("THE OREGON TRAIL"))
        assertTrue(text.contains("Miles") || text.contains("Arrived"))
        assertTrue(text.contains("Banker"))
    }

    @Test
    fun the_postcard_changes_with_the_ending() {
        val win = travelGame()
        win.phase = Phase.ARRIVED
        win.lastScore = 3456
        assertTrue(win.postcardText().contains("Arrived in the Willamette Valley"))
        assertTrue(win.postcardText().contains("3456"))

        val lost = travelGame()
        lost.phase = Phase.DEATH
        lost.deathCause = "dysentery"
        assertTrue(lost.postcardText().contains("Died of dysentery"))
    }

    @Test
    fun sharing_is_requested_from_the_menus() {
        val g = travelGame()
        g.onTap("pause:open")
        assertEquals(Phase.PAUSE, g.phase)
        assertTrue(g.render().hotspots.any { it.id == "pause:postcard" }, "pause offers a postcard")
        g.onTap("pause:postcard")
        assertEquals(Phase.POSTCARD, g.phase)

        val arrived = travelGame()
        arrived.phase = Phase.ARRIVED
        assertTrue(arrived.render().hotspots.any { it.id == "arrived:postcard" })

        val dead = travelGame()
        dead.phase = Phase.DEATH
        assertTrue(dead.render().hotspots.any { it.id == "death:postcard" })
    }

    @Test
    fun the_preview_can_be_shared_or_left() {
        val g = travelGame()
        g.onTap("pause:open")
        g.onTap("pause:postcard")
        assertEquals(Phase.POSTCARD, g.phase)
        val preview = g.render()
        assertTrue(preview.hotspots.any { it.id == "postcard:share" })
        assertTrue(preview.hotspots.any { it.id == "postcard:back" })
        assertTrue(preview.toText().contains("YOUR POSTCARD"))

        g.onTap("postcard:share")
        assertTrue(g.requestedShare, "the Share button requests a share")
        assertEquals(Phase.POSTCARD, g.phase, "sharing leaves the preview up")

        g.onTap("postcard:back")
        assertEquals(Phase.PAUSE, g.phase, "Back returns to where we came from")
    }
}
