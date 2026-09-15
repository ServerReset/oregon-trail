package com.oregontrail.engine

import kotlin.math.PI
import kotlin.math.sin

/**
 * The sky over the trail: a sun that arcs and sinks toward the west as the day
 * passes, stars that come out at night, drifting clouds, birds, a tumbleweed
 * and the occasional shooting star. Everything decorates the scene with
 * [Screen.putIfBlank] so it never overwrites the artwork.
 */
internal fun Game.overlaySky(screen: Screen, top: Int, bottom: Int) {
    if (bottom <= top) return
    val kind = weather.kind
    val fair = kind == WeatherKind.CLEAR || kind == WeatherKind.HOT ||
        kind == WeatherKind.CLOUDY || kind == WeatherKind.WINDY
    val p = dayPhase()
    val night = p > 0.88

    if (fair && cols >= 16) {
        if (night) {
            for (i in 0 until cols / 6) {
                val x = (i * 13 + 3) % cols
                val y = (top + i % 2).coerceAtMost(bottom - 1)
                screen.putIfBlank(x, y, if ((i + frame / 4) % 2 == 0) '*' else '.', Palette.DIM)
            }
        } else {
            drawSun(screen, top, bottom, p)
        }
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

    if (fair && !night && cols >= 20) {
        val bx = ((frame * 2) % (cols + 6)) - 3
        val by = (top + 1).coerceAtMost(bottom - 1)
        val bird = if (frame % 2 == 0) 'v' else '^'
        screen.putIfBlank(bx, by, bird, Palette.DIM)
        screen.putIfBlank(bx + 2, by, bird, Palette.DIM)
        val ty = (bottom - 1).coerceAtLeast(top)
        val tx = ((frame * 3) % (cols + 4)) - 2
        screen.putIfBlank(tx, ty, 'o', Palette.BROWN)
    }
    // Shooting stars only show at night.
    if (night && cols >= 24 && frame % 41 < 3) {
        val sx = ((frame * 5) % (cols + 12)) - 6
        screen.putIfBlank(sx, top, '\\', Palette.BRIGHT_WHITE)
        screen.putIfBlank(sx + 1, top, '-', Palette.WHITE)
        screen.putIfBlank(sx + 2, top, '-', Palette.DIM)
    }
}

/** The sun's disc and rays, placed along its daily arc. */
private fun Game.drawSun(screen: Screen, top: Int, bottom: Int, p: Double) {
    val span = (cols - 8).coerceAtLeast(4)
    val sx = (p * span).toInt().coerceIn(1, cols - 8)
    val arc = sin(p * PI).coerceIn(0.0, 1.0)
    // Keep the sun in the sky band so it never sits on the hills.
    val skyH = ((bottom - top).coerceAtMost(3)).coerceAtLeast(1)
    val sy = (top + skyH - 1 - (arc * (skyH - 1)).toInt()).coerceIn(top, top + skyH - 1)
    val color = if (arc > 0.35) Palette.BRIGHT_YELLOW else Palette.YELLOW
    screen.putIfBlank(sx + 1, sy, if (frame % 2 == 0) '\\' else '/', color)
    screen.putIfBlank(sx + 5, sy, if (frame % 2 == 0) '/' else '\\', color)
    screen.putIfBlank(sx + 2, sy, '-', color)
    screen.putIfBlank(sx + 3, sy, 'o', color)
    screen.putIfBlank(sx + 4, sy, '-', color)
}
