package com.oregontrail.engine

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

internal val Game.contentW: Int get() = min(cols, 76)
internal val Game.marginX: Int get() = ((cols - contentW) / 2).coerceAtLeast(0)

/**
 * True on very small displays: watches, phone cover screens and tiny split
 * windows. These layouts drop artwork and long prose and use short labels so
 * the game stays playable.
 */
internal val Game.ultraCompact: Boolean get() = cols < 26 || rows < 16

/** Even tighter: a watch in particular. */
internal val Game.watchLike: Boolean get() = cols < 22 || rows < 13

// ----------------------------------------------------------------------
// Animated visuals (driven by Game.frame, advanced by the front-end)
// ----------------------------------------------------------------------

/** Draws a menu with a bright marker and a forgiving tap target on each row. */
internal fun Screen.menuAt(x: Int, yStart: Int, options: List<Pair<String, String>>): Int {
    var y = yStart
    for ((label, id) in options) {
        text(x, y, ">", Palette.BRIGHT_YELLOW, bold = true)
        text(x + 2, y, label, Palette.GREEN)
        val sx = (x - 1).coerceAtLeast(0)
        val len = (label.length + 4).coerceAtMost(width - sx)
        hotspot(id, sx, y, len)
        y++
    }
    return y
}

internal fun Screen.footer(rows: Int, s: String) {
    center(rows - 1, s, Palette.DIM)
}

/** Draws a small pause button in the top-right corner of a pausable screen. */
internal fun Game.pauseButton(screen: Screen) {
    if (!canPause()) return
    val label = "[||]"
    val x = (cols - label.length - 1).coerceAtLeast(0)
    screen.text(x, 0, label, Palette.BRIGHT_YELLOW, bold = true)
    screen.hotspot("pause:open", x, 0, label.length)
}

/** Lays out a short menu in one or two columns to fit tiny screens. */
internal fun Game.renderMenuColumns(screen: Screen, startY: Int, options: List<Pair<String, String>>) {
    if (options.isEmpty()) return
    val widest = options.maxOf { it.first.length + 2 }
    fun line(x: Int, y: Int, label: String, id: String) {
        screen.text(x, y, ">", Palette.BRIGHT_YELLOW, bold = true)
        screen.text(x + 2, y, label, Palette.GREEN)
        val sx = (x - 1).coerceAtLeast(0)
        val len = (label.length + 4).coerceAtMost(cols - sx)
        screen.hotspot(id, sx, y, len)
    }
    if (widest <= cols && options.size <= rows - startY) {
        var y = startY
        for ((label, id) in options) {
            line(0, y, label, id)
            y++
        }
        return
    }
    val half = (options.size + 1) / 2
    val col2 = max(widest + 1, contentW / 2)
    options.forEachIndexed { i, (label, id) ->
        val cx = if (i < half) 0 else min(col2, cols - 1)
        val cy = startY + (i % half)
        if (cy >= rows) return@forEachIndexed
        line(cx, cy, label, id)
    }
}

internal fun Game.blink(): Boolean = frame % 2 == 0
