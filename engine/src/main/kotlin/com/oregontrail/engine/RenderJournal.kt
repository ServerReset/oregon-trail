package com.oregontrail.engine

import kotlin.math.max
import kotlin.math.min

internal fun Game.journalPageSize(): Int = max(if (ultraCompact) 2 else 3, (rows - 5) / 2)

internal fun Game.journalLastPage(): Int =
    if (journal.isEmpty()) 0 else (journal.size - 1) / journalPageSize()

internal fun Game.renderJournal(screen: Screen) {
    screen.center(0, "MY JOURNAL", Palette.BRIGHT_GREEN, bold = true)
    pauseButton(screen)
    if (journal.isEmpty()) {
        screen.wrap(marginX + 1, 3, contentW - 2, "Nothing has happened yet. The trail awaits.", Palette.GREEN)
    } else {
        val size = journalPageSize()
        val page = journalPage.coerceIn(0, journalLastPage())
        val from = page * size
        val to = min(journal.size, from + size)
        var y = 2
        for (i in from until to) {
            val e = journal[i]
            screen.text(marginX + 1, y, e.date, Palette.YELLOW)
            y++
            y = screen.wrap(marginX + 3, y, contentW - 4, e.text, Palette.GREEN)
            y++
            if (y >= rows - 2) break
        }
        screen.text(
            marginX + 1, rows - 1,
            "Page ${page + 1} of ${journalLastPage() + 1}",
            Palette.DIM
        )
    }
    if (ultraCompact) {
        val prev = "[<]"
        val next = "[>]"
        val back = "[X]"
        val y = rows - 1
        screen.text(0, y, prev, Palette.BRIGHT_GREEN)
        screen.hotspot("journal:prev", 0, y, prev.length)
        screen.text(5, y, next, Palette.BRIGHT_GREEN)
        screen.hotspot("journal:next", 5, y, next.length)
        screen.text(10, y, back, Palette.BRIGHT_GREEN, bold = true)
        screen.hotspot("journal:back", 10, y, back.length)
        return
    }
    val prev = "[< Prev ]"
    val next = "[ Next >]"
    val back = "[ Back ]"
    val y = rows - 2
    screen.text(marginX + 1, y, prev, Palette.BRIGHT_GREEN)
    screen.hotspot("journal:prev", marginX + 1, y, prev.length)
    screen.text(marginX + 12, y, next, Palette.BRIGHT_GREEN)
    screen.hotspot("journal:next", marginX + 12, y, next.length)
    screen.text(marginX + 23, y, back, Palette.BRIGHT_GREEN, bold = true)
    screen.hotspot("journal:back", marginX + 23, y, back.length)
}
