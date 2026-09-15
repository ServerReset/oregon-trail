package com.oregontrail.engine

import kotlin.math.min

/**
 * The animated trail progress bar with a bobbing wagon marker and a little dust
 * kicked up behind it. Returns the row after the bar.
 */
internal fun Game.drawProgressBar(screen: Screen, y: Int): Int {
    if (y >= rows) return y
    val barW = min(contentW - 16, 26).coerceAtLeast(8)
    val base = (miles.toLong() * barW / Data.TOTAL_MILES).toInt().coerceIn(0, barW - 1)
    val pos = (base + (frame % 2)).coerceIn(0, barW - 1)
    val sb = StringBuilder("[")
    for (i in 0 until barW) {
        sb.append(
            when {
                i < pos -> '='
                i == pos -> '>'
                i == pos - 1 -> if (frame % 2 == 0) ':' else '-'
                i == pos - 2 -> if (frame % 4 == 0) '.' else '-'
                else -> '-'
            }
        )
    }
    sb.append("]  $miles/${Data.TOTAL_MILES} mi")
    screen.text(marginX + 1, y, sb.toString().take(contentW - 1), Palette.CYAN)
    return y + 1
}
