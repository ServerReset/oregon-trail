package com.oregontrail.engine

/** Opens the postcard preview, remembering where to return afterwards. */
internal fun Game.openPostcard() {
    postcardReturn = phase
    phase = Phase.POSTCARD
}

/** Shows the shareable postcard with Share / Back actions. */
internal fun Game.renderPostcard(screen: Screen) {
    screen.center(0, "YOUR POSTCARD", Palette.BRIGHT_GREEN, bold = true)
    val card = postcard()
    val w = card.firstOrNull()?.length ?: 0
    val startY = ((rows - card.size - 2) / 2).coerceAtLeast(1)
    val x = ((cols - w) / 2).coerceAtLeast(0)
    card.forEachIndexed { i, line ->
        if (startY + i < rows - 1) screen.text(x, startY + i, line.take(cols), Palette.GREEN)
    }
    val by = rows - 1
    val share = "[ Share ]"
    val back = "[ Back ]"
    screen.text(marginX + 1, by, share, Palette.BRIGHT_GREEN, bold = true)
    screen.hotspot("postcard:share", marginX + 1, by, share.length)
    screen.text(marginX + 12, by, back, Palette.BRIGHT_GREEN, bold = true)
    screen.hotspot("postcard:back", marginX + 12, by, back.length)
}
