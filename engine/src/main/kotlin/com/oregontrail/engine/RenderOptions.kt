package com.oregontrail.engine


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
        short.add("Test sound" to "manage:testsound")
        uiSettings?.let { ui -> short.add("Hap ${onOff(ui.haptics)}" to "manage:haptics") }
        uiSettings?.let { ui ->
            short.add("Text ${textScaleName(ui.textScaleIndex).take(1)}" to "manage:textsize")
            short.add("Theme ${themeName(ui.themeIndex).take(6)}" to "manage:theme")
            short.add("Contr ${onOff(ui.highContrast)}" to "manage:contrast")
            short.add("Scan ${onOff(ui.scanlines)}" to "manage:scanlines")
        }
        short.add("Export" to "manage:export")
        short.add("Import" to "manage:import")
        short.add("Back" to "manage:back")
        renderMenuColumns(screen, 1, short)
        return
    }
    screen.center(1, "SETTINGS", Palette.BRIGHT_GREEN, bold = true)
    val options = ArrayList<Pair<String, String>>()
    options.add("See the Oregon Top Ten" to "manage:topten")
    options.add("Choose a different leader" to "manage:newleader")
    options.add("Difficulty: ${difficulty.displayName}" to "manage:difficulty")
    options.add("Sound is ${if (soundEnabled) "ON" else "OFF"}" to "manage:sound")
    options.add("Play a test sound" to "manage:testsound")
    uiSettings?.let { ui ->
        options.add("Haptics: ${onOff(ui.haptics)}" to "manage:haptics")
    }
    uiSettings?.let { ui ->
        options.add("Text size: ${textScaleName(ui.textScaleIndex)}" to "manage:textsize")
        options.add("Theme: ${themeName(ui.themeIndex)}" to "manage:theme")
        options.add("High contrast: ${onOff(ui.highContrast)}" to "manage:contrast")
        options.add("Scanlines: ${onOff(ui.scanlines)}" to "manage:scanlines")
    }
    options.add("Export saves to a file" to "manage:export")
    options.add("Import saves from a file" to "manage:import")
    options.add("Return to the title screen" to "manage:back")
    val step = if (rows < 26) 1 else 2
    var y = 3
    for ((label, id) in options) {
        if (y >= rows - 1) break
        screen.text(marginX + 1, y, ">", Palette.BRIGHT_YELLOW, bold = true)
        screen.text(marginX + 3, y, label, Palette.GREEN)
        screen.hotspot(id, marginX + 1, y, label.length + 4)
        y += step
    }
}

internal fun Game.textScaleName(index: Int): String = when (index) {
    0 -> "Small"
    1 -> "Medium"
    else -> "Large"
}

internal fun Game.themeName(index: Int): String = when (index) {
    2 -> "Material You"
    1 -> "Classic"
    else -> "Terminal"
}

internal fun Game.onOff(value: Boolean): String = if (value) "ON" else "OFF"
