package com.oregontrail.engine

import kotlin.math.max

internal fun Game.renderDeath(screen: Screen) {
    if (ultraCompact) {
        screen.center(0, "YOU DIED", Palette.RED, bold = true)
        val leader = party.firstOrNull()?.name ?: "Traveler"
        screen.center(1, leader.take(cols), Palette.WHITE, bold = true)
        screen.center(2, "of $deathCause".take(cols), Palette.GRAY)
        renderMenuColumns(
            screen, 4,
            listOf(
                "Top Ten" to "death:topten",
                "Postcard" to "death:postcard",
                "Epitaph" to "death:epitaph",
                "Again" to "death:restart"
            )
        )
        return
    }
    screen.center(0, "YOU HAVE DIED", Palette.RED, bold = true)
    val leader = party.firstOrNull()?.name ?: "Traveler"
    val art = AsciiScenery.grave
    var y = max(2, (rows - art.size - 8) / 2)
    Ascii.draw(screen, (cols - Ascii.width(art)) / 2, y, art, Palette.GRAY)
    y += art.size + 1
    val cause = "died of $deathCause"
    screen.center(y, "$leader $cause", Palette.WHITE, bold = true)
    screen.center(y + 1, "on ${date}", Palette.GRAY)
    y += 3
    val options = listOf(
        "See the Oregon Top Ten" to "death:topten",
        "Send a postcard" to "death:postcard",
        "Write an epitaph" to "death:epitaph",
        "Try again" to "death:restart"
    )
    val x = (cols - 22) / 2
    screen.menuAt(x, y, options)
    if (y + 3 < rows) {
        lastGravestone?.let {
            screen.wrap(marginX + 1, y + 3, contentW - 2, it, Palette.DIM, bg = Palette.BLACK)
        }
    }
}
