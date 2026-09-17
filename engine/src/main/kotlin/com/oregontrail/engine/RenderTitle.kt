package com.oregontrail.engine


internal fun Game.renderTitle(screen: Screen) {
    if (ultraCompact) {
        renderTitleCompact(screen)
        return
    }
    // Birds drift across the sky even on the title screen.
    if (contentW >= 20) {
        val bx = ((frame * 2) % (cols + 6)) - 3
        val bird = if (frame % 2 == 0) 'v' else '^'
        screen.putIfBlank(bx, 0, bird, Palette.DIM)
        screen.putIfBlank(bx + 2, 0, bird, Palette.DIM)
    }
    // Layered artwork: twinkling stars, the wagon, then the title letters,
    // each layer with its own colour.
    data class Layer(val rows: List<String>, val fg: Palette, val bold: Boolean = false)
    val layers = ArrayList<Layer>()
    if (contentW >= 36 && rows >= 30) {
        layers.add(Layer(starsArt(frame), Palette.DIM))
        layers.add(Layer(AsciiScenery.wagon, Palette.GREEN))
        layers.add(Layer(Ascii.blockWord("OREGON"), Palette.BRIGHT_GREEN, true))
        layers.add(Layer(Ascii.blockWord("TRAIL"), Palette.BRIGHT_YELLOW, true))
        layers.add(Layer(listOf(""), Palette.DEFAULT))
    } else {
        layers.add(Layer(AsciiScenery.wagonSmall, Palette.GREEN))
        layers.add(Layer(listOf(""), Palette.DEFAULT))
        layers.add(Layer(listOf("T H E   O R E G O N   T R A I L"), Palette.BRIGHT_GREEN, true))
    }
    val menu = ArrayList<Pair<String, String>>()
    var n = 1
    menu.add("${n++}. Travel the trail" to "title:travel")
    if (dailySeed != 0L) menu.add("${n++}. Trail of the Day" to "title:daily")
    if (autosaveAvailable) menu.add("${n++}. Continue saved journey" to "title:continue")
    if (saveSlots.isNotEmpty()) menu.add("${n++}. Load a saved game" to "title:load")
    menu.add("${n++}. Learn about the trail" to "title:about")
    menu.add("${n++}. See the Oregon Top Ten" to "title:topten")
    menu.add("${n++}. Achievements (${achievements.size}/${Achievements.all.size})" to "title:ach")
    menu.add("${n++}. Statistics" to "title:stats")
    menu.add("${n++}. Settings and options" to "title:manage")
    menu.add("${n++}. End" to "title:end")
    val artH = layers.sumOf { it.rows.size }
    val totalH = artH + 3 + menu.size
    var y = ((rows - totalH) / 2).coerceAtLeast(0)
    for (layer in layers) {
        val ax = ((cols - (layer.rows.maxOfOrNull { it.length } ?: 0)) / 2).coerceAtLeast(0)
        Ascii.draw(screen, ax, y, layer.rows, layer.fg, layer.bold)
        y += layer.rows.size
    }
    // A dim rule beneath the artwork separates it from the menu.
    val ruleW = contentW.coerceAtMost(40).coerceAtLeast(8)
    screen.hline(((cols - ruleW) / 2).coerceAtLeast(0), y, ruleW, '-', Palette.DIM)
    y += 2
    val x = ((cols - (menu.maxOf { it.first.length } + 2)) / 2).coerceAtLeast(0)
    for ((label, id) in menu) {
        screen.text(x, y, ">", Palette.BRIGHT_YELLOW, bold = true)
        screen.text(x + 2, y, label, Palette.GREEN)
        screen.hotspot(id, (x - 1).coerceAtLeast(0), y, label.length + 4)
        y++
    }
    screen.footer(rows, "Oregon Trail v${Game.VERSION}")
}

/** Watch / cover-screen title: no artwork, short labels, everything on screen. */
internal fun Game.renderTitleCompact(screen: Screen) {
    val menu = ArrayList<Pair<String, String>>()
    var n = 1
    menu.add("${n++}. Travel" to "title:travel")
    if (dailySeed != 0L) menu.add("${n++}. Daily" to "title:daily")
    if (autosaveAvailable) menu.add("${n++}. Continue" to "title:continue")
    if (saveSlots.isNotEmpty()) menu.add("${n++}. Load" to "title:load")
    menu.add("${n++}. About" to "title:about")
    menu.add("${n++}. Top Ten" to "title:topten")
    menu.add("${n++}. Awards" to "title:ach")
    menu.add("${n++}. Stats" to "title:stats")
    menu.add("${n++}. Settings" to "title:manage")
    menu.add("${n++}. End" to "title:end")
    screen.center(0, "OREGON", Palette.BRIGHT_YELLOW, bold = true)
    screen.center(1, "TRAIL", Palette.BRIGHT_YELLOW, bold = true)
    renderMenuColumns(screen, 3, menu)
}
