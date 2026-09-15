package com.oregontrail.engine

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Sharing the trail journal as text. */
class JournalShareTest {

    private fun game(): Game {
        val g = Game(DefaultRng(1L), InMemoryScoreStore())
        g.setViewport(60, 30)
        g.onTap("title:travel"); g.onTap("prof:0"); g.onTap("month:0"); g.onTap("names:go")
        g.onTap("store:inc:OXEN"); g.onTap("store:leave"); g.onTap("notice:continue")
        return g
    }

    @Test
    fun the_journal_can_be_rendered_as_text() {
        val g = game()
        g.addJournal("Crossed a muddy creek today.")
        val text = g.journalText()
        assertTrue(text.contains("journal"))
        assertTrue(text.contains("Crossed a muddy creek today."))
    }

    @Test
    fun the_journal_screen_offers_share_on_wide_screens() {
        val g = game()
        g.onTap("travel:journal")
        assertTrue(g.render().hotspots.any { it.id == "journal:share" }, "share button on the journal")
        g.onTap("journal:share")
        assertTrue(g.requestedJournalShare)
        g.clearJournalShareRequest()
        assertFalse(g.requestedJournalShare)
    }
}
