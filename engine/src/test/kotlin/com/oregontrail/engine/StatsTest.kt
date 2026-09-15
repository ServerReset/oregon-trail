package com.oregontrail.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Lifetime statistics and the awards tied to them. */
class StatsTest {

    private fun game(): Game {
        val g = Game(DefaultRng(1L), InMemoryScoreStore())
        g.setViewport(48, 34)
        return g
    }

    @Test
    fun reaching_landmarks_counts_and_unlocks_trailblazer() {
        val g = game()
        repeat(11) { g.advanceToLandmark(ArrayList()) }
        assertTrue(g.landmarkIndex >= 10, "index was ${g.landmarkIndex}")
        assertTrue(g.stats.landmarksVisited >= 11, "visited ${g.stats.landmarksVisited}")
        assertTrue(Achievements.TRAILBLAZER in g.achievements)
    }

    @Test
    fun sending_a_postcard_is_recorded_and_awarded() {
        val g = game()
        g.requestedShare = true
        g.recordPostcardSent()
        assertEquals(1, g.stats.postcardsSent)
        assertTrue(Achievements.POSTMASTER in g.achievements)
        assertFalse(g.requestedShare, "the share request should be cleared")
    }

    @Test
    fun the_stats_screen_mentions_landmarks_and_postcards() {
        val g = game()
        g.stats = g.stats.copy(landmarksVisited = 4, postcardsSent = 2)
        g.phase = Phase.STATS
        val text = g.render().toText()
        assertTrue(text.contains("4"), "landmarks should show")
        assertTrue(text.contains("Postcards") || text.contains("postcards"))
    }
}
