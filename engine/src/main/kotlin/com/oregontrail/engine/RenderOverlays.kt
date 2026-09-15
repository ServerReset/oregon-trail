package com.oregontrail.engine


/** Rain, snow and hail drifting through a band of the screen. */
internal fun Game.overlayWeather(screen: Screen, top: Int, bottom: Int) {
    val ch: Char
    val step: Int
    val count: Int
    val color: Palette
    when (weather.kind) {
        WeatherKind.SNOW -> { ch = '*'; step = 1; count = 26; color = Palette.BRIGHT_WHITE }
        WeatherKind.BLIZZARD -> { ch = '*'; step = 1; count = 46; color = Palette.WHITE }
        WeatherKind.HAIL -> { ch = 'o'; step = 2; count = 22; color = Palette.CYAN }
        WeatherKind.RAIN -> { ch = '/'; step = 2; count = 28; color = Palette.CYAN }
        WeatherKind.HEAVY_RAIN -> { ch = '/'; step = 2; count = 44; color = Palette.BLUE }
        WeatherKind.THUNDERSTORM -> { ch = '/'; step = 3; count = 38; color = Palette.YELLOW }
        else -> return
    }
    if (bottom <= top) return
    val bandH = bottom - top
    for (i in 0 until count) {
        val x = (i * 37 + frame / 2) % cols
        val y = top + (i * 53 + frame * step) % bandH
        val cell = screen.cell(x, y) ?: continue
        if (cell.ch == ' ') screen.put(x, y, ch, color)
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

/** Sunlight sparkling on a calm river. */
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
}
