package com.oregontrail.engine

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

internal fun Game.renderAchievements(screen: Screen) {
    screen.center(
        0, "AWARDS ${achievements.size}/${Achievements.all.size}",
        Palette.BRIGHT_GREEN, bold = true
    )
    val detailed = rows >= 30
    var y = 2
    for (a in Achievements.all) {
        if (y >= rows - 2) break
        val got = a.id in achievements
        val mark = if (got) "[x]" else "[ ]"
        val color = if (got) Palette.BRIGHT_GREEN else Palette.GRAY
        screen.text(marginX + 1, y, "$mark ${a.name}".take(contentW - 2), color)
        y++
        if (detailed) y = screen.wrap(marginX + 5, y, contentW - 6, a.description, Palette.DIM)
    }
    val back = "[ Back ]"
    screen.text(marginX + 1, rows - 2, back, Palette.BRIGHT_GREEN, bold = true)
    screen.hotspot("ach:back", marginX + 1, rows - 2, back.length)
}
