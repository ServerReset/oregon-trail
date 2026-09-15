package com.oregontrail.engine


internal fun Game.renderEpilogue(screen: Screen) {
    screen.center(0, "EPILOGUE", Palette.BRIGHT_GREEN, bold = true)
    if (ultraCompact) {
        var y = 1
        for (m in party) {
            if (y >= rows - 1) break
            val fate = if (m.alive) "lived" else "died: ${m.condition ?: "trail"}"
            screen.text(0, y, "${m.name.take(cols / 2)} $fate".take(cols), if (m.alive) Palette.GREEN else Palette.GRAY)
            y++
        }
        val back = "[X]"
        screen.text(0, rows - 1, back, Palette.BRIGHT_GREEN, bold = true)
        screen.hotspot("epilogue:back", 0, rows - 1, back.length)
        return
    }
    var y = 2
    y = screen.wrap(
        marginX + 1, y, contentW - 2,
        "The journey is over. Here is how it ended for each of your travelers.",
        Palette.GRAY
    )
    y++
    for (m in party) {
        val fate = if (m.alive) "lived to see Oregon" else "died of ${m.condition ?: "the trail"}"
        y = screen.wrap(marginX + 2, y, contentW - 4, "${m.name}: $fate.", if (m.alive) Palette.GREEN else Palette.GRAY)
    }
    y++
    if (rows - y >= 3) {
        screen.wrap(
            marginX + 1, y, contentW - 2,
            "You traveled $miles miles in ${daysOnTrail()} days. " +
                (if (aliveCount > 0) "The valley is green and the land is yours." else "The trail claimed them all."),
            Palette.GREEN
        )
    }
    val share = "[ Share ]"
    val back = "[ Back ]"
    val by = rows - 2
    screen.text(marginX + 1, by, share, Palette.BRIGHT_GREEN, bold = true)
    screen.hotspot("arrived:share", marginX + 1, by, share.length)
    screen.text(marginX + 12, by, back, Palette.BRIGHT_GREEN, bold = true)
    screen.hotspot("epilogue:back", marginX + 12, by, back.length)
}
