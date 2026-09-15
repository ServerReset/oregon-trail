package com.oregontrail.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GameSaveTest {

private fun newGame(seed: Long = 1L): Game {
        val rng = DefaultRng(seed)
        val game = Game(rng, InMemoryScoreStore())
        game.setViewport(48, 34)
        return game
    }

    private fun Game.doIntro() {
        onTap("title:travel")
        onTap("prof:0")
        onTap("month:0")
        onTap("names:go")
    }

    private fun Game.buyBasics() {
        repeat(3) { onTap("store:inc:OXEN") }
        repeat(40) { onTap("store:inc:FOOD") }
        repeat(5) { onTap("store:inc:CLOTHING") }
        repeat(20) { onTap("store:inc:AMMUNITION") }
        repeat(2) { onTap("store:inc:WHEEL") }
        repeat(2) { onTap("store:inc:AXLE") }
        repeat(2) { onTap("store:inc:TONGUE") }
        onTap("store:leave")
    }

    @Test
    fun save_and_restore_round_trip() {
        val game = newGame(11L)
        game.doIntro()
        game.buyBasics()
        game.onTap("notice:continue")
        // Move a little way down the trail.
        repeat(5) {
            if (game.phase == Phase.TRAVEL) game.onTap("travel:continue")
            if (game.phase == Phase.NOTICE) game.onTap("notice:continue")
        }
        val snapshot = game.save()
        assertTrue(snapshot.contains("v=1"))

        val restored = Game(DefaultRng(999L), InMemoryScoreStore())
        restored.setViewport(48, 34)
        assertTrue(restored.load(snapshot))
        assertEquals(game.miles, restored.miles)
        assertEquals(game.date.toString(), restored.date.toString())
        assertEquals(game.inventory.food, restored.inventory.food)
        assertEquals(game.inventory.cash, restored.inventory.cash, 0.001)
        assertEquals(game.party.map { it.name }, restored.party.map { it.name })
        // Restored game must be playable.
        if (restored.phase == Phase.TRAVEL) {
            restored.onTap("travel:continue")
            assertTrue(restored.phase != Phase.TITLE)
        }
    }

    @Test
    fun load_rejects_garbage() {
        val game = newGame()
        assertFalse(game.load("not a save file"))
    }
}
