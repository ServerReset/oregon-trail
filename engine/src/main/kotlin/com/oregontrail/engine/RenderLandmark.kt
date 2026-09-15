package com.oregontrail.engine

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

internal fun Game.renderLandmark(screen: Screen) {
    val lm = Data.landmarkAt(landmarkIndex)
    if (ultraCompact) {
        screen.center(0, shortLandmarkName(lm).uppercase(), Palette.BRIGHT_GREEN, bold = true)
        if (lm.id == "dalles") {
            renderMenuColumns(
                screen, 1,
                listOf(
                    "1.Barlow" to "dalles:barlow",
                    "2.Raft" to "dalles:raft",
                    "3.Port" to "dalles:portage",
                    "4.Wait" to "dalles:wait"
                )
            )
            return
        }
        val options = ArrayList<Pair<String, String>>()
        if (lm.kind == LandmarkKind.FORT) options.add("1.Buy" to "land:buy")
        var n = if (lm.kind == LandmarkKind.FORT) 2 else 1
        options.add("${n++}.Go" to "land:continue")
        if (lm.cutoffId != null && Data.indexOf(lm.cutoffId) > landmarkIndex + 1) {
            options.add("${n++}.Cut" to "land:cutoff")
        }
        options.add("${n++}.Inv" to "land:supplies")
        options.add("${n++}.Hist" to "land:fact")
        options.add("${n++}.Map" to "land:map")
        options.add("${n++}.Rest" to "land:rest")
        if (lm.kind == LandmarkKind.FORT || lm.kind == LandmarkKind.LANDMARK) {
            options.add("${n++}.Talk" to "land:talk")
        }
        if (lm.kind != LandmarkKind.RIVER) options.add("${n++}.Hunt" to "land:hunt")
        renderMenuColumns(screen, 1, options)
        return
    }
    screen.center(0, lm.name.uppercase(), Palette.BRIGHT_GREEN, bold = true)
    pauseButton(screen)
    var y = 2
    if (rows >= 26) {
        val art = landmarkArt(lm)
        val artTop = y
        Ascii.draw(screen, (cols - Ascii.width(art)) / 2, y, art, Palette.GREEN)
        val artBottom = artTop + Ascii.height(art)
        y += Ascii.height(art) + 1
        overlaySky(screen, artTop, artBottom)
        overlayWeather(screen, artTop, artBottom)
        overlayWildlife(screen, artTop, artBottom)
    }
    y = screen.wrap(marginX + 1, y, contentW - 2, lm.blurb.joinToString(" "), Palette.GREEN)
    // A little postcard for travellers who linger.
    TrailQuotes.byLandmark[lm.id]?.let { quote ->
        if (rows >= 30 && y + 3 < rows - 6) {
            y++
            y = screen.wrap(marginX + 1, y, contentW - 2, "\"$quote\"", Palette.DIM)
        }
    }
    y++
    if (lm.id == "dalles") {
        screen.text(marginX + 1, y, "The last decision of the trail:", Palette.BRIGHT_YELLOW)
        y++
        val toll = 5.0
        val barlow = if (inventory.cash < toll) {
            "1. Take the Barlow Road (toll $5 - can't afford)"
        } else {
            "1. Take the Barlow Road (toll $5)"
        }
        val options = listOf(
            barlow to "dalles:barlow",
            "2. Raft down the Columbia River" to "dalles:raft",
            "3. Portage around the rapids" to "dalles:portage",
            "4. Wait for better weather" to "dalles:wait"
        )
        screen.menuAt(marginX + 1, y, options)
        return
    }
    val options = ArrayList<Pair<String, String>>()
    if (lm.kind == LandmarkKind.FORT) options.add("1. Buy supplies" to "land:buy")
    var n = if (lm.kind == LandmarkKind.FORT) 2 else 1
    options.add("${n++}. Continue on the trail" to "land:continue")
    if (lm.cutoffId != null && Data.indexOf(lm.cutoffId) > landmarkIndex + 1) {
        options.add("${n++}. ${lm.cutoffLabel}" to "land:cutoff")
    }
    options.add("${n++}. Check supplies" to "land:supplies")
    options.add("${n++}. Learn the history" to "land:fact")
    options.add("${n++}. Look at the map" to "land:map")
    options.add("${n++}. Stop to rest" to "land:rest")
    if (lm.kind == LandmarkKind.FORT || lm.kind == LandmarkKind.LANDMARK) {
        options.add("${n++}. Talk to people" to "land:talk")
    }
    if (lm.kind == LandmarkKind.FORT || lm.kind == LandmarkKind.LANDMARK || lm.kind == LandmarkKind.MOUNTAINS) {
        options.add("${n++}. Hunt for food" to "land:hunt")
    }
    screen.menuAt(marginX + 1, y, options)
}

/** Picks artwork for a landmark, with special pieces for famous places. */
internal fun Game.landmarkArt(lm: Landmark): List<String> = when (lm.id) {
    "independence" -> AsciiLandmarks.town
    "chimney" -> AsciiLandmarks.chimneyRock
    "southpass" -> AsciiLandmarks.southPass
    "dalles" -> AsciiLandmarks.dalles
    "independence_rock" -> AsciiScenery.rock
    else -> when (lm.kind) {
        LandmarkKind.FORT -> AsciiScenery.fort
        LandmarkKind.MOUNTAINS -> AsciiScenery.mountains
        LandmarkKind.RIVER -> riverArt(frame)
        else -> AsciiScenery.rock
    }
}

/** Shortens a landmark name for tiny screens. */
internal fun Game.shortLandmarkName(lm: Landmark): String = when (lm.id) {
    "dalles" -> "The Dalles"
    "independence" -> "Independence"
    "independence_rock" -> "Ind. Rock"
    "bluemountains" -> "Blue Mtns"
    "wallawalla" -> "Ft Walla"
    "snake" -> "Snake Riv"
    "green" -> "Green Riv"
    "bigblue" -> "Big Blue"
    "kansas" -> "Kansas"
    else -> lm.name
}
