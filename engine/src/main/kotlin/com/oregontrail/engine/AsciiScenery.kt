package com.oregontrail.engine

import kotlin.math.max

/** Covered wagons and the places they roll past. */
internal object AsciiScenery {
    val wagon: List<String> = listOf(
        "            _.---._",
        "          .'  ___  '.",
        "         /   /   \\   \\",
        "        |   |     |   |",
        "        |   |     |   |",
        "         \\   \\___/   /",
        "          '._______.'",
        "    _______|_______|_______",
        "   |_______________________|",
        "        (O)       (O)"
    )
    val wagonSmall: List<String> = listOf(
        "       .-.\"\"\"-.",
        "      /  ___  \\",
        "     |  /   \\  |",
        "     | |     | |",
        "      \\ \\___/ /",
        "   ____'.___.'____",
        "  |_______________|",
        "      (@)   (@)"
    )
    val store: List<String> = listOf(
        "   _______________________",
        "  /  MATT'S GENERAL STORE /",
        " /_______________________/",
        " |  [ ] [ ] [ ] [ ] [ ]  |",
        " |  [ ] [ ] [ ] [ ] [ ]  |",
        " |_______________________|"
    )
    val fort: List<String> = listOf(
        "    _|_   _|_   _|_",
        "   |   | |   | |   |",
        " __|___|_|___|_|___|__",
        "|  ________________  |",
        "| |  FORT  TRADING | |",
        "|_|________________|_|"
    )
    val mountains: List<String> = listOf(
        "           /\\",
        "          /  \\  /\\",
        "     /\\  /    \\/  \\",
        "    /  \\/          \\",
        "   /                \\",
        "  /                  \\"
    )
    val river: List<String> = listOf(
        "  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~",
        "  ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~",
        "  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~",
        "  ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~",
        "  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
    )
    val rock: List<String> = listOf(
        "          _____",
        "        /       \\",
        "       |   ___   |",
        "        \\       /",
        "      /         \\",
        "     |___________|"
    )
    val trees: List<String> = listOf(
        "     /\\",
        "    /  \\",
        "   /    \\",
        "    /  \\",
        "   /    \\",
        "  /______\\",
        "     ||"
    )
    val grave: List<String> = listOf(
        "     __________",
        "    /          \\",
        "   |    R.I.P   |",
        "   |            |",
        "   |            |",
        "   |            |",
        "   |            |",
        "  /|____________|\\"
    )

    // ------------------------------------------------------------------
    // Landscape set
    // ------------------------------------------------------------------

    /** A night camp with the wagon and a fire; used by the pause menu. */
}
