package com.oregontrail.engine

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Plays hundreds of games with [TestPilot] and asserts the shape of the game's
 * balance: difficulty and occupation must matter, and the economy must leave
 * the Farmer viable but clearly the hardest crossing. The harness is
 * deterministic, so these bands are a stable regression guard.
 */
class BalanceTest {

    private fun row(occ: Occupation, diff: Difficulty) =
        rows.first { it.occ == occ && it.diff == diff }

    @Test
    fun difficulty_scales_survival() {
        val easy = row(Occupation.BANKER, Difficulty.EASY)
        val normal = row(Occupation.BANKER, Difficulty.NORMAL)
        val hard = row(Occupation.BANKER, Difficulty.HARD)

        assertTrue(easy.avgSurvivors >= normal.avgSurvivors, "easy should be gentler than normal")
        assertTrue(normal.avgSurvivors >= hard.avgSurvivors, "normal should be gentler than hard")
        assertTrue(easy.avgSurvivors >= 4.5, "easy should rarely lose anyone (${easy.avgSurvivors})")
        assertTrue(hard.avgSurvivors <= normal.avgSurvivors + 0.01)
        assertTrue(easy.rate >= hard.rate, "easy should arrive at least as often as hard")
    }

    @Test
    fun occupations_trade_risk_for_reward() {
        val banker = row(Occupation.BANKER, Difficulty.NORMAL)
        val carpenter = row(Occupation.CARPENTER, Difficulty.NORMAL)
        val farmer = row(Occupation.FARMER, Difficulty.NORMAL)

        // The Farmer starts poorest, so should not out-survive the Banker...
        assertTrue(farmer.avgSurvivors <= banker.avgSurvivors, "Farmer should be riskier than Banker")
        // ...but must score far more when they make it.
        assertTrue(farmer.avgScore > carpenter.avgScore, "Farmer should outscore Carpenter")
        assertTrue(carpenter.avgScore > banker.avgScore, "Carpenter should outscore Banker")
    }

    private companion object {
        val rows: List<BalanceHarness.Row> by lazy { BalanceHarness.all(120) }
    }

    @Test
    fun the_journey_takes_a_sane_number_of_days() {
        for (r in rows) {
            assertTrue(r.avgDays in 120..300, "${r.occ}/${r.diff} took ${r.avgDays} days")
        }
    }
}
