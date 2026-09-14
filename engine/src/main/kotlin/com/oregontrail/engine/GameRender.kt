package com.oregontrail.engine

import kotlin.math.max
import kotlin.math.min

/*
 * All screen rendering for [Game] lives here so the rules and the presentation
 * stay in separate files. Every function is a file-private extension on Game,
 * called by Game.render().
 */

// ====================================================================
//  Rendering
// ====================================================================


internal val Game.contentW: Int get() = min(cols, 76)
internal val Game.marginX: Int get() = ((cols - contentW) / 2).coerceAtLeast(0)

/**
 * True on very small displays: watches, phone cover screens and tiny split
 * windows. These layouts drop artwork and long prose and use short labels so
 * the game stays playable.
 */
internal val Game.ultraCompact: Boolean get() = cols < 26 || rows < 16

/** Even tighter: a watch in particular. */
internal val Game.watchLike: Boolean get() = cols < 22 || rows < 13

private fun Screen.menuAt(x: Int, yStart: Int, options: List<Pair<String, String>>): Int {
    var y = yStart
    for ((label, id) in options) {
        text(x, y, label, Palette.GREEN)
        hotspot(id, x, y, label.length)
        y++
    }
    return y
}

private fun Screen.footer(rows: Int, s: String) {
    center(rows - 1, s, Palette.DIM)
}

internal fun Game.renderTitle(screen: Screen) {
    if (ultraCompact) {
        renderTitleCompact(screen)
        return
    }
    val art = ArrayList<String>()
    if (contentW >= 36 && rows >= 30) {
        art.addAll(Ascii.wagon)
        art.addAll(Ascii.blockWord("OREGON"))
        art.addAll(Ascii.blockWord("TRAIL"))
    } else {
        art.addAll(Ascii.wagonSmall)
        art.add("")
        art.add("T H E   O R E G O N   T R A I L")
    }
    val menu = listOf(
        "1. Travel the trail" to "title:travel",
        "2. Learn about the trail" to "title:about",
        "3. See the Oregon Top Ten" to "title:topten",
        "4. Choose Management Options" to "title:manage",
        "5. End" to "title:end"
    )
    val totalH = art.size + 2 + menu.size
    var y = ((rows - totalH) / 2).coerceAtLeast(0)
    Ascii.draw(screen, (cols - Ascii.width(art)) / 2, y, art, Palette.BRIGHT_GREEN)
    y += art.size + 1
    val x = (cols - menu.maxOf { it.first.length }) / 2
    for ((label, id) in menu) {
        screen.text(x, y, label, Palette.GREEN)
        screen.hotspot(id, x, y, label.length)
        y++
    }
    screen.footer(rows, "Oregon Trail v${Game.VERSION}")
}

/** Watch / cover-screen title: no artwork, short labels, everything on screen. */
private fun Game.renderTitleCompact(screen: Screen) {
    val menu = listOf(
        "1. Travel" to "title:travel",
        "2. About" to "title:about",
        "3. Top Ten" to "title:topten",
        "4. Options" to "title:manage",
        "5. End" to "title:end"
    )
    val totalH = 3 + menu.size
    var y = ((rows - totalH) / 2).coerceAtLeast(0)
    screen.center(y, "OREGON", Palette.BRIGHT_YELLOW, bold = true)
    y++
    screen.center(y, "TRAIL", Palette.BRIGHT_YELLOW, bold = true)
    y += 2
    val x = ((cols - menu.maxOf { it.first.length }) / 2).coerceAtLeast(0)
    for ((label, id) in menu) {
        if (y >= rows) break
        screen.text(x, y, label, Palette.GREEN)
        screen.hotspot(id, x, y, label.length)
        y++
    }
}

internal fun Game.renderAbout(screen: Screen) {
    val page = Game.ABOUT_PAGES[aboutPage.coerceIn(0, Game.ABOUT_PAGES.size - 1)]
    if (ultraCompact) {
        var y = 0
        y = screen.wrap(0, y, cols, page, Palette.GREEN)
        val label = if (aboutPage >= Game.ABOUT_PAGES.size - 1) "[X]" else "[>]"
        screen.text(0, rows - 1, label, Palette.BRIGHT_GREEN, bold = true)
        screen.hotspot("about:next", 0, rows - 1, label.length)
        return
    }
    screen.center(1, "ABOUT THE OREGON TRAIL", Palette.BRIGHT_GREEN, bold = true)
    var y = 3
    y = screen.wrap(marginX + 1, y, contentW - 2, page, Palette.GREEN)
    screen.text(marginX + 1, rows - 3, "Page ${aboutPage + 1} of ${Game.ABOUT_PAGES.size}", Palette.DIM)
    val label = if (aboutPage >= Game.ABOUT_PAGES.size - 1) "[ Back to title ]" else "[ Continue ]"
    screen.text(marginX + 1, rows - 2, label, Palette.BRIGHT_GREEN, bold = true)
    screen.hotspot("about:next", marginX + 1, rows - 2, label.length)
}

internal fun Game.renderManagement(screen: Screen) {
    if (ultraCompact) {
        val short = ArrayList<Pair<String, String>>()
        short.add("Top Ten" to "manage:topten")
        short.add("New leader" to "manage:newleader")
        short.add("Diff ${difficulty.displayName.take(4)}" to "manage:difficulty")
        short.add("Sound ${onOff(soundEnabled)}" to "manage:sound")
        uiSettings?.let { ui ->
            short.add("Text ${textScaleName(ui.textScaleIndex).take(1)}" to "manage:textsize")
            short.add("Contr ${onOff(ui.highContrast)}" to "manage:contrast")
            short.add("Scan ${onOff(ui.scanlines)}" to "manage:scanlines")
        }
        short.add("Back" to "manage:back")
        renderMenuColumns(screen, 1, short)
        return
    }
    screen.center(1, "MANAGEMENT OPTIONS", Palette.BRIGHT_GREEN, bold = true)
    val options = ArrayList<Pair<String, String>>()
    options.add("See the Oregon Top Ten" to "manage:topten")
    options.add("Choose a different leader" to "manage:newleader")
    options.add("Difficulty: ${difficulty.displayName}" to "manage:difficulty")
    options.add("Sound is ${if (soundEnabled) "ON" else "OFF"}" to "manage:sound")
    uiSettings?.let { ui ->
        options.add("Text size: ${textScaleName(ui.textScaleIndex)}" to "manage:textsize")
        options.add("High contrast: ${onOff(ui.highContrast)}" to "manage:contrast")
        options.add("Scanlines: ${onOff(ui.scanlines)}" to "manage:scanlines")
    }
    options.add("Return to the title screen" to "manage:back")
    val step = if (rows < 26) 1 else 2
    var y = 3
    for ((label, id) in options) {
        if (y >= rows - 1) break
        screen.text(marginX + 2, y, label, Palette.GREEN)
        screen.hotspot(id, marginX + 2, y, label.length)
        y += step
    }
}

internal fun Game.textScaleName(index: Int): String = when (index) {
    0 -> "Small"
    1 -> "Medium"
    else -> "Large"
}

internal fun Game.onOff(value: Boolean): String = if (value) "ON" else "OFF"

internal fun Game.renderTopTen(screen: Screen) {
    if (ultraCompact) {
        screen.center(0, "TOP TEN", Palette.BRIGHT_GREEN, bold = true)
        var y = 1
        topTen.take(10).forEachIndexed { i, e ->
            if (y >= rows - 1) return@forEachIndexed
            val line = "${i + 1}.${e.name.take((cols - 9).coerceAtLeast(4))} ${e.points}"
            screen.text(0, y, line.take(cols), Palette.GREEN)
            y++
        }
        val back = "[X]"
        screen.text(0, rows - 1, back, Palette.BRIGHT_GREEN, bold = true)
        screen.hotspot("topten:back", 0, rows - 1, back.length)
        return
    }
    screen.center(1, "THE OREGON TOP TEN", Palette.BRIGHT_GREEN, bold = true)
    val nameW = (contentW - 12).coerceIn(8, 20)
    screen.text(marginX + 2, 3, "Rank  Name".padEnd(nameW + 8) + "Points", Palette.YELLOW)
    var y = 5
    topTen.take(10).forEachIndexed { i, e ->
        if (y >= rows - 3) return@forEachIndexed
        val name = e.name.take(nameW).padEnd(nameW)
        val points = e.points.toString().padStart(5)
        val line = "${(i + 1).toString().padStart(2)}.   $name $points"
        screen.text(marginX + 1, y, line, Palette.GREEN)
        y++
    }
    val label = "[ Back ]"
    screen.text(marginX + 1, rows - 2, label, Palette.BRIGHT_GREEN, bold = true)
    screen.hotspot("topten:back", marginX + 1, rows - 2, label.length)
}

internal fun Game.renderProfession(screen: Screen) {
    val compact = rows < 24
    if (ultraCompact) {
        screen.center(0, "OCCUPATION", Palette.BRIGHT_GREEN, bold = true)
        Occupation.entries.forEachIndexed { i, occ ->
            val y = 2 + i
            if (y >= rows) return
            val label = "${i + 1}. ${occ.displayName}"
            screen.text(0, y, label, Palette.BRIGHT_YELLOW, bold = true)
            screen.hotspot("prof:$i", 0, y, label.length)
        }
        return
    }
    screen.center(1, "CHOOSE YOUR OCCUPATION", Palette.BRIGHT_GREEN, bold = true)
    var y = 3
    y = screen.wrap(marginX + 1, y, contentW - 2,
        "Your occupation decides how much money you start with and how many points you earn.", Palette.GRAY)
    y++
    Occupation.entries.forEachIndexed { i, occ ->
        screen.text(marginX + 1, y, "${i + 1}. ${occ.displayName} - $${occ.startingMoney}", Palette.BRIGHT_YELLOW, bold = true)
        screen.hotspot("prof:$i", marginX + 1, y, 20)
        y++
        if (!compact) {
            y = screen.wrap(marginX + 4, y, contentW - 5, occ.blurb, Palette.GREEN)
            y = screen.wrap(marginX + 4, y, contentW - 5, occ.perk, Palette.CYAN)
            y++
        }
    }
}

internal fun Game.renderMonth(screen: Screen) {
    if (ultraCompact) {
        screen.center(0, "MONTH", Palette.BRIGHT_GREEN, bold = true)
        TravelMonth.entries.forEachIndexed { i, m ->
            val y = 2 + i
            if (y >= rows) return
            val label = "${i + 1}. ${m.displayName}"
            screen.text(1, y, label, Palette.GREEN)
            screen.hotspot("month:$i", 1, y, label.length)
        }
        return
    }
    val compact = rows < 22
    screen.center(1, "WHEN DO YOU WANT TO START?", Palette.BRIGHT_GREEN, bold = true)
    var y = 3
    if (!compact) {
        y = screen.wrap(marginX + 1, y, contentW - 2,
            "Starting later means better grass, but winter may catch you in the mountains.", Palette.GRAY)
        y++
    }
    TravelMonth.entries.forEachIndexed { i, m ->
        screen.text(marginX + 3, y, "${i + 1}. ${m.displayName}", Palette.GREEN)
        screen.hotspot("month:$i", marginX + 3, y, 14)
        y += if (compact) 1 else 2
    }
}

internal fun Game.renderNames(screen: Screen) {
    if (ultraCompact) {
        screen.center(0, "PARTY", Palette.BRIGHT_GREEN, bold = true)
        party.forEachIndexed { i, m ->
            val y = 1 + i
            if (y >= rows - 1) return
            val label = "${i + 1}. ${m.name}".take(cols)
            screen.text(0, y, label, Palette.GREEN)
            screen.hotspot("name:$i", 0, y, label.length)
        }
        val go = "[ Start ]"
        screen.text(0, rows - 1, go, Palette.BRIGHT_GREEN, bold = true)
        screen.hotspot("names:go", 0, rows - 1, go.length)
        return
    }
    val compact = rows < 24
    screen.center(1, "NAME YOUR PARTY", Palette.BRIGHT_GREEN, bold = true)
    var y = 3
    if (!compact) {
        screen.wrap(marginX + 1, 3, contentW - 2,
            "Tap a name to change it. These five will travel with you.", Palette.GRAY)
        y = 6
    }
    party.forEachIndexed { i, m ->
        val label = "${i + 1}. ${m.name}"
        screen.text(marginX + 3, y, label, Palette.GREEN)
        screen.hotspot("name:$i", marginX + 3, y, label.length)
        y += if (compact) 1 else 2
    }
    val go = "[ Begin the journey ]"
    screen.text(marginX + 1, rows - 2, go, Palette.BRIGHT_GREEN, bold = true)
    screen.hotspot("names:go", marginX + 1, rows - 2, go.length)
}

internal fun Game.renderStore(screen: Screen) {
    if (ultraCompact) {
        renderStoreCompact(screen)
        return
    }
    val title = if (storeAtFort) "FORT TRADING POST" else "MATT'S GENERAL STORE"
    screen.center(0, title, Palette.BRIGHT_GREEN, bold = true)
    screen.text(marginX + 1, 1, "Cash: $${"%.2f".format(inventory.cash)}", Palette.BRIGHT_YELLOW, bold = true)
    screen.hline(marginX, 2, contentW, '-', Palette.DIM)

    // Adaptive column layout that always fits the smallest supported width.
    val available = (contentW - 2).coerceAtLeast(20)
    val btnW = 7          // "[-][+]"
    val qtyW = 6
    val priceW = 6
    val nameW = (available - btnW - qtyW - priceW).coerceIn(6, 16)
    val nameX = marginX + 1
    val priceX = nameX + nameW
    val qtyX = priceX + priceW
    val btnX = qtyX + qtyW

    var y = 3
    Item.entries.forEachIndexed { index, item ->
        val name = "$index ${shortItemName(item)}".padEnd(nameW).take(nameW)
        screen.text(nameX, y, name, Palette.GREEN)
        screen.text(priceX, y, "$" + "%.2f".format(priceOf(item)), Palette.GRAY)
        screen.text(qtyX, y, displayQty(item).toString().padStart(qtyW - 1), Palette.WHITE)
        screen.text(btnX, y, "[-][+]", Palette.BRIGHT_GREEN)
        screen.hotspot("store:dec:${item.name}", btnX, y, 3)
        screen.hotspot("store:inc:${item.name}", btnX + 3, y, 4)
        y++
    }
    screen.hline(marginX, y, contentW, '-', Palette.DIM)
    y++
    val leave = "[ Leave the store ]"
    screen.text(marginX + 1, y, leave, Palette.BRIGHT_GREEN, bold = true)
    screen.hotspot("store:leave", marginX + 1, y, leave.length)
    screen.footer(rows, "Ammunition is sold by the box of 20 bullets")
}

/**
 * Watch / cover-screen store: each row is a tappable "buy one" button (hold to
 * repeat). No price or quantity columns are needed at this size.
 */
private fun Game.renderStoreCompact(screen: Screen) {
    screen.center(0, "STORE", Palette.BRIGHT_GREEN, bold = true)
    screen.text(0, 1, "Cash \$${"%.0f".format(inventory.cash)}".take(cols), Palette.BRIGHT_YELLOW, bold = true)
    var y = 2
    Item.entries.forEachIndexed { i, item ->
        if (y >= rows - 1) return@forEachIndexed
        val label = "$i ${shortItemName(item)} ${displayQty(item)} \$${"%.0f".format(priceOf(item))}"
        screen.text(0, y, label.take(cols), Palette.GREEN)
        screen.hotspot("store:inc:${item.name}", 0, y, min(cols, label.length))
        y++
    }
    val leave = "[ Leave ]"
    screen.text(0, rows - 1, leave, Palette.BRIGHT_GREEN, bold = true)
    screen.hotspot("store:leave", 0, rows - 1, leave.length)
}

internal fun Game.shortItemName(item: Item): String = when (item) {    Item.OXEN -> "Oxen"
    Item.FOOD -> "Food"
    Item.CLOTHING -> "Cloths"
    Item.AMMUNITION -> "Ammo"
    Item.WHEEL -> "Wheel"
    Item.AXLE -> "Axle"
    Item.TONGUE -> "Tongue"
}

internal fun Game.renderTravel(screen: Screen) {
    if (ultraCompact) {
        renderTravelCompact(screen)
        return
    }
    val compact = rows < 30
    var y = 0
    screen.center(y, "THE OREGON TRAIL", Palette.BRIGHT_GREEN, bold = true)
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
        screen.text(marginX + 2, y + 1 + i, line, Palette.GREEN)
    }
    y += wrapped.size + 3

    if (!compact) {
        val scene = sceneArt()
        Ascii.draw(screen, (cols - Ascii.width(scene)) / 2, y, scene, Palette.GREEN)
        y += Ascii.height(scene) + 1
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
private fun Game.renderTravelCompact(screen: Screen) {
    val next = nextLandmark()
    val toNext = if (next != null) "${(next.mile - miles).coerceAtLeast(0)}mi" else "end"
    val status = listOf(
        "${date.monthName.take(3)} ${date.day}  ${weather.kind.displayName.take(8)}",
        "${miles}/${Data.TOTAL_MILES}mi  next $toNext",
        "fd${inventory.food} ammo${inventory.ammo} \$${"%.0f".format(inventory.cash)}"
    )
    var y = 0
    for (line in status) {
        screen.text(0, y, line.take(cols), Palette.GREEN)
        y++
    }
    val options = ultraTravelOptions()
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

private fun ultraTravelOptions(): List<Pair<String, String>> = listOf(
    "1. Go" to "travel:continue",
    "2. Inv" to "travel:supplies",
    "3. Map" to "travel:map",
    "4. Log" to "travel:journal",
    "5. Pace" to "travel:pace",
    "6. Food" to "travel:rations",
    "7. Rest" to "travel:rest",
    "8. Trade" to "travel:trade",
    "9. Hunt" to "travel:hunt"
)

internal fun Game.compactStatusLines(): List<String> {
    val next = nextLandmark()
    val short = if (next != null) mapShortName(next.id) else "Oregon"
    val toNext = if (next != null) "$short in ${(next.mile - miles).coerceAtLeast(0)} mi" else "Oregon!"
    return listOf(
        date.toString(),
        "Weather: ${weather.kind.displayName}, ${weather.tempF}F",
        "Pace: ${pace.displayName}  Rations: ${rations.displayName}",
        "Miles: $miles/${Data.TOTAL_MILES}  Next: $toNext",
        "Food: ${inventory.food}  Ammo: ${inventory.ammo}  " +
            "Cash: $${"%.0f".format(inventory.cash)}  Oxen: ${inventory.oxen}"
    )
}

internal fun Game.statusLines(): List<String> {
    val next = nextLandmark()
    val toNext = if (next != null) "${next.name} in ${(next.mile - miles).coerceAtLeast(0)} mi" else "Oregon!"
    val healthy = aliveMembers().joinToString(", ") { it.name } 
    return listOf(
        date.toString(),
        "Weather: ${weather.description}",
        "Pace: ${pace.displayName}    Rations: ${rations.displayName}",
        "Miles: $miles / ${Data.TOTAL_MILES}",
        "Next: $toNext",
        "Food: ${inventory.food} lb   Clothing: ${inventory.clothing}",
        "Ammo: ${inventory.ammo}   Cash: $${"%.0f".format(inventory.cash)}   Oxen: ${inventory.oxen}",
        if (party.any { !it.alive }) "$healthy" else "All five are alive"
    )
}

internal fun Game.sceneArt(): List<String> = when (Data.landmarkAt(landmarkIndex).kind) {
    LandmarkKind.MOUNTAINS -> Ascii.mountains
    LandmarkKind.RIVER -> Ascii.river
    LandmarkKind.FORT -> Ascii.fort
    LandmarkKind.START -> Ascii.wagonSmall
    else -> if (miles > 700) Ascii.rock else Ascii.trees
}

internal fun Game.travelOptions(): List<Pair<String, String>> = listOf(
    "1. Continue on trail" to "travel:continue",
    "2. Check supplies" to "travel:supplies",
    "3. Look at map" to "travel:map",
    "4. Look at journal" to "travel:journal",
    "5. Change pace" to "travel:pace",
    "6. Change food rations" to "travel:rations",
    "7. Stop to rest" to "travel:rest",
    "8. Attempt to trade" to "travel:trade",
    "9. Hunt for food" to "travel:hunt"
)

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
                    "3.Wait" to "dalles:wait"
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
    var y = 2
    if (rows >= 26) {
        val art = when (lm.kind) {
            LandmarkKind.FORT -> Ascii.fort
            LandmarkKind.MOUNTAINS -> Ascii.mountains
            LandmarkKind.RIVER -> Ascii.river
            else -> Ascii.rock
        }
        Ascii.draw(screen, (cols - Ascii.width(art)) / 2, y, art, Palette.GREEN)
        y += Ascii.height(art) + 1
    }
    y = screen.wrap(marginX + 1, y, contentW - 2, lm.blurb.joinToString(" "), Palette.GREEN)
    y++
    if (lm.id == "dalles") {
        screen.text(marginX + 1, y, "The last decision of the trail:", Palette.BRIGHT_YELLOW)
        y++
        val options = listOf(
            "1. Take the Barlow Road (toll $5)" to "dalles:barlow",
            "2. Raft down the Columbia River" to "dalles:raft",
            "3. Wait for better weather" to "dalles:wait"
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

internal fun Game.renderRiver(screen: Screen) {
    val lm = Data.landmarkAt(landmarkIndex)
    val river = lm.river ?: return
    if (ultraCompact) {
        screen.center(0, shortLandmarkName(lm).uppercase(), Palette.BRIGHT_GREEN, bold = true)
        val options = ArrayList<Pair<String, String>>()
        var n = 1
        options.add("${n++}.Ford" to "river:ford")
        options.add("${n++}.Caulk" to "river:caulk")
        if (river.ferryCost != null) options.add("${n++}.Ferry" to "river:ferry")
        if (river.guideCost != null) options.add("${n++}.Guide" to "river:guide")
        options.add("${n++}.Wait" to "river:wait")
        renderMenuColumns(screen, 1, options)
        return
    }
    screen.center(0, lm.name.uppercase(), Palette.BRIGHT_GREEN, bold = true)
    var y = 2
    if (rows >= 26) {
        Ascii.draw(screen, (cols - Ascii.width(Ascii.river)) / 2, y, Ascii.river, Palette.CYAN)
        y += Ascii.height(Ascii.river) + 1
    }
    y = screen.wrap(marginX + 1, y, contentW - 2, lm.blurb.joinToString(" "), Palette.GREEN)
    screen.text(marginX + 1, y, "The river is ${riverState(river)}.", Palette.CYAN); y += 2
    val options = ArrayList<Pair<String, String>>()
    var n = 1
    options.add("${n++}. Ford the river" to "river:ford")
    options.add("${n++}. Caulk and float across" to "river:caulk")
    if (river.ferryCost != null) options.add("${n++}. Take the ferry ($${"%.2f".format(river.ferryCost)})" to "river:ferry")
    if (river.guideCost != null) options.add("${n++}. Hire a guide ($${"%.2f".format(river.guideCost)})" to "river:guide")
    options.add("${n++}. Wait a day" to "river:wait")
    screen.menuAt(marginX + 1, y, options)
}

/** Lays out a short menu in one or two columns to fit tiny screens. */
internal fun Game.renderMenuColumns(screen: Screen, startY: Int, options: List<Pair<String, String>>) {
    if (options.isEmpty()) return
    val widest = options.maxOf { it.first.length }
    if (widest <= cols && options.size <= rows - startY) {
        var y = startY
        for ((label, id) in options) {
            screen.text(0, y, label, Palette.GREEN)
            screen.hotspot(id, 0, y, label.length)
            y++
        }
        return
    }
    val half = (options.size + 1) / 2
    val col2 = max(widest + 1, contentW / 2)
    options.forEachIndexed { i, (label, id) ->
        val cx = if (i < half) 0 else min(col2, cols - 1)
        val cy = startY + (i % half)
        if (cy >= rows) return@forEachIndexed
        screen.text(cx, cy, label, Palette.GREEN)
        screen.hotspot(id, cx, cy, label.length)
    }
}

internal fun Game.journalPageSize(): Int = max(if (ultraCompact) 2 else 3, (rows - 5) / 2)

internal fun Game.journalLastPage(): Int =
    if (journal.isEmpty()) 0 else (journal.size - 1) / journalPageSize()

internal fun Game.renderJournal(screen: Screen) {
    screen.center(0, "MY JOURNAL", Palette.BRIGHT_GREEN, bold = true)
    if (journal.isEmpty()) {
        screen.wrap(marginX + 1, 3, contentW - 2, "Nothing has happened yet. The trail awaits.", Palette.GREEN)
    } else {
        val size = journalPageSize()
        val page = journalPage.coerceIn(0, journalLastPage())
        val from = page * size
        val to = min(journal.size, from + size)
        var y = 2
        for (i in from until to) {
            val e = journal[i]
            screen.text(marginX + 1, y, e.date, Palette.YELLOW)
            y++
            y = screen.wrap(marginX + 3, y, contentW - 4, e.text, Palette.GREEN)
            y++
            if (y >= rows - 2) break
        }
        screen.text(
            marginX + 1, rows - 1,
            "Page ${page + 1} of ${journalLastPage() + 1}",
            Palette.DIM
        )
    }
    if (ultraCompact) {
        val prev = "[<]"
        val next = "[>]"
        val back = "[X]"
        val y = rows - 1
        screen.text(0, y, prev, Palette.BRIGHT_GREEN)
        screen.hotspot("journal:prev", 0, y, prev.length)
        screen.text(5, y, next, Palette.BRIGHT_GREEN)
        screen.hotspot("journal:next", 5, y, next.length)
        screen.text(10, y, back, Palette.BRIGHT_GREEN, bold = true)
        screen.hotspot("journal:back", 10, y, back.length)
        return
    }
    val prev = "[< Prev ]"
    val next = "[ Next >]"
    val back = "[ Back ]"
    val y = rows - 2
    screen.text(marginX + 1, y, prev, Palette.BRIGHT_GREEN)
    screen.hotspot("journal:prev", marginX + 1, y, prev.length)
    screen.text(marginX + 12, y, next, Palette.BRIGHT_GREEN)
    screen.hotspot("journal:next", marginX + 12, y, next.length)
    screen.text(marginX + 23, y, back, Palette.BRIGHT_GREEN, bold = true)
    screen.hotspot("journal:back", marginX + 23, y, back.length)
}

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

internal fun Game.renderChoice(screen: Screen) {
    screen.center(0, choiceTitle.uppercase().take(cols), Palette.BRIGHT_GREEN, bold = true)
    if (ultraCompact) {
        var y = 1
        for ((label, id) in choiceOptions) {
            if (y >= rows) break
            val short = label.take(cols)
            screen.text(0, y, short, Palette.GREEN)
            screen.hotspot(id, 0, y, short.length)
            y++
        }
        return
    }
    var y = 2
    y = screen.wrap(marginX + 1, y, contentW - 2, choiceLines.joinToString("\n"), Palette.GREEN)
    y++
    screen.menuAt(marginX + 1, y, choiceOptions)
}

internal fun Game.renderHunting(screen: Screen) {
    val field = huntField ?: return
    if (ultraCompact) {
        renderHuntingCompact(screen, field)
        return
    }
    screen.center(0, "HUNTING", Palette.BRIGHT_GREEN, bold = true)
    val hLeft = "Meat ${field.meat}/${field.carryLimit}"
    val hMid = "Ammo ${inventory.ammo}"
    val hRight = "Kills ${field.kills}"
    screen.text(marginX + 1, 1, hLeft, Palette.BRIGHT_YELLOW)
    screen.text(marginX + (contentW / 2 - hMid.length / 2).coerceAtLeast(marginX + 1 + hLeft.length), 1, hMid, Palette.WHITE)
    screen.text((marginX + contentW - hRight.length).coerceAtLeast(marginX + 1), 1, hRight, Palette.GREEN)
    val fieldX = marginX + 1
    val fieldY = 3
    screen.box(fieldX - 1, fieldY - 1, field.width + 2, field.height + 2, Palette.GREEN)
    for (fy in 0 until field.height) {
        for (fx in 0 until field.width) {
            val ch: Char
            val color: Palette
            val animal = field.animalGlyphAt(fx, fy)
            when {
                fx == field.hunterX && fy == field.hunterY -> { ch = '@'; color = Palette.BRIGHT_GREEN }
                animal != null -> { ch = animal.first; color = animal.second }
                field.bulletAt(fx, fy) -> { ch = '*'; color = Palette.BRIGHT_WHITE }
                else -> {
                    val decor = field.decorationAt(fx, fy)
                    if (decor != null) { ch = decor; color = if (decor == 'o') Palette.GRAY else Palette.GREEN }
                    else { ch = ' '; color = Palette.DEFAULT }
                }
            }
            screen.put(fieldX + fx, fieldY + fy, ch, color)
        }
    }
    // Controls
    var cy = fieldY + field.height + 1
    if (cy > rows - 4) cy = rows - 4
    val cx = marginX + 1
    val up = "  ^  "
    val left = "<    "
    val right = "    >"
    val down = "  v  "
    screen.text(cx + 2, cy, up, Palette.BRIGHT_GREEN); screen.hotspot("hunt:up", cx + 2, cy, up.length)
    screen.text(cx, cy + 1, left, Palette.BRIGHT_GREEN); screen.hotspot("hunt:left", cx, cy + 1, left.length)
    screen.text(cx + 6, cy + 1, right, Palette.BRIGHT_GREEN); screen.hotspot("hunt:right", cx + 6, cy + 1, right.length)
    screen.text(cx + 2, cy + 2, down, Palette.BRIGHT_GREEN); screen.hotspot("hunt:down", cx + 2, cy + 2, down.length)
    val shoot = "[ SHOOT ]"
    screen.text(cx + 14, cy + 1, shoot, Palette.BRIGHT_YELLOW, bold = true)
    screen.hotspot("hunt:shoot", cx + 14, cy + 1, shoot.length)
    val leave = "[ Return to trail ]"
    screen.text(cx + 14, cy + 2, leave, Palette.BRIGHT_GREEN)
    screen.hotspot("hunt:leave", cx + 14, cy + 2, leave.length)
}

/** Watch / cover-screen hunt: a small field with a one-row control strip. */
private fun Game.renderHuntingCompact(screen: Screen, field: HuntField) {
    screen.text(0, 0, "HUNT ${field.meat}/${field.carryLimit} A${inventory.ammo}".take(cols), Palette.BRIGHT_GREEN, bold = true)
    val fieldY = 1
    for (fy in 0 until field.height) {
        if (fieldY + fy >= rows - 1) break
        for (fx in 0 until min(field.width, cols)) {
            val animal = field.animalGlyphAt(fx, fy)
            val ch: Char
            val color: Palette
            when {
                fx == field.hunterX && fy == field.hunterY -> { ch = '@'; color = Palette.BRIGHT_GREEN }
                animal != null -> { ch = animal.first; color = animal.second }
                field.bulletAt(fx, fy) -> { ch = '*'; color = Palette.BRIGHT_WHITE }
                else -> {
                    val decor = field.decorationAt(fx, fy)
                    if (decor != null) { ch = decor; color = if (decor == 'o') Palette.GRAY else Palette.GREEN }
                    else { ch = ' '; color = Palette.DEFAULT }
                }
            }
            screen.put(fx, fieldY + fy, ch, color)
        }
    }
    val cy = rows - 1
    val controls = listOf(
        "<" to "hunt:left", ">" to "hunt:right", "^" to "hunt:up",
        "v" to "hunt:down", "O" to "hunt:shoot", "X" to "hunt:leave"
    )
    var x = 0
    for ((label, id) in controls) {
        if (x + 1 >= cols) break
        val color = if (id == "hunt:shoot") Palette.BRIGHT_YELLOW else Palette.BRIGHT_GREEN
        screen.text(x, cy, label, color, bold = id == "hunt:shoot")
        screen.hotspot(id, x, cy, 1)
        x += 2
    }
}

internal fun Game.renderRafting(screen: Screen) {
    val field = raftField ?: return
    if (ultraCompact) {
        screen.text(0, 0, "RAFT ${field.progress}/${field.totalProgress} ${field.integrity}/${field.maxHits}".take(cols), Palette.CYAN, bold = true)
        val fieldY = 1
        for (fy in 0 until field.height) {
            if (fieldY + fy >= rows - 1) break
            for (fx in 0 until min(field.width, cols)) {
                val ch: Char
                val color: Palette
                when {
                    field.isRaftAt(fx, fy) -> { ch = "[=]"[fx - field.raftX]; color = Palette.BRIGHT_GREEN }
                    field.rockAt(fx, fy) -> { ch = 'O'; color = Palette.GRAY }
                    else -> { ch = ' '; color = Palette.DEFAULT }
                }
                screen.put(fx, fieldY + fy, ch, color)
            }
        }
        val cy = rows - 1
        screen.text(0, cy, "<<", Palette.BRIGHT_GREEN, bold = true)
        screen.hotspot("raft:left", 0, cy, 2)
        screen.text(4, cy, ">>", Palette.BRIGHT_GREEN, bold = true)
        screen.hotspot("raft:right", 4, cy, 2)
        return
    }
    screen.center(0, "COLUMBIA RIVER", Palette.BRIGHT_GREEN, bold = true)
        val left = "Distance ${field.progress}/${field.totalProgress}"
        val right = "Raft ${field.integrity}/${field.maxHits}"
    screen.text(marginX + 1, 1, left, Palette.BRIGHT_YELLOW)
    screen.text((marginX + contentW - right.length).coerceAtLeast(marginX + 1), 1, right, Palette.WHITE)
    val fieldX = marginX + 1
    val fieldY = 3
    screen.box(fieldX - 1, fieldY - 1, field.width + 2, field.height + 2, Palette.CYAN)
    for (fy in 0 until field.height) {
        for (fx in 0 until field.width) {
            val ch: Char
            val color: Palette
            when {
                field.isRaftAt(fx, fy) -> { ch = "[=]"[fx - field.raftX]; color = Palette.BRIGHT_GREEN }
                field.rockAt(fx, fy) -> { ch = 'O'; color = Palette.GRAY }
                else -> { ch = if (fy % 2 == 0 && (fx + fy) % 7 == 0) '~' else ' '; color = Palette.DIM }
            }
            screen.put(fieldX + fx, fieldY + fy, ch, color)
        }
    }
    var cy = fieldY + field.height + 1
    if (cy > rows - 2) cy = rows - 2
    val cx = marginX + 1
    val leftBtn = "<< LEFT "
    val rightBtn = " RIGHT >>"
    screen.text(cx, cy, leftBtn, Palette.BRIGHT_GREEN, bold = true)
    screen.hotspot("raft:left", cx, cy, leftBtn.length)
    screen.text(cx + 12, cy, rightBtn, Palette.BRIGHT_GREEN, bold = true)
    screen.hotspot("raft:right", cx + 12, cy, rightBtn.length)
}

internal fun Game.renderNotice(screen: Screen) {
    if (ultraCompact) {
        screen.center(0, noticeTitle.uppercase().take(cols), Palette.BRIGHT_GREEN, bold = true)
        var y = 1
        for (line in noticeLines) {
            if (y >= rows - 1) break
            y = screen.wrap(0, y, cols, line, Palette.GREEN)
        }
        val label = "[>]"
        screen.text(0, rows - 1, label, Palette.BRIGHT_GREEN, bold = true)
        screen.hotspot("notice:continue", 0, rows - 1, label.length)
        return
    }
    screen.center(0, noticeTitle.uppercase(), Palette.BRIGHT_GREEN, bold = true)
    var y = 2
    for (line in noticeLines) {
        y = screen.wrap(marginX + 1, y, contentW - 2, line, Palette.GREEN)
    }
    val label = "[ Continue ]"
    screen.text(marginX + 1, rows - 2, label, Palette.BRIGHT_GREEN, bold = true)
    screen.hotspot("notice:continue", marginX + 1, rows - 2, label.length)
}

internal fun Game.renderDeath(screen: Screen) {
    if (ultraCompact) {
        screen.center(0, "YOU DIED", Palette.RED, bold = true)
        val leader = party.firstOrNull()?.name ?: "Traveler"
        screen.center(1, leader.take(cols), Palette.WHITE, bold = true)
        screen.center(2, "of $deathCause".take(cols), Palette.GRAY)
        renderMenuColumns(
            screen, 4,
            listOf(
                "Top Ten" to "death:topten",
                "Epitaph" to "death:epitaph",
                "Again" to "death:restart"
            )
        )
        return
    }
    screen.center(0, "YOU HAVE DIED", Palette.RED, bold = true)
    val leader = party.firstOrNull()?.name ?: "Traveler"
    val art = Ascii.grave
    var y = max(2, (rows - art.size - 8) / 2)
    Ascii.draw(screen, (cols - Ascii.width(art)) / 2, y, art, Palette.GRAY)
    y += art.size + 1
    val cause = "died of $deathCause"
    screen.center(y, "$leader $cause", Palette.WHITE, bold = true)
    screen.center(y + 1, "on ${date}", Palette.GRAY)
    y += 3
    val options = listOf(
        "See the Oregon Top Ten" to "death:topten",
        "Write an epitaph" to "death:epitaph",
        "Try again" to "death:restart"
    )
    val x = (cols - 22) / 2
    screen.menuAt(x, y, options)
    if (y + 3 < rows) {
        lastGravestone?.let {
            screen.wrap(marginX + 1, y + 3, contentW - 2, it, Palette.DIM, bg = Palette.BLACK)
        }
    }
}

internal fun Game.renderArrived(screen: Screen) {
    screen.center(0, "OREGON!", Palette.BRIGHT_GREEN, bold = true)
    if (ultraCompact) {
        screen.center(1, "Score $lastScore".take(cols), Palette.BRIGHT_YELLOW, bold = true)
        renderMenuColumns(
            screen, 3,
            listOf("Top Ten" to "arrived:topten", "Again" to "arrived:restart")
        )
        return
    }
    val art = Ascii.blockWord("WELCOME")
    var y = 2
    Ascii.draw(screen, (cols - Ascii.width(art)) / 2, y, art, Palette.BRIGHT_YELLOW)
    y += art.size + 2
    y = screen.wrap(marginX + 1, y, contentW - 2,
        "You have reached the end of the Oregon Trail and the fertile Willamette Valley.",
        Palette.GREEN)
    y++
    screen.text(marginX + 1, y, "Final score: $lastScore points", Palette.BRIGHT_YELLOW, bold = true)
    y++
    if (rows >= 28) {
        screen.text(marginX + 1, y, "How your score was earned:", Palette.DIM)
        y++
        for ((label, value) in scoreParts()) {
            screen.text(marginX + 1, y, label.take(contentW - 8), Palette.GRAY)
            screen.text(marginX + contentW - 6, y, value.toString().padStart(6), Palette.WHITE)
            y++
            if (y >= rows - 5) break
        }
    }
    y++
    val options = listOf(
        "See the Oregon Top Ten" to "arrived:topten",
        "Travel the trail again" to "arrived:restart"
    )
    screen.menuAt(marginX + 1, y, options)
}
