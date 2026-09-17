package com.oregontrail.engine

import kotlin.math.min

/**
 * The animated trail progress bar with a bobbing wagon marker, a little dust
 * kicked up behind it and tick marks every quarter of the track. The readout
 * adds a percentage when it still fits. Returns the row after the bar.
 */
internal fun Game.drawProgressBar(screen: Screen, y: Int): Int {
    if (y >= rows) return y
    val barW = min(contentW - 16, 26).coerceAtLeast(8)
    val base = (miles.toLong() * barW / Data.TOTAL_MILES).toInt().coerceIn(0, barW - 1)
    val pos = (base + (frame % 2)).coerceIn(0, barW - 1)
    val ticks = intArrayOf(barW / 4, barW / 2, barW * 3 / 4)
    val sb = StringBuilder("[")
    for (i in 0 until barW) {
        sb.append(
            when {
                i < pos -> '='
                i == pos -> '>'
                i == pos - 1 -> if (frame % 2 == 0) ':' else '-'
                i == pos - 2 -> if (frame % 4 == 0) '.' else if (ticks.contains(i)) '|' else '-'
                ticks.contains(i) -> '|'
                else -> '-'
            }
        )
    }
    sb.append("]  $miles/${Data.TOTAL_MILES} mi")
    val bar = sb.toString()
    val pct = (miles.toLong() * 100 / Data.TOTAL_MILES).toInt()
    val readout = if ((bar + " ($pct%)").length <= contentW - 1) "$bar ($pct%)" else bar
    screen.text(marginX + 1, y, readout.take(contentW - 1), Palette.CYAN)
    return y + 1
}
