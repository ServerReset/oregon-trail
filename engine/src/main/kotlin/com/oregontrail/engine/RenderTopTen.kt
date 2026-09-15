package com.oregontrail.engine

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

internal fun Game.renderTopTen(screen: Screen) {
    if (ultraCompact) {
        screen.center(0, "TOP TEN", Palette.BRIGHT_GREEN, bold = true)
        var y = 1
        topTen.take(10).forEachIndexed { i, e ->
            if (y >= rows - 1) return@forEachIndexed
            val line = "${i + 1}.${e.name.take((cols - 9).coerceAtLeast(4))} ${e.points}"
            screen.text(0, y, line.take(cols), Palette.GREEN)
            y++
        }
        val back = "[X]"
        screen.text(0, rows - 1, back, Palette.BRIGHT_GREEN, bold = true)
        screen.hotspot("topten:back", 0, rows - 1, back.length)
        return
    }
    screen.center(1, "THE OREGON TOP TEN", Palette.BRIGHT_GREEN, bold = true)
    val nameW = (contentW - 12).coerceIn(8, 20)
    screen.text(marginX + 2, 3, "Rank  Name".padEnd(nameW + 8) + "Points", Palette.YELLOW)
    var y = 5
    topTen.take(10).forEachIndexed { i, e ->
        if (y >= rows - 3) return@forEachIndexed
        val name = e.name.take(nameW).padEnd(nameW)
        val points = e.points.toString().padStart(5)
        val line = "${(i + 1).toString().padStart(2)}.   $name $points"
        screen.text(marginX + 1, y, line, Palette.GREEN)
        y++
    }
    val label = "[ Back ]"
    screen.text(marginX + 1, rows - 2, label, Palette.BRIGHT_GREEN, bold = true)
    screen.hotspot("topten:back", marginX + 1, rows - 2, label.length)
}
