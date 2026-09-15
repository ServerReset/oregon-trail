package com.oregontrail.engine

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

internal fun Game.renderNotice(screen: Screen) {
    if (ultraCompact) {
        screen.center(0, noticeHeading().take(cols), Palette.BRIGHT_GREEN, bold = true)
        // Wrap first, then draw only what fits above the [>] marker so long
        // prose never overlaps the continue prompt on a tiny screen.
        val wrapped = ArrayList<String>()
        for (line in noticeLines) wrapped.addAll(wrapString(line, cols))
        val budget = (rows - 2).coerceAtLeast(1)
        wrapped.take(budget).forEachIndexed { i, line ->
            screen.text(0, 1 + i, line.take(cols), Palette.GREEN)
        }
        val label = "[>]"
        screen.text(0, rows - 1, label, Palette.BRIGHT_GREEN, bold = true)
        screen.hotspot("notice:continue", 0, rows - 1, label.length)
        return
    }
    screen.center(0, noticeHeading(), Palette.BRIGHT_GREEN, bold = true)
    var y = 2
    noticeArt?.let { art ->
        if (!ultraCompact && rows - 2 > art.size + 3) {
            Ascii.draw(screen, (cols - Ascii.width(art)) / 2, y, art, Palette.GREEN)
            y += Ascii.height(art) + 1
        }
    }
    for (line in noticeLines) {
        y = screen.wrap(marginX + 1, y, contentW - 2, line, Palette.GREEN)
    }
    val label = "[ Continue ]"
    val marker = if (blink()) ">" else " "
    screen.text(marginX + 1, rows - 2, "$marker $label", Palette.BRIGHT_GREEN, bold = true)
    screen.hotspot("notice:continue", marginX + 1, rows - 2, label.length + 2)
}

/** A landmark arrival gets a small blinking flourish. */
internal fun Game.noticeHeading(): String =
    if (noticeTitle == "Landmark" && blink()) "* ${noticeTitle.uppercase()}"
    else noticeTitle.uppercase()
