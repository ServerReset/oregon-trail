package com.oregontrail.engine

/** Small illustrations for random events. */
internal object AsciiEvents {
    val wheel: List<String> = listOf(
        "  ___",
        " /   \\",
        "|  +  |",
        " \\___/"
    )
    val bandit: List<String> = listOf(
        "  ____",
        " /    \\",
        " | oo |",
        "  \\__/",
        "  /||\\",
        "  /  \\"
    )
    val snake: List<String> = listOf(
        "   __",
        "  /  \\____",
        "  \\_/\\    \\",
        "      \\____>"
    )
    val wolf: List<String> = listOf(
        " /\\_/\\",
        "( o o )",
        " \\_v_/",
        "  |||"
    )
    val teepee: List<String> = listOf(
        "   /\\",
        "  /  \\",
        " /    \\",
        "/______\\"
    )
    val bush: List<String> = listOf(
        "  @ @  @",
        " (*****)",
        "   \\|/"
    )
    val fireArt: List<String> = listOf(
        "   ( )",
        " ( ) ( )",
        "  (   )",
        "   \\|/"
    )
    val horses: List<String> = listOf(
        "  /\\   /\\",
        " (  )-(  )",
        "  \\/   \\/"
    )
    val brokenWagon: List<String> = listOf(
        "   ___",
        "  /   \\",
        " |  X  |",
        "  \\___/",
        "  O   O"
    )
    val prairieDog: List<String> = listOf(
        "  (o.o)",
        "  /|_|\\",
        "   / \\"
    )
    val rainbow: List<String> = listOf(
        "    ,---.",
        "  ,'     '.",
        " /  ,---.  \\",
        "' -'     '- '"
    )
    val spring: List<String> = listOf(
        "  ~ ~ ~ ~",
        " ~ ~ ~ ~ ~",
        "  ~ ~ ~ ~"
    )

    // ------------------------------------------------------------------
    // Hunting field props (roguelike style)
    // ------------------------------------------------------------------

    val buffalo: List<String> = listOf(
        "   __       __",
        "  (  )-----/  )",
        "   \\/  \\/  \\/"
    )

    val fiddle: List<String> = listOf(
        "   ,---.",
        "  |  o  |====",
        "   '---'"
    )
}
