package com.oregontrail.engine


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
    pauseButton(screen)
    var y = 2
    if (rows >= 26) {
        val art = riverArt(frame)
        val artTop = y
        Ascii.draw(screen, (cols - Ascii.width(art)) / 2, y, art, Palette.CYAN)
        val artBottom = y + Ascii.height(art)
        y += Ascii.height(art) + 1
        overlaySky(screen, artTop, artBottom)
        overlayWeather(screen, artTop, artBottom)
        overlayWater(screen, artTop, artBottom)
    }
    y = screen.wrap(marginX + 1, y, contentW - 2, lm.blurb.joinToString(" "), Palette.GREEN)
    screen.text(marginX + 1, y, "The river is ${riverState(river)}.", Palette.CYAN)
    screen.text(marginX + 1, y + 1, "Depth: about ${"%.1f".format(riverDepth(river))} feet.", Palette.CYAN)
    y += 3
    val options = ArrayList<Pair<String, String>>()
    var n = 1
    options.add("${n++}. Ford the river" to "river:ford")
    options.add("${n++}. Caulk and float across" to "river:caulk")
    river.ferryCost?.let { cost ->
        val note = if (inventory.cash < cost) " - can't afford" else ""
        options.add("${n++}. Take the ferry ($${"%.2f".format(cost)})$note" to "river:ferry")
    }
    river.guideCost?.let { cost ->
        val note = if (inventory.cash < cost) " - can't afford" else ""
        options.add("${n++}. Hire a guide ($${"%.2f".format(cost)})$note" to "river:guide")
    }
    options.add("${n++}. Wait a day" to "river:wait")
    screen.menuAt(marginX + 1, y, options)
}
