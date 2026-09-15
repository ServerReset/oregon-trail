package com.oregontrail.engine


internal fun Game.renderStats(screen: Screen) {
    if (ultraCompact) {
        screen.center(0, "STATS", Palette.BRIGHT_GREEN, bold = true)
        val lines = listOf(
            "played ${stats.gamesPlayed}",
            "made it ${stats.arrivals}",
            "died ${stats.deaths}",
            "best ${stats.bestScore}",
            "miles ${stats.totalMiles}",
            "awards ${achievements.size}/${Achievements.all.size}"
        )
        lines.forEachIndexed { i, l ->
            if (1 + i >= rows - 1) return@forEachIndexed
            screen.text(0, 1 + i, l.take(cols), Palette.GREEN)
        }
        val back = "[X]"
        screen.text(0, rows - 1, back, Palette.BRIGHT_GREEN, bold = true)
        screen.hotspot("stats:back", 0, rows - 1, back.length)
        return
    }
    screen.center(0, "STATISTICS", Palette.BRIGHT_GREEN, bold = true)
    val lines = ArrayList<String>()
    lines.add("Games played: ${stats.gamesPlayed}")
    lines.add("Reached Oregon: ${stats.arrivals}")
    lines.add("Died on the trail: ${stats.deaths}")
    lines.add("Best score: ${stats.bestScore}")
    lines.add("Total miles travelled: ${stats.totalMiles}")
    lines.add("Achievements: ${achievements.size}/${Achievements.all.size}")
    lines.add("")
    lines.add("Oregon Top Ten:")
    topTen.take(5).forEachIndexed { i, e -> lines.add("${i + 1}. ${e.name}  ${e.points}") }
    var y = 3
    for (line in lines) y = screen.wrap(marginX + 1, y, contentW - 2, line, Palette.GREEN)
    val back = "[ Back ]"
    screen.text(marginX + 1, rows - 2, back, Palette.BRIGHT_GREEN, bold = true)
    screen.hotspot("stats:back", marginX + 1, rows - 2, back.length)
}
