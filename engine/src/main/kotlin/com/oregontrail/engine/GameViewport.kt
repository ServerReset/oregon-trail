package com.oregontrail.engine

import kotlin.math.min

/**
 * Resizes the character grid. Tiny screens (watches, phone cover displays)
 * clamp to a still-readable minimum.
 */
fun Game.setViewport(cols: Int, rows: Int) {
    val c = cols.coerceAtLeast(16)
    val r = rows.coerceAtLeast(10)
    val changed = c != this.cols || r != this.rows
    this.cols = c
    this.rows = r
    if (changed) resizeActiveFields()
}

/** Field dimensions for the hunting minigame at the current viewport. */
internal fun Game.huntSize(): Pair<Int, Int> =
    min(cols, 64).coerceIn(10, 64) to min(rows - 8, 16).coerceIn(4, 16)

/** Field dimensions for the rafting finale at the current viewport. */
internal fun Game.raftSize(): Pair<Int, Int> =
    min(contentW, 40).coerceIn(10, 40) to min(rows - 5, 16).coerceIn(4, 16)

/**
 * Keeps an in-progress minigame usable when the form factor changes
 * (folding, unfolding, split-screen, watch size). Progress is preserved.
 */
internal fun Game.resizeActiveFields() {
    huntField?.let { old ->
        val (w, h) = huntSize()
        if (w != old.width || h != old.height) {
            huntField = HuntField(w, h, rng, huntPool(), old.carryLimit, old.meat, old.kills, old.shotsFired)
        }
    }
    raftField?.let { old ->
        val (w, h) = raftSize()
        if (w != old.width || h != old.height) {
            raftField = RaftField(w, h, rng, old.totalProgress, old.maxHits, old.progress, old.hits)
        }
    }
    barlowField?.let { old ->
        val (w, h) = barlowSize()
        if (w != old.width || h != old.height) {
            barlowField = BarlowField(w, h, rng, old.totalProgress, old.maxDamage, old.progress, old.damage)
        }
    }
}

/** Advances the animation clock (called a few times a second). */
fun Game.animate() {
    frame = (frame + 1) % 100000
}
