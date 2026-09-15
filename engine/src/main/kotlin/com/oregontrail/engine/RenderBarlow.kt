package com.oregontrail.engine

import kotlin.math.abs
import kotlin.math.min

internal fun Game.renderBarlow(screen: Screen) {
    val field = barlowField ?: return
    if (ultraCompact) {
        screen.text(
            0, 0,
            "BARLOW ${field.progress}/${field.totalProgress} ${field.integrity}/${field.maxDamage}".take(cols),
            Palette.BRIGHT_GREEN, bold = true
        )
        val fieldY = 1
        for (fy in 0 until field.height) {
            if (fieldY + fy >= rows - 1) break
            val center = field.centerAt(fy)
            for (fx in 0 until min(field.width, cols)) {
                val inRoad = abs(fx - center) <= field.roadHalf()
                val ch: Char
                val color: Palette
                when {
                    field.isWagonAt(fx, fy) -> { ch = if (fx == field.wagonX) 'W' else '='; color = Palette.BRIGHT_YELLOW }
                    field.rockAt(fx, fy) -> { ch = 'O'; color = Palette.GRAY }
                    inRoad -> { ch = '.'; color = Palette.DIM }
                    else -> { ch = ' '; color = Palette.DEFAULT }
                }
                screen.put(fx, fieldY + fy, ch, color)
            }
        }
        val cy = rows - 1
        screen.text(0, cy, "<<", Palette.BRIGHT_GREEN, bold = true)
        screen.hotspot("barlow:left", 0, cy, 2)
        screen.text(4, cy, ">>", Palette.BRIGHT_GREEN, bold = true)
        screen.hotspot("barlow:right", 4, cy, 2)
        return
    }
    screen.center(0, "BARLOW ROAD", Palette.BRIGHT_GREEN, bold = true)
    pauseButton(screen)
    val left = "Climb ${field.progress}/${field.totalProgress}"
    val right = "Wagon ${field.integrity}/${field.maxDamage}"
    screen.text(marginX + 1, 1, left, Palette.BRIGHT_YELLOW)
    screen.text((marginX + contentW - right.length).coerceAtLeast(marginX + 1), 1, right, Palette.WHITE)
    val fieldX = marginX + 1
    val fieldY = 3
    screen.box(fieldX - 1, fieldY - 1, field.width + 2, field.height + 2, Palette.BROWN)
    for (fy in 0 until field.height) {
        val center = field.centerAt(fy)
        for (fx in 0 until field.width) {
            val inRoad = abs(fx - center) <= field.roadHalf()
            val ch: Char
            val color: Palette
            when {
                field.isWagonAt(fx, fy) -> { ch = if (fx == field.wagonX) 'W' else '='; color = Palette.BRIGHT_YELLOW }
                field.rockAt(fx, fy) -> { ch = 'O'; color = Palette.GRAY }
                inRoad -> { ch = '.'; color = Palette.DIM }
                else -> { ch = if ((fx + fy) % 9 == 0) '^' else ' '; color = Palette.GREEN }
            }
            screen.put(fieldX + fx, fieldY + fy, ch, color)
        }
    }
    var cy = fieldY + field.height + 1
    if (cy > rows - 2) cy = rows - 2
    val cx = marginX + 1
    screen.text(cx, cy, "<< LEFT ", Palette.BRIGHT_GREEN, bold = true)
    screen.hotspot("barlow:left", cx, cy, 8)
    screen.text(cx + 12, cy, " RIGHT >>", Palette.BRIGHT_GREEN, bold = true)
    screen.hotspot("barlow:right", cx + 12, cy, 9)
}
