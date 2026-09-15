package com.oregontrail.engine

/**
 * Weather settling on the ground: snow drifts and rain puddles along the base
 * of a scene. Drawn with [Screen.putIfBlank] so it decorates the artwork.
 */
internal fun Game.overlayGround(screen: Screen, top: Int, bottom: Int) {
    if (bottom - top < 2) return
    val y = (bottom - 1).coerceAtLeast(top)
    when (weather.kind) {
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
}
