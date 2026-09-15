package com.oregontrail.engine

import kotlin.math.max
import kotlin.math.min

internal fun Game.renderLoad(screen: Screen) {
    val perPage = max(1, (rows - 5) / 2)
    val lastPage = if (saveSlots.isEmpty()) 0 else (saveSlots.size - 1) / perPage
    val page = loadPage.coerceIn(0, lastPage)
    val from = page * perPage
    val to = min(saveSlots.size, from + perPage)

    if (ultraCompact) {
        screen.center(0, "SAVED", Palette.BRIGHT_GREEN, bold = true)
        if (saveSlots.isEmpty()) screen.center(1, "(none)", Palette.GRAY)
        saveSlots.subList(from, to).forEachIndexed { i, slot ->
            val y = 1 + i
            if (y >= rows - 1) return@forEachIndexed
            val label = ("*" + slot.label).take(cols)
            screen.text(0, y, label, Palette.GREEN)
            screen.hotspot("slot:load:${slot.id}", 0, y, label.length)
        }
        val back = "[X]"
        screen.text(0, rows - 1, back, Palette.BRIGHT_GREEN, bold = true)
        screen.hotspot("slots:back", 0, rows - 1, back.length)
        return
    }

    screen.center(0, "SAVED GAMES", Palette.BRIGHT_GREEN, bold = true)
    screen.text(marginX + 1, 1, "Page ${page + 1}/${lastPage + 1}  -  ${saveSlots.size} saves", Palette.DIM)
    screen.text(marginX + 1, 2, "[ren] rename  [sv] save over  [del] delete", Palette.DIM)
    if (saveSlots.isEmpty()) {
        screen.wrap(
            marginX + 1, 4, contentW - 2,
            "No saved games yet. Open the pause menu (Back or [||]) and choose " +
                "Save game or Quick save.",
            Palette.GRAY
        )
    } else {
        var y = 4
        for (slot in saveSlots.subList(from, to)) {
            if (y >= rows - 3) break
            val label = slot.label.take(contentW - 10)
            screen.text(marginX + 2, y, ">", Palette.BRIGHT_YELLOW, bold = true)
            screen.text(marginX + 4, y, label, Palette.BRIGHT_GREEN, bold = true)
            screen.hotspot("slot:load:${slot.id}", marginX + 1, y, label.length + 4)
            val ren = "[ren]"
            val rx = marginX + contentW - 6
            screen.text(rx, y, ren, Palette.CYAN)
            screen.hotspot("slot:rename:${slot.id}", rx, y, ren.length)
            y++
            val sv = "[sv]"
            val del = "[del]"
            screen.text(marginX + 4, y, slot.detail.take(contentW - 18), Palette.GRAY)
            val ax = marginX + contentW - 10
            screen.text(ax, y, sv, Palette.CYAN)
            screen.hotspot("slot:overwrite:${slot.id}", ax, y, sv.length)
            screen.text(ax + 5, y, del, Palette.RED)
            screen.hotspot("slot:del:${slot.id}", ax + 5, y, del.length)
            y += 2
        }
        if (lastPage > 0) {
            val prev = "[< Prev]"
            val next = "[Next >]"
            screen.text(marginX + 1, rows - 3, prev, Palette.BRIGHT_GREEN)
            screen.hotspot("slots:prev", marginX + 1, rows - 3, prev.length)
            screen.text(marginX + 10, rows - 3, next, Palette.BRIGHT_GREEN)
            screen.hotspot("slots:next", marginX + 10, rows - 3, next.length)
        }
    }
    val back = "[ Back ]"
    screen.text(marginX + 1, rows - 2, back, Palette.BRIGHT_GREEN, bold = true)
    screen.hotspot("slots:back", marginX + 1, rows - 2, back.length)
}
