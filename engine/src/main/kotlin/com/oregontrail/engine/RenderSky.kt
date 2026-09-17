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
    // A little flock of birds crosses the fair-weather sky now and then.
    if (fair && !night && cols >= 16) {
        val win = 220
        val ph = frame % win
        if (ph < 60) {
            val n = 3 + (frame / win) % 3
            val x0 = ph * (cols + 10) / 60 - 5
            for (i in 0 until n) {
                val bx = x0 + i * 2 + i % 2
                val by = top + i % 2
                if (by in top until bottom) {
                    screen.putIfBlank(bx, by, if ((frame + i) % 2 == 0) 'v' else '^', Palette.DIM)
                }
            }
        }
    }
    // A rainbow lingers for a few frames once the rain has moved off.
    if (kind == WeatherKind.CLEAR && frame % 600 < 36) {
        drawRainbow(screen, top, bottom)
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

/** A shallow, many-hued arch that only fits when the band is tall enough. */
private fun Game.drawRainbow(screen: Screen, top: Int, bottom: Int) {
    if (bottom - top < 2 || cols < 12) return
    val h = (bottom - top).coerceAtMost(3)
    val w = (cols / 3).coerceIn(7, 15)
    val x0 = ((cols - w) / 2).coerceAtLeast(1)
    val baseY = bottom - 1
    screen.putIfBlank(x0, baseY, '(', Palette.MAGENTA)
    screen.putIfBlank(x0 + w - 1, baseY, ')', Palette.MAGENTA)
    if (h >= 2) {
        screen.putIfBlank(x0 + 1, baseY - 1, '/', Palette.MAGENTA)
        screen.putIfBlank(x0 + w - 2, baseY - 1, '\\', Palette.YELLOW)
    }
    val topY = if (h >= 3) baseY - 2 else baseY - 1
    for (i in 2 until w - 2) {
        screen.putIfBlank(x0 + i, topY, if (i % 2 == 0) '_' else '-', Palette.CYAN)
    }
}
