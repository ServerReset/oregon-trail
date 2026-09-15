package com.oregontrail.engine


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
        screen.text(marginX + 1, y, ">", Palette.BRIGHT_YELLOW, bold = true)
        screen.text(marginX + 3, y, "${i + 1}. ${occ.displayName} - $${occ.startingMoney}", Palette.BRIGHT_YELLOW, bold = true)
        screen.hotspot("prof:$i", marginX + 1, y, occ.displayName.length + 12)
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
        screen.text(marginX + 1, y, ">", Palette.BRIGHT_YELLOW, bold = true)
        screen.text(marginX + 3, y, "${i + 1}. ${m.displayName}", Palette.GREEN)
        screen.hotspot("month:$i", marginX + 1, y, 16)
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
        screen.text(marginX + 1, y, ">", Palette.BRIGHT_YELLOW, bold = true)
        screen.text(marginX + 3, y, label, Palette.GREEN)
        screen.hotspot("name:$i", marginX + 1, y, label.length + 2)
        y += if (compact) 1 else 2
    }
    val go = "[ Begin the journey ]"
    screen.text(marginX + 1, rows - 2, go, Palette.BRIGHT_GREEN, bold = true)
    screen.hotspot("names:go", marginX + 1, rows - 2, go.length)
}
