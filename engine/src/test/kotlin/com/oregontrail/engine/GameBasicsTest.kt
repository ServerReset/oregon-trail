package com.oregontrail.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameBasicsTest {

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
    fun title_renders_with_menu() {
        val game = newGame()
        val screen = game.render()
        assertTrue(screen.toText().isNotBlank())
        assertTrue(screen.hotspots.any { it.id == "title:travel" })
    }

    @Test
    fun store_purchases_deduct_cash_and_add_goods() {
        val game = newGame()
        game.doIntro()
        val start = game.inventory.cash
        game.onTap("store:inc:OXEN")
        assertEquals(2, game.inventory.oxen)
        assertEquals(start - 20.0, game.inventory.cash, 0.001)
        game.onTap("store:inc:FOOD")
        assertEquals(50, game.inventory.food)
        game.onTap("store:dec:FOOD")
        assertEquals(0, game.inventory.food)
    }

    @Test
    fun cannot_spend_more_than_available_cash() {
        val game = newGame()
        game.doIntro()
        // Farmer has $400; ensure we cannot overspend.
        game.onTap("prof:2")
        game.onTap("month:0")
        var guard = 0
        while (game.inventory.cash >= 20 && guard++ < 100) game.onTap("store:inc:OXEN")
        assertTrue(game.inventory.cash >= 0.0)
        assertTrue(game.inventory.cash < 20.0)
    }

    @Test
    fun leaving_without_oxen_is_refused() {
        val game = newGame()
        game.doIntro()
        game.onTap("store:leave")
        assertEquals(Phase.NOTICE, game.phase)
        game.onTap("notice:continue")
        assertEquals(Phase.STORE, game.phase)
    }

    @Test
    fun dates_advance_across_month_and_year_ends() {
        val d = GameDate(1848, 5, 31)
        d.plusDays(1)
        assertEquals(6, d.month)
        assertEquals(1, d.day)

        val dec = GameDate(1848, 12, 31)
        dec.plusDays(1)
        assertEquals(1849, dec.year)
        assertEquals(1, dec.month)
        assertEquals(1, dec.day)

        val feb = GameDate(1848, 2, 28) // 1848 is a leap year
        feb.plusDays(1)
        assertEquals(29, feb.day)
        feb.plusDays(1)
        assertEquals(3, feb.month)
        assertEquals(1, feb.day)
    }
}
