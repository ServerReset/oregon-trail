package com.oregontrail.engine

import kotlin.math.min

internal fun Game.renderRafting(screen: Screen) {
    val field = raftField ?: return
    if (ultraCompact) {
        screen.text(0, 0, "RAFT ${field.progress}/${field.totalProgress} ${field.integrity}/${field.maxHits}".take(cols), Palette.CYAN, bold = true)
        val fieldY = 1
        for (fy in 0 until field.height) {
            if (fieldY + fy >= rows - 1) break
            for (fx in 0 until min(field.width, cols)) {
                val ch: Char
                val color: Palette
                when {
                    field.isRaftAt(fx, fy) -> { ch = "[=]"[fx - field.raftX]; color = Palette.BRIGHT_GREEN }
                    field.rockAt(fx, fy) -> { ch = 'O'; color = Palette.GRAY }
                    else -> { ch = ' '; color = Palette.DEFAULT }
                }
                screen.put(fx, fieldY + fy, ch, color)
            }
        }
        val cy = rows - 1
        screen.text(0, cy, "<<", Palette.BRIGHT_GREEN, bold = true)
        screen.hotspot("raft:left", 0, cy, 2)
        screen.text(4, cy, ">>", Palette.BRIGHT_GREEN, bold = true)
        screen.hotspot("raft:right", 4, cy, 2)
        return
    }
    screen.center(0, "COLUMBIA RIVER", Palette.BRIGHT_GREEN, bold = true)
    pauseButton(screen)
        val left = "Distance ${field.progress}/${field.totalProgress}"
        val right = "Raft ${field.integrity}/${field.maxHits}"
    screen.text(marginX + 1, 1, left, Palette.BRIGHT_YELLOW)
    screen.text((marginX + contentW - right.length).coerceAtLeast(marginX + 1), 1, right, Palette.WHITE)
    val fieldX = marginX + 1
    val fieldY = 3
    screen.box(fieldX - 1, fieldY - 1, field.width + 2, field.height + 2, Palette.CYAN)
    for (fy in 0 until field.height) {
        for (fx in 0 until field.width) {
            val ch: Char
            val color: Palette
            when {
                field.isRaftAt(fx, fy) -> { ch = "[=]"[fx - field.raftX]; color = Palette.BRIGHT_GREEN }
                field.rockAt(fx, fy) -> { ch = 'O'; color = Palette.GRAY }
                else -> { ch = if (fy % 2 == 0 && (fx + fy) % 7 == 0) '~' else ' '; color = Palette.DIM }
            }
            screen.put(fieldX + fx, fieldY + fy, ch, color)
        }
    }
    var cy = fieldY + field.height + 1
    if (cy > rows - 2) cy = rows - 2
    val cx = marginX + 1
    val leftBtn = "<< LEFT "
    val rightBtn = " RIGHT >>"
    screen.text(cx, cy, leftBtn, Palette.BRIGHT_GREEN, bold = true)
    screen.hotspot("raft:left", cx, cy, leftBtn.length)
    screen.text(cx + 12, cy, rightBtn, Palette.BRIGHT_GREEN, bold = true)
    screen.hotspot("raft:right", cx + 12, cy, rightBtn.length)
}
