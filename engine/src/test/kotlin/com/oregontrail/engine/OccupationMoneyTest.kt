package com.oregontrail.engine

import kotlin.test.Test
import kotlin.test.assertEquals

/** Regression: picking a leader must apply that leader's starting funds. */
class OccupationMoneyTest {

    @Test
    fun the_chosen_occupation_sets_the_starting_money() {
        for (occ in Occupation.entries) {
            val g = Game(DefaultRng(1L), InMemoryScoreStore())
            g.setViewport(48, 34)
            g.onTap("title:travel")
            g.onTap("prof:${occ.ordinal}")
            g.onTap("month:0")
            assertEquals(
                occ.startingMoney.toDouble(), g.inventory.cash, 0.001,
                "${occ.displayName} should start with ${occ.startingMoney}"
            )
            assertEquals(occ, g.occupation)
            assertEquals(0, g.inventory.food, "the wagon should be empty before shopping")
        }
    }
}
