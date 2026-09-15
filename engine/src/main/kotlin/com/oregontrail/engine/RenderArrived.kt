package com.oregontrail.engine


internal fun Game.renderArrived(screen: Screen) {
    screen.center(0, "OREGON!", Palette.BRIGHT_GREEN, bold = true)
    if (!ultraCompact && cols >= 14) {
        val sx = cols - 7
        screen.putIfBlank(sx + 1, 1, if (frame % 2 == 0) '\\' else '/', Palette.BRIGHT_YELLOW)
        screen.putIfBlank(sx + 5, 1, if (frame % 2 == 0) '/' else '\\', Palette.BRIGHT_YELLOW)
        screen.text(sx, 2, "-(o)-", Palette.BRIGHT_YELLOW)
        screen.putIfBlank(sx + 1, 3, if (frame % 2 == 0) '/' else '\\', Palette.BRIGHT_YELLOW)
        screen.putIfBlank(sx + 5, 3, if (frame % 2 == 0) '\\' else '/', Palette.BRIGHT_YELLOW)
    }
    if (ultraCompact) {
        screen.center(1, "Score $lastScore".take(cols), Palette.BRIGHT_YELLOW, bold = true)
        renderMenuColumns(
            screen, 3,
            listOf(
                "Top Ten" to "arrived:topten",
                "Postcard" to "arrived:postcard",
                "Again" to "arrived:restart"
            )
        )
        return
    }
    val art = Ascii.blockWord("WELCOME")
    var y = 2
    if (rows >= 40 && contentW >= 34) {
        val valley = AsciiLandmarks.valley
        Ascii.draw(screen, (cols - Ascii.width(valley)) / 2, y, valley, Palette.GREEN)
        y += Ascii.height(valley) + 1
    }
    Ascii.draw(screen, (cols - Ascii.width(art)) / 2, y, art, Palette.BRIGHT_YELLOW)
    y += art.size + 2
    y = screen.wrap(marginX + 1, y, contentW - 2,
        "You have reached the end of the Oregon Trail and the fertile Willamette Valley.",
        Palette.GREEN)
    y++
    screen.text(marginX + 1, y, "Final score: $lastScore points", Palette.BRIGHT_YELLOW, bold = true)
    y++
    if (rows >= 24) {
        screen.text(
            marginX + 1, y,
            "Career: ${stats.landmarksVisited} landmarks - ${stats.postcardsSent} postcards",
            Palette.DIM
        )
        y++
    }
    if (rows >= 28) {
        screen.text(marginX + 1, y, "How your score was earned:", Palette.DIM)
        y++
        for ((label, value) in scoreParts()) {
            screen.text(marginX + 1, y, label.take(contentW - 8), Palette.GRAY)
            screen.text(marginX + contentW - 6, y, value.toString().padStart(6), Palette.WHITE)
            y++
            if (y >= rows - 5) break
        }
    }
    y++
    val options = listOf(
        "See the Oregon Top Ten" to "arrived:topten",
        "Send a postcard" to "arrived:postcard",
        "Travel the trail again" to "arrived:restart"
    )
    screen.menuAt(marginX + 1, y, options)
}
