package com.oregontrail.engine

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

internal fun Game.renderChoice(screen: Screen) {
    screen.center(0, choiceTitle.uppercase().take(cols), Palette.BRIGHT_GREEN, bold = true)
    if (ultraCompact) {
        var y = 1
        for ((label, id) in choiceOptions) {
            if (y >= rows) break
            val short = label.take(cols)
            screen.text(0, y, short, Palette.GREEN)
            screen.hotspot(id, 0, y, short.length)
            y++
        }
        return
    }
    var y = 2
    y = screen.wrap(marginX + 1, y, contentW - 2, choiceLines.joinToString("\n"), Palette.GREEN)
    y++
    screen.menuAt(marginX + 1, y, choiceOptions)
}
