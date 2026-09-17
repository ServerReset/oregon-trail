package com.oregontrail.engine


internal fun ultraTravelOptions(): List<Pair<String, String>> = listOf(
    "1. Go" to "travel:continue",
    "2. Inv" to "travel:supplies",
    "3. Map" to "travel:map",
    "4. Log" to "travel:journal",
    "5. Pace" to "travel:pace",
    "6. Food" to "travel:rations",
    "7. Rest" to "travel:rest",
    "8. Trade" to "travel:trade",
    "9. Hunt" to "travel:hunt",
    "10.Save" to "travel:save"
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
            "Cash: $${"%.0f".format(inventory.cash)}  Oxen: ${inventory.oxen} ${oxCondition()}"
    )
}

internal fun Game.statusLines(): List<String> {
    val next = nextLandmark()
    val toNext = if (next != null) "${next.name} in ${(next.mile - miles).coerceAtLeast(0)} mi" else "Oregon!"
    val healthy = aliveMembers().joinToString(", ") { it.name }
    return listOf(
        date.toString(),
        "Weather: ${weather.kind.displayName}, ${weather.tempF}F",
        "Pace:    ${pace.displayName}    Rations: ${rations.displayName}",
        "Miles:   $miles / ${Data.TOTAL_MILES}",
        "Next:    $toNext",
        "Food:    ${inventory.food} lb   Clothing: ${inventory.clothing}",
        "Ammo:    ${inventory.ammo}   Cash: $${"%.0f".format(inventory.cash)}   Oxen: ${inventory.oxen} (${oxCondition()})",
        if (party.any { !it.alive }) healthy else "All five are alive"
    )
}

/**
 * Draws a status line, painting the leading "Label:" tokens in gray so the
 * values stand out in [value]. Alignment spaces are preserved.
 */
internal fun Screen.paintStatusLine(x: Int, y: Int, line: String, value: Palette) {
    var cx = x
    line.split(' ').forEachIndexed { i, token ->
        val fg = if (token.endsWith(":")) Palette.GRAY else value
        val chunk = if (i == 0) token else " $token"
        text(cx, y, chunk, fg)
        cx += chunk.length
    }
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
    "9. Hunt for food" to "travel:hunt",
    "10. Save game" to "travel:save"
)
