package com.oregontrail.engine

import kotlin.math.abs

/**
 * The Barlow Road: the toll-road ending the designer wished he'd had room for.
 * The wagon drives up a winding, rutted mountain track, steering to stay on the
 * road and dodge rocks and stumps. Reaching the top succeeds; too much damage
 * wrecks the wagon. Pure ASCII and front-end agnostic.
 */
class BarlowField(
    val width: Int,
    val height: Int,
    private val rng: Rng,
    val totalProgress: Int = 45,
    val maxDamage: Int = 9,
    initialProgress: Int = 0,
    initialDamage: Int = 0
) {

    private val centers = DoubleArray(height) { width / 2.0 }
    private val rocks = ArrayList<Pair<Int, Int>>()
    private var halfRoad = 3.0

    var wagonX: Int = width / 2
        private set
    var progress: Int = initialProgress
        private set
    var damage: Int = initialDamage
        private set
    var done: Boolean = false
        private set
    var success: Boolean = false
        private set

    init {
        // Start with a bit of a bend in the road.
        for (i in 0 until minOf(8, height)) {
            val x = (rng.nextDouble() * width).toInt()
            if (x in 1 until width - 1) centers[i] = x.toDouble()
        }
    }

    fun moveLeft() {
        wagonX = (wagonX - 1).coerceAtLeast(0)
    }

    fun moveRight() {
        wagonX = (wagonX + 1).coerceAtMost(width - 1)
    }

    fun centerAt(y: Int): Double = centers[y.coerceIn(0, height - 1)]

    fun roadHalf(): Double = halfRoad

    fun rockAt(x: Int, y: Int): Boolean = rocks.any { it.first == x && it.second == y }

    /** The wagon is drawn three cells wide. */
    fun isWagonAt(x: Int, y: Int): Boolean = y == height - 1 && abs(x - wagonX) <= 1

    val integrity: Int get() = (maxDamage - damage).coerceAtLeast(0)

    fun tick() {
        if (done) return

        // The road scrolls down towards the wagon as it climbs.
        for (y in height - 1 downTo 1) centers[y] = centers[y - 1]
        centers[0] = (centers[0] + (rng.nextDouble() - 0.5) * 2.0).coerceIn(1.0, width - 2.0)
        halfRoad = (halfRoad + (rng.nextDouble() - 0.5) * 0.4).coerceIn(2.0, 5.0)

        val survivors = ArrayList<Pair<Int, Int>>()
        for (r in rocks) {
            val ny = r.second + 1
            if (ny == height - 1 && abs(r.first - wagonX) <= 1) {
                damage++
                continue
            }
            if (ny >= height) continue
            survivors.add(r.first to ny)
        }
        rocks.clear()
        rocks.addAll(survivors)

        if (rng.chance(0.55)) {
            val x = (centers[0] + (rng.nextDouble() - 0.5) * 2 * halfRoad).toInt().coerceIn(1, width - 2)
            if (rocks.none { it.second <= 1 && it.first == x }) rocks.add(x to 1)
        }

        // Drifting off the graded road into ruts and stumps.
        if (abs(wagonX - centers[height - 1]) > halfRoad && rng.chance(0.5)) damage++

        progress++
        if (damage >= maxDamage) {
            done = true
            success = false
        } else if (progress >= totalProgress) {
            done = true
            success = true
        }
    }
}
