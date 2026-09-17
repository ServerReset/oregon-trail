package com.oregontrail.engine


/** Rain, snow and hail drifting through a band, plus wind, fog and lightning. */
internal fun Game.overlayWeather(screen: Screen, top: Int, bottom: Int) {
    if (bottom <= top) return
    val bandH = bottom - top
    val kind = weather.kind
    var ch = ' '
    var step = 1
    var count = 0
    var color = Palette.DEFAULT
    when (kind) {
        WeatherKind.SNOW -> { ch = '*'; step = 1; count = 26; color = Palette.BRIGHT_WHITE }
        WeatherKind.BLIZZARD -> { ch = '*'; step = 1; count = 46; color = Palette.WHITE }
        WeatherKind.HAIL -> { ch = 'o'; step = 2; count = 22; color = Palette.CYAN }
        WeatherKind.RAIN -> { ch = '/'; step = 2; count = 28; color = Palette.CYAN }
        WeatherKind.HEAVY_RAIN -> { ch = '/'; step = 2; count = 44; color = Palette.BLUE }
        WeatherKind.THUNDERSTORM -> { ch = '/'; step = 3; count = 38; color = Palette.YELLOW }
        else -> {}
    }
    if (count > 0 && cols > 0) {
        for (i in 0 until count) {
            val x = (i * 37 + frame / 2) % cols
            val y = top + (i * 53 + frame * step) % bandH
            val cell = screen.cell(x, y) ?: continue
            if (cell.ch == ' ') screen.put(x, y, ch, color)
        }
    }
    if (kind == WeatherKind.THUNDERSTORM) lightningBolt(screen, top, bottom)
    if (kind == WeatherKind.WINDY) windStreaks(screen, top, bottom)
    if (kind == WeatherKind.FOG) fogBanks(screen, top, bottom)
}

/** A short jagged bolt flickers down the band every couple of seconds. */
private fun Game.lightningBolt(screen: Screen, top: Int, bottom: Int) {
    val bandH = bottom - top
    if (bandH < 3 || cols < 10 || frame % 90 >= 3) return
    val x = (cols / 2 + frame % 5 - 2).coerceIn(1, cols - 2)
    val len = (bandH - 1).coerceAtMost(6)
    for (i in 0 until len) {
        val dx = when (i % 4) { 1 -> 1; 3 -> -1; else -> 0 }
        val bx = (x + dx).coerceIn(0, cols - 1)
        screen.putIfBlank(bx, top + i, if (i % 2 == 0) '\\' else '/', Palette.BRIGHT_WHITE)
    }
    screen.putIfBlank(x, top, '-', Palette.WHITE)
}

/** Dashes of air racing sideways through the band. */
private fun Game.windStreaks(screen: Screen, top: Int, bottom: Int) {
    val bandH = bottom - top
    if (bandH < 1 || cols < 12) return
    val n = (cols / 14).coerceIn(2, 5)
    for (i in 0 until n) {
        val x = ((i * 17 + frame * 3) % (cols + 10)) - 5
        val y = top + (i * 3) % bandH
        val len = 3 + i % 2
        val glyph = if (i % 2 == 0) '~' else '-'
        for (j in 0 until len) screen.putIfBlank(x + j, y, glyph, Palette.GRAY)
    }
}

/** Slow horizontal banks of haze that drift across the band. */
private fun Game.fogBanks(screen: Screen, top: Int, bottom: Int) {
    val bandH = bottom - top
    if (bandH < 1 || cols < 8) return
    for (i in 0 until bandH) {
        if (i % 2 == 1) continue
        val x = ((i * 11 + frame / 4) % (cols + 12)) - 6
        for (j in 0 until 7) {
            val g = if ((j + i) % 3 == 0) '-' else '.'
            screen.putIfBlank(x + j, top + i, g, Palette.GRAY)
        }
    }
}

/** Smoke curling up from the campfire. */
internal fun Game.overlaySmoke(screen: Screen, artTop: Int, artH: Int) {
    if (artH <= 2) return
    val cx = cols / 2
    for (i in 0 until 4) {
        val rise = (frame / 2 + i * 2) % (artH - 1)
        val y = artTop + artH - 2 - rise
        val x = cx + ((i % 3) - 1)
        screen.putIfBlank(x, y, if (i % 2 == 0) '.' else 'o', Palette.GRAY)
    }
    // Now and then a spark winks out of the flames.
    if (frame % 6 < 2) {
        screen.putIfBlank(cx + (frame % 3 - 1), artTop + artH - 2, '*', Palette.YELLOW)
    }
}

/** A lone wild animal crossing the plains now and then. */
internal fun Game.overlayWildlife(screen: Screen, top: Int, bottom: Int) {
    if (bottom - top < 2 || cols < 18) return
    val period = 150
    val phase = frame % period
    if (phase > 70) return
    val spawn = frame / period
    val kinds = listOf('d', 'w', 'b')
    val glyph = kinds[((spawn % kinds.size) + kinds.size) % kinds.size]
    val color = when (glyph) {
        'b' -> Palette.BROWN
        'w' -> Palette.GRAY
        else -> Palette.YELLOW
    }
    val x = phase * (cols + 4) / 70 - 2
    val y = (bottom - 1).coerceAtLeast(top)
    val bob = if (frame % 2 == 0) 1 else 0
    screen.putIfBlank(x, y, glyph, color)
    screen.putIfBlank(x + 1, y, glyph, color)
    if (bob == 1) screen.putIfBlank(x, y - 1, '.', color)
}

/** Sunlight sparkling on a calm river, with soft drifting wave crests. */
internal fun Game.overlayWater(screen: Screen, top: Int, bottom: Int) {
    if (bottom - top < 2) return
    val kind = weather.kind
    if (kind != WeatherKind.CLEAR && kind != WeatherKind.HOT) return
    val bandH = bottom - top
    for (i in 0 until 6) {
        if ((i + frame / 2) % 3 != 0) continue
        val x = (i * 29 + frame / 3) % cols
        val y = top + (i * 17 + frame) % bandH
        if (screen.cell(x, y)?.ch == ' ') screen.put(x, y, '*', Palette.BRIGHT_WHITE)
    }
    if (cols < 10) return
    // A gentle wave pattern on alternating rows, drifting a touch slower.
    val rows = (bandH + 1) / 2
    for (i in 0 until rows) {
        val y = top + i * 2
        val x = ((i * 13 + frame / 4) % (cols + 8)) - 4
        screen.putIfBlank(x, y, '~', Palette.CYAN)
        screen.putIfBlank(x + 1, y, '~', Palette.CYAN)
        screen.putIfBlank(x + 5, y, '-', Palette.BLUE)
    }
}
