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
    val art = ArrayList<String>()
    if (contentW >= 36 && rows >= 30) {
        art.addAll(starsArt(frame))
        art.addAll(AsciiScenery.wagon)
        art.addAll(Ascii.blockWord("OREGON"))
        art.addAll(Ascii.blockWord("TRAIL"))
    } else {
        art.addAll(AsciiScenery.wagonSmall)
        art.add("")
        art.add("T H E   O R E G O N   T R A I L")
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
    menu.add("${n++}. Choose Management Options" to "title:manage")
    menu.add("${n++}. End" to "title:end")
    val totalH = art.size + 2 + menu.size
    var y = ((rows - totalH) / 2).coerceAtLeast(0)
    Ascii.draw(screen, (cols - Ascii.width(art)) / 2, y, art, Palette.BRIGHT_GREEN)
    y += art.size + 1
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
    menu.add("${n++}. Options" to "title:manage")
    menu.add("${n++}. End" to "title:end")
    screen.center(0, "OREGON", Palette.BRIGHT_YELLOW, bold = true)
    screen.center(1, "TRAIL", Palette.BRIGHT_YELLOW, bold = true)
    renderMenuColumns(screen, 3, menu)
}
