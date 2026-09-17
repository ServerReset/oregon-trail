package com.oregontrail.engine

/**
 * Weather settling on the ground: snow drifts and rain puddles along the base
 * of a scene. Drawn with [Screen.putIfBlank] so it decorates the artwork.
 */
internal fun Game.overlayGround(screen: Screen, top: Int, bottom: Int) {
    if (bottom - top < 2) return
    val kind = weather.kind
    val y = (bottom - 1).coerceAtLeast(top)
    when (kind) {
        WeatherKind.SNOW, WeatherKind.BLIZZARD -> {
            for (i in 0 until cols / 4) {
                val x = (i * 7 + frame / 4) % cols
                screen.putIfBlank(x, y, '*', Palette.BRIGHT_WHITE)
            }
        }
        WeatherKind.RAIN, WeatherKind.HEAVY_RAIN, WeatherKind.THUNDERSTORM -> {
            for (i in 0 until cols / 6) {
                val x = (i * 11 + frame / 3) % cols
                screen.putIfBlank(x, y, '~', Palette.CYAN)
            }
        }
        else -> {}
    }
    if (kind == WeatherKind.HOT) heatShimmer(screen, top, bottom)
    if (kind == WeatherKind.SNOW || kind == WeatherKind.BLIZZARD) frost(screen, top, bottom)
}

/** Heat rising off the trail: faint flickers on alternating frames. */
private fun Game.heatShimmer(screen: Screen, top: Int, bottom: Int) {
    if (bottom - top < 2 || cols < 8) return
    val y = bottom - 2
    val g = if (frame % 2 == 0) '.' else '\''
    for (i in 0 until cols / 8) {
        val x = (i * 13 + frame / 2) % cols
        screen.putIfBlank(x, y, g, Palette.YELLOW)
    }
    if (frame % 4 < 2) {
        screen.putIfBlank((cols / 2 + frame % 5) % cols, bottom - 1, '\'', Palette.YELLOW)
    }
}

/** A few icy specks glinting just above the snow line. */
private fun Game.frost(screen: Screen, top: Int, bottom: Int) {
    if (bottom - top < 2 || cols < 8 || frame % 7 >= 2) return
    val y = bottom - 2
    for (i in 0 until cols / 9) {
        val x = (i * 19 + frame) % cols
        screen.putIfBlank(x, y, if ((i + frame) % 2 == 0) '\'' else '*', Palette.BRIGHT_WHITE)
    }
    screen.putIfBlank((cols / 3 + frame) % cols, bottom - 1, '*', Palette.BRIGHT_WHITE)
}
