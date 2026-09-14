package com.oregontrail.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RngTest {

    @Test
    fun default_rng_is_reproducible_for_a_seed() {
        val a = DefaultRng(42L)
        val b = DefaultRng(42L)
        repeat(50) { assertEquals(a.nextDouble(), b.nextDouble()) }
    }

    @Test
    fun next_int_stays_in_range() {
        val rng = DefaultRng(7L)
        repeat(1000) {
            val v = rng.nextInt(6)
            assertTrue(v in 0..5, "value $v out of range")
            val w = rng.nextInt(3, 9)
            assertTrue(w in 3..8, "value $w out of range")
        }
    }

    @Test
    fun scripted_rng_returns_scripted_values_then_default() {
        val rng = ScriptedRng.of(0.1, 0.2)
        assertEquals(0.1, rng.nextDouble())
        assertEquals(0.2, rng.nextDouble())
        assertEquals(0.5, rng.nextDouble())
        assertTrue(rng.chance(0.6))     // 0.5 < 0.6
    }

    @Test
    fun shuffle_is_a_permutation() {
        val rng = DefaultRng(3L)
        val list = MutableList(30) { it }
        rng.shuffleInPlace(list)
        assertEquals((0 until 30).toList(), list.sorted())
    }
}
