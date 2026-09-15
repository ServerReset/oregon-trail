package com.oregontrail.engine

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

internal fun Game.renderMap(screen: Screen) {
    if (ultraCompact) {
        screen.center(0, "MAP", Palette.BRIGHT_GREEN, bold = true)
        val all = Data.landmarks
        val windowSize = min(all.size, max(4, rows - 2))
        val startIdx = (landmarkIndex - windowSize / 2).coerceIn(0, max(0, all.size - windowSize))
        val endIdx = min(all.size, startIdx + windowSize)
        var y = 1
        for (i in startIdx until endIdx) {
            if (y >= rows - 1) break
            val lm = all[i]
            val marker = when {
                i < landmarkIndex -> "x"
                i == landmarkIndex -> "*"
                else -> "o"
            }
            val color = when (marker) {
                "x" -> Palette.GRAY
                "*" -> Palette.BRIGHT_YELLOW
                else -> Palette.GREEN
            }
            val name = shortLandmarkName(lm).take((cols - 3).coerceAtLeast(4))
            screen.text(0, y, "$marker $name", color, bold = marker == "*")
            y++
        }
        val back = "[X]"
        screen.text(0, rows - 1, back, Palette.BRIGHT_GREEN, bold = true)
        screen.hotspot("map:back", 0, rows - 1, back.length)
        return
    }
    screen.center(0, "MAP OF THE OREGON TRAIL", Palette.BRIGHT_GREEN, bold = true)
    pauseButton(screen)
    screen.text(marginX + 1, 2, "--> West                          East", Palette.DIM)
    val all = Data.landmarks
    val windowSize = min(all.size, max(8, rows - 6))
    val startIdx = (landmarkIndex - windowSize / 2).coerceIn(0, max(0, all.size - windowSize))
    val endIdx = (startIdx + windowSize).coerceAtMost(all.size)
    var y = 4
    for (i in startIdx until endIdx) {
        val lm = all[i]
        val marker = when {
            i < landmarkIndex -> "x"
            i == landmarkIndex -> if (contentW >= 34) "[_]" else ">"
            else -> "o"
        }
        val color = when {
            i < landmarkIndex -> Palette.GRAY
            i == landmarkIndex -> Palette.BRIGHT_YELLOW
            else -> Palette.GREEN
        }
        val name = if (contentW >= 44) mapShortName(lm.id) else shortLandmarkName(lm)
        screen.text(marginX + 1, y, " $marker $name", color, bold = i == landmarkIndex)
        y++
    }
    screen.text(marginX + 1, y, "  Mile $miles of ${Data.TOTAL_MILES}", Palette.WHITE)
    val back = "[ Back ]"
    screen.text(marginX + 1, rows - 2, back, Palette.BRIGHT_GREEN, bold = true)
    screen.hotspot("map:back", marginX + 1, rows - 2, back.length)
}

internal fun Game.mapShortName(id: String): String = when (id) {
    "independence" -> "Independence, MO"
    "kansas" -> "Kansas River"
    "bigblue" -> "Big Blue River"
    "kearney" -> "Fort Kearney"
    "chimney" -> "Chimney Rock"
    "laramie" -> "Fort Laramie"
    "independence_rock" -> "Independence Rock"
    "southpass" -> "South Pass"
    "green" -> "Green River"
    "bridger" -> "Fort Bridger"
    "soda" -> "Soda Springs"
    "hall" -> "Fort Hall"
    "snake" -> "Snake River"
    "boise" -> "Fort Boise"
    "bluemountains" -> "Blue Mountains"
    "wallawalla" -> "Fort Walla Walla"
    "dalles" -> "The Dalles"
    "willamette" -> "Willamette Valley"
    else -> id
}
