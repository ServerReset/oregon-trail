package com.oregontrail.engine

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

internal fun Game.renderTravel(screen: Screen) {
    if (ultraCompact) {
        renderTravelCompact(screen)
        return
    }
    val compact = rows < 30
    var y = 0
    screen.center(y, "THE OREGON TRAIL", Palette.BRIGHT_GREEN, bold = true)
    pauseButton(screen)
    y++
    // Status box, wrapped to fit narrow screens.
    val boxW = min(contentW, 56).coerceAtLeast(24)
    val inner = boxW - 4
    val wrapped = ArrayList<String>()
    for (line in if (compact) compactStatusLines() else statusLines()) {
        wrapped.addAll(wrapString(line, inner))
    }
    screen.box(marginX, y, boxW, wrapped.size + 2, Palette.GREEN, "Status")
    wrapped.forEachIndexed { i, line ->
        val color = when {
            (line.startsWith("Food") || line.startsWith("fd")) && inventory.food <= 150 -> Palette.YELLOW
            (line.startsWith("Ammo") || line.contains("ammo")) && inventory.ammo <= 20 -> Palette.YELLOW
            line.contains("poor") || line.contains("failing") -> Palette.RED
            else -> Palette.GREEN
        }
        screen.text(marginX + 2, y + 1 + i, line, color)
    }
    y += wrapped.size + 3
    if ((inventory.food <= 80 || oxHealth <= 25) && y < rows - 2) {
        screen.center(
            y,
            if (blink()) "*** LOW SUPPLIES - HUNT OR BUY FOOD ***" else "",
            Palette.RED, bold = true
        )
        y++
    }
    // Animated trail progress bar with a bobbing wagon marker.
    if (y < rows - 1) {
        val barW = min(contentW - 16, 26).coerceAtLeast(8)
        val base = (miles.toLong() * barW / Data.TOTAL_MILES).toInt().coerceIn(0, barW - 1)
        val pos = (base + (frame % 2)).coerceIn(0, barW - 1)
        val sb = StringBuilder("[")
        for (i in 0 until barW) {
            sb.append(
                when {
                    i < pos -> '='
                    i == pos -> '>'
                    // Kicked-up dust trails just behind the wagon.
                    i == pos - 1 -> if (frame % 2 == 0) ':' else '-'
                    i == pos - 2 -> if (frame % 4 == 0) '.' else '-'
                    else -> '-'
                }
            )
        }
        sb.append("]  $miles/${Data.TOTAL_MILES} mi")
        screen.text(marginX + 1, y, sb.toString().take(contentW - 1), Palette.CYAN)
        y++
    }

    if (!compact) {
        val scene = sceneArt()
        val sceneTop = y
        Ascii.draw(screen, (cols - Ascii.width(scene)) / 2, y, scene, Palette.GREEN)
        val sceneBottom = sceneTop + Ascii.height(scene)
        y += Ascii.height(scene) + 1
        overlaySky(screen, sceneTop, sceneBottom)
        overlayWeather(screen, sceneTop, sceneBottom)
        overlayWildlife(screen, sceneTop, sceneBottom)
        overlayGround(screen, sceneTop, sceneBottom)
    }

    val remaining = rows - y - 1
    val options = travelOptions()
    if (remaining >= options.size + 1) {
        screen.text(marginX + 1, y, "What would you like to do?", Palette.BRIGHT_YELLOW)
        y++
        screen.menuAt(marginX + 1, y, options)
    } else {
        // Compact: put the menu in two columns.
        val half = (options.size + 1) / 2
        val colX = marginX + 1
        val colX2 = marginX + contentW / 2
        options.forEachIndexed { i, (label, id) ->
            val cx = if (i < half) colX else colX2
            val cy = y + (i % half)
            val short = label.substringBefore("  ").trim()
            screen.text(cx, cy, short, Palette.GREEN)
            screen.hotspot(id, cx, cy, short.length)
        }
    }
}

/** Watch / cover-screen travel: a couple of status lines and a two-column menu. */
internal fun Game.renderTravelCompact(screen: Screen) {
    val next = nextLandmark()
    val toNext = if (next != null) "${(next.mile - miles).coerceAtLeast(0)}mi" else "end"
    val wx = when (weather.kind) {
        WeatherKind.SNOW, WeatherKind.BLIZZARD, WeatherKind.COLD -> '*'
        WeatherKind.RAIN, WeatherKind.HEAVY_RAIN, WeatherKind.THUNDERSTORM,
        WeatherKind.HAIL -> '/'
        WeatherKind.CLEAR, WeatherKind.HOT -> 'o'
        else -> '~'
    }
    val status = listOf(
        "${date.monthName.take(3)} ${date.day}  $wx${weather.kind.displayName.take(7)}",
        "${miles}/${Data.TOTAL_MILES}mi  next $toNext",
        "fd${inventory.food} ammo${inventory.ammo} \$${"%.0f".format(inventory.cash)}"
    )
    var y = 0
    for (line in status) {
        screen.text(0, y, line.take(cols), Palette.GREEN)
        y++
    }
    val options = ultraTravelOptions()
    val menuRows = (options.size + 1) / 2
    // A compact progress bar with a bobbing wagon, when there is room.
    if (rows - y > menuRows) {
        val barW = (cols - 2).coerceIn(6, 30)
        val base = (miles.toLong() * barW / Data.TOTAL_MILES).toInt().coerceIn(0, barW - 1)
        val pos = (base + (frame % 2)).coerceIn(0, barW - 1)
        val sb = StringBuilder()
        for (i in 0 until barW) sb.append(if (i < pos) '=' else if (i == pos) '>' else '-')
        screen.text(0, y, sb.toString().take(cols), Palette.CYAN)
        y++
    }
    val half = (options.size + 1) / 2
    val col2 = max(8, contentW / 2)
    options.forEachIndexed { i, (label, id) ->
        val cx = if (i < half) 0 else min(col2, cols - 1)
        val cy = y + (i % half)
        if (cy >= rows) return@forEachIndexed
        screen.text(cx, cy, label, Palette.GREEN)
        screen.hotspot(id, cx, cy, label.length)
    }
}
