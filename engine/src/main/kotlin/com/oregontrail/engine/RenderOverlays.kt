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

/** A sun with turning rays, drifting clouds and the odd bird. */
internal fun Game.overlaySky(screen: Screen, top: Int, bottom: Int) {
    if (bottom <= top) return
    val kind = weather.kind
    val fair = kind == WeatherKind.CLEAR || kind == WeatherKind.HOT ||
        kind == WeatherKind.CLOUDY || kind == WeatherKind.WINDY
    if (fair && cols >= 14) {
        val sx = (cols - 7).coerceAtLeast(1)
        screen.putIfBlank(sx + 1, top, if (frame % 2 == 0) '\\' else '/', Palette.BRIGHT_YELLOW)
        screen.putIfBlank(sx + 5, top, if (frame % 2 == 0) '/' else '\\', Palette.BRIGHT_YELLOW)
        screen.text(sx, top + 1, "-(o)-", Palette.BRIGHT_YELLOW)
        screen.putIfBlank(sx + 1, top + 2, if (frame % 2 == 0) '/' else '\\', Palette.BRIGHT_YELLOW)
        screen.putIfBlank(sx + 5, top + 2, if (frame % 2 == 0) '\\' else '/', Palette.BRIGHT_YELLOW)
    }
    val cloudy = kind == WeatherKind.CLOUDY || kind == WeatherKind.RAIN ||
        kind == WeatherKind.HEAVY_RAIN || kind == WeatherKind.THUNDERSTORM ||
        kind == WeatherKind.WINDY || kind == WeatherKind.SNOW || kind == WeatherKind.BLIZZARD
    if (cloudy) {
        val n = if (kind == WeatherKind.HEAVY_RAIN || kind == WeatherKind.THUNDERSTORM ||
            kind == WeatherKind.BLIZZARD
        ) 3 else 2
        val bandH = bottom - top
        for (i in 0 until n) {
            val x = ((i * 23 + frame) % (cols + 8)) - 4
            val y = top + (i * 3) % bandH
            screen.putIfBlank(x, y, '(', Palette.GRAY)
            screen.putIfBlank(x + 1, y, '.', Palette.GRAY)
            screen.putIfBlank(x + 2, y, '.', Palette.GRAY)
            screen.putIfBlank(x + 3, y, ')', Palette.GRAY)
        }
    }
    if (fair && cols >= 20) {
        val bx = ((frame * 2) % (cols + 6)) - 3
        val by = (top + 1).coerceAtMost(bottom - 1)
        val bird = if (frame % 2 == 0) 'v' else '^'
        screen.putIfBlank(bx, by, bird, Palette.DIM)
        screen.putIfBlank(bx + 2, by, bird, Palette.DIM)
        // A tumbleweed rolls along the ground.
        val ty = (bottom - 1).coerceAtLeast(top)
        val tx = ((frame * 3) % (cols + 4)) - 2
        screen.putIfBlank(tx, ty, 'o', Palette.BROWN)
        // Now and then a shooting star streaks across.
        if (frame % 41 < 3 && cols >= 24) {
            val sx = ((frame * 5) % (cols + 12)) - 6
            val sy = top
            screen.putIfBlank(sx, sy, '\\', Palette.BRIGHT_WHITE)
            screen.putIfBlank(sx + 1, sy, '-', Palette.WHITE)
            screen.putIfBlank(sx + 2, sy, '-', Palette.DIM)
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
