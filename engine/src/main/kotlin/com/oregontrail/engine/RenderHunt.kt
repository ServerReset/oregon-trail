package com.oregontrail.engine

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

internal fun Game.renderHunting(screen: Screen) {
    val field = huntField ?: return
    if (ultraCompact) {
        renderHuntingCompact(screen, field)
        return
    }
    screen.center(0, "HUNTING", Palette.BRIGHT_GREEN, bold = true)
    pauseButton(screen)
    val hLeft = "Meat ${field.meat}/${field.carryLimit}"
    val hMid = "Ammo ${inventory.ammo}"
    val hRight = "Kills ${field.kills}"
    screen.text(marginX + 1, 1, hLeft, Palette.BRIGHT_YELLOW)
    screen.text(marginX + (contentW / 2 - hMid.length / 2).coerceAtLeast(marginX + 1 + hLeft.length), 1, hMid, Palette.WHITE)
    screen.text((marginX + contentW - hRight.length).coerceAtLeast(marginX + 1), 1, hRight, Palette.GREEN)
    val fieldX = marginX + 1
    val fieldY = 3
    screen.box(fieldX - 1, fieldY - 1, field.width + 2, field.height + 2, Palette.GREEN)
    for (fy in 0 until field.height) {
        for (fx in 0 until field.width) {
            val ch: Char
            val color: Palette
            val animal = field.animalGlyphAt(fx, fy)
            when {
                fx == field.hunterX && fy == field.hunterY -> { ch = '@'; color = Palette.BRIGHT_GREEN }
                animal != null -> { ch = animal.first; color = animal.second }
                field.bulletAt(fx, fy) -> { ch = '*'; color = Palette.BRIGHT_WHITE }
                else -> {
                    val decor = field.decorationAt(fx, fy)
                    if (decor != null) { ch = decor; color = if (decor == 'o') Palette.GRAY else Palette.GREEN }
                    else { ch = ' '; color = Palette.DEFAULT }
                }
            }
            screen.put(fieldX + fx, fieldY + fy, ch, color)
        }
    }
    // Controls
    var cy = fieldY + field.height + 1
    if (cy > rows - 4) cy = rows - 4
    val cx = marginX + 1
    val up = "  ^  "
    val left = "<    "
    val right = "    >"
    val down = "  v  "
    screen.text(cx + 2, cy, up, Palette.BRIGHT_GREEN); screen.hotspot("hunt:up", cx + 2, cy, up.length)
    screen.text(cx, cy + 1, left, Palette.BRIGHT_GREEN); screen.hotspot("hunt:left", cx, cy + 1, left.length)
    screen.text(cx + 6, cy + 1, right, Palette.BRIGHT_GREEN); screen.hotspot("hunt:right", cx + 6, cy + 1, right.length)
    screen.text(cx + 2, cy + 2, down, Palette.BRIGHT_GREEN); screen.hotspot("hunt:down", cx + 2, cy + 2, down.length)
    val shoot = "[ SHOOT ]"
    screen.text(cx + 14, cy + 1, shoot, Palette.BRIGHT_YELLOW, bold = true)
    screen.hotspot("hunt:shoot", cx + 14, cy + 1, shoot.length)
    val leave = "[ Return to trail ]"
    screen.text(cx + 14, cy + 2, leave, Palette.BRIGHT_GREEN)
    screen.hotspot("hunt:leave", cx + 14, cy + 2, leave.length)
}

/** Watch / cover-screen hunt: a small field with a one-row control strip. */
internal fun Game.renderHuntingCompact(screen: Screen, field: HuntField) {
    screen.text(0, 0, "HUNT ${field.meat}/${field.carryLimit} A${inventory.ammo}".take(cols), Palette.BRIGHT_GREEN, bold = true)
    val fieldY = 1
    for (fy in 0 until field.height) {
        if (fieldY + fy >= rows - 1) break
        for (fx in 0 until min(field.width, cols)) {
            val animal = field.animalGlyphAt(fx, fy)
            val ch: Char
            val color: Palette
            when {
                fx == field.hunterX && fy == field.hunterY -> { ch = '@'; color = Palette.BRIGHT_GREEN }
                animal != null -> { ch = animal.first; color = animal.second }
                field.bulletAt(fx, fy) -> { ch = '*'; color = Palette.BRIGHT_WHITE }
                else -> {
                    val decor = field.decorationAt(fx, fy)
                    if (decor != null) { ch = decor; color = if (decor == 'o') Palette.GRAY else Palette.GREEN }
                    else { ch = ' '; color = Palette.DEFAULT }
                }
            }
            screen.put(fx, fieldY + fy, ch, color)
        }
    }
    val cy = rows - 1
    val controls = listOf(
        "<" to "hunt:left", ">" to "hunt:right", "^" to "hunt:up",
        "v" to "hunt:down", "O" to "hunt:shoot", "X" to "hunt:leave"
    )
    var x = 0
    for ((label, id) in controls) {
        if (x + 1 >= cols) break
        val color = if (id == "hunt:shoot") Palette.BRIGHT_YELLOW else Palette.BRIGHT_GREEN
        screen.text(x, cy, label, color, bold = id == "hunt:shoot")
        screen.hotspot(id, x, cy, 1)
        x += 2
    }
}
