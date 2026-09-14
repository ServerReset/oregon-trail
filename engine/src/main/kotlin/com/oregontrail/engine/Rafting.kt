package com.oregontrail.engine

/**
 * The Columbia River rafting finale: steer a raft down a scrolling rapid,
 * dodging rocks. Pure ASCII and free of Android dependencies so it can be
 * unit tested and driven by any front-end via [tick].
 */
class RaftField(
    val width: Int,
    val height: Int,
    private val rng: Rng,
    val totalProgress: Int = 55,
    val maxHits: Int = 6,
    initialProgress: Int = 0,
    initialHits: Int = 0
) {

    /** Left column of the three-cell raft "[=]". */
    var raftX: Int = ((width - 3) / 2).coerceIn(1, (width - 4).coerceAtLeast(1))
        private set

    private val rocks = ArrayList<Pair<Int, Int>>()

    var progress: Int = initialProgress
        private set
    var hits: Int = initialHits
        private set
    var done: Boolean = false
        private set
    var success: Boolean = false
        private set

    val integrity: Int get() = (maxHits - hits).coerceAtLeast(0)

    fun moveLeft() {
        raftX = (raftX - 1).coerceAtLeast(1)
    }

    fun moveRight() {
        raftX = (raftX + 1).coerceAtMost((width - 4).coerceAtLeast(1))
    }

    fun rockAt(x: Int, y: Int): Boolean = rocks.any { it.first == x && it.second == y }

    fun isRaftAt(x: Int, y: Int): Boolean = y == height - 1 && x in raftX..raftX + 2

    /** Advances the river one step. */
    fun tick() {
        if (done) return

        val survivors = ArrayList<Pair<Int, Int>>()
        for (r in rocks) {
            val nx = r.first
            val ny = r.second + 1
            if (ny == height - 1 && nx in raftX..raftX + 2) {
                hits++
                continue // the rock breaks against the raft
            }
            if (ny >= height) continue // swept past
            survivors.add(nx to ny)
        }
        rocks.clear()
        rocks.addAll(survivors)

        // Spawn new rocks near the top.
        if (rng.chance(0.72)) {
            val x = rng.nextInt((width - 2).coerceAtLeast(1)) + 1
            if (rocks.none { it.second <= 1 && it.first == x }) rocks.add(x to 1)
        }

        progress++
        if (hits >= maxHits) {
            done = true
            success = false
        } else if (progress >= totalProgress) {
            done = true
            success = true
        }
    }

    /** Number of times the raft struck a rock. */
    fun damage(): Int = hits
}
