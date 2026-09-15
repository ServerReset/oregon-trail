package com.oregontrail.engine

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** A camp-at-night pause screen with save-state actions. */
internal fun Game.renderPause(screen: Screen) {
    screen.center(0, "PAUSED", Palette.BRIGHT_YELLOW, bold = true)
    val options = listOf(
        "1. Resume the journey" to "pause:resume",
        "2. Save game" to "pause:save",
        "3. Quick save" to "pause:quicksave",
        "4. Quick load" to "pause:quickload",
        "5. Load a saved game" to "pause:load",
        "6. Management options" to "pause:manage",
        "7. Save and return to title" to "pause:title",
        "8. Quit" to "pause:quit"
    )
    val short = listOf(
        "1. Resume" to "pause:resume",
        "2. Save" to "pause:save",
        "3. Q.save" to "pause:quicksave",
        "4. Q.load" to "pause:quickload",
        "5. Load" to "pause:load",
        "6. Options" to "pause:manage",
        "7. Title" to "pause:title",
        "8. Quit" to "pause:quit"
    )
    val camp = campArt(frame)
    val artFits = !ultraCompact && rows >= 2 + Ascii.height(camp) + 3
    val startY = if (artFits) {
        val top = Ascii.draw(screen, (cols - Ascii.width(camp)) / 2, 2, camp, Palette.GREEN)
        overlaySmoke(screen, 2, Ascii.height(camp))
        top
    } else {
        2
    }
    renderMenuColumns(screen, startY, if (ultraCompact) short else options)
}
