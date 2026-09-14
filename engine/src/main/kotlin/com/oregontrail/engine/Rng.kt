package com.oregontrail.engine

import kotlin.random.Random

/**
 * Small randomness abstraction so the game can be run deterministically in tests.
 */
interface Rng {
    fun nextDouble(): Double

    fun nextInt(bound: Int): Int = if (bound <= 0) 0 else (nextDouble() * bound).toInt().coerceIn(0, bound - 1)

    fun nextInt(from: Int, untilExclusive: Int): Int =
        from + nextInt((untilExclusive - from).coerceAtLeast(1))

    fun chance(probability: Double): Boolean = nextDouble() < probability

    fun <T> pick(items: List<T>): T = items[nextInt(items.size)]

    fun <T> pick(items: Array<T>): T = items[nextInt(items.size)]

    fun shuffleInPlace(items: MutableList<Int>) {
        for (i in items.indices.reversed()) {
            val j = nextInt(i + 1)
            val tmp = items[i]
            items[i] = items[j]
            items[j] = tmp
        }
    }
}

/** Default RNG backed by [kotlin.random.Random]. */
class DefaultRng(seed: Long = System.nanoTime()) : Rng {
    private val random = Random(seed)
    override fun nextDouble(): Double = random.nextDouble()
}

/** Deterministic RNG for reproducible playthroughs and tests. */
class ScriptedRng(private val values: MutableList<Double>) : Rng {
    override fun nextDouble(): Double = if (values.isEmpty()) 0.5 else values.removeAt(0)

    companion object {
        fun of(vararg values: Double): ScriptedRng = ScriptedRng(values.toMutableList())
    }
}
