package com.oregontrail.engine

/**
 * A shareable "postcard" for the current journey: a tiny piece of ASCII art and
 * a few lines that capture where the party is, how far they have come and how
 * it is going. Works at any point, and especially when the trail ends.
 */
fun Game.postcard(): List<String> {
    val width = 42
    val inner = width - 4
    val art = AsciiScenery.wagonSmall
    val leader = party.firstOrNull()?.name ?: "Traveler"
    val landmark = Data.landmarkAt(landmarkIndex).name
    val outcome = when (phase) {
        Phase.DEATH -> "Died of $deathCause near $landmark."
        Phase.ARRIVED, Phase.EPILOGUE -> "Arrived in the Willamette Valley."
        else -> "On the trail near $landmark."
    }
    val score = when (phase) {
        Phase.DEATH -> "Score: $lastScore points"
        Phase.ARRIVED, Phase.EPILOGUE -> "Final score: $lastScore points"
        else -> "Miles: $miles of ${Data.TOTAL_MILES}"
    }
    val alive = party.count { it.alive }
    val quote = TrailQuotes.byLandmark["willamette"] ?: "Westward, ever westward."

    val out = ArrayList<String>()
    fun line(s: String) = out.add("| " + s.padEnd(inner).take(inner) + " |")
    out.add("+" + "-".repeat(width - 2) + "+")
    line(center("THE OREGON TRAIL", inner))
    line("")
    art.forEach { line(center(it, inner)) }
    line("")
    line("$leader and party  -  ${occupation.displayName}")
    line("Departed ${travelMonth.displayName} 1848  -  ${date.monthName} ${date.day}")
    line("$outcome")
    line(score + "  -  $alive of 5 alive")
    line("")
    for (q in wrapString("\"$quote\"", inner)) line(q)
    out.add("+" + "-".repeat(width - 2) + "+")
    return out
}

private fun center(s: String, width: Int): String {
    if (s.length >= width) return s.take(width)
    val left = (width - s.length) / 2
    return " ".repeat(left) + s
}

/** The postcard as a single string (for text sharing or tests). */
fun Game.postcardText(): String = postcard().joinToString("\n")
