package com.oregontrail.engine

/**
 * All static ASCII artwork lives here. Everything is plain ASCII (no Unicode
 * box-drawing or emoji) to match the teleprinter era, and every piece is kept
 * at or below 38 columns so it fits even the narrowest phone in portrait.
 */
object Ascii {

    /** Minimal 5x5 block font for banner lettering. */
    private val font: Map<Char, List<String>> = mapOf(
        'A' to listOf(" ### ", "#   #", "#####", "#   #", "#   #"),
        'B' to listOf("#### ", "#   #", "#### ", "#   #", "#### "),
        'C' to listOf(" ### ", "#   #", "#    ", "#   #", " ### "),
        'D' to listOf("#### ", "#   #", "#   #", "#   #", "#### "),
        'E' to listOf("#####", "#    ", "###  ", "#    ", "#####"),
        'F' to listOf("#####", "#    ", "###  ", "#    ", "#    "),
        'G' to listOf(" ### ", "#   #", "# ###", "#   #", " ### "),
        'H' to listOf("#   #", "#   #", "#####", "#   #", "#   #"),
        'I' to listOf("#####", "  #  ", "  #  ", "  #  ", "#####"),
        'J' to listOf("#####", "    #", "    #", "#   #", " ### "),
        'K' to listOf("#   #", "#  # ", "###  ", "#  # ", "#   #"),
        'L' to listOf("#    ", "#    ", "#    ", "#    ", "#####"),
        'M' to listOf("#   #", "## ##", "# # #", "#   #", "#   #"),
        'N' to listOf("#   #", "##  #", "# # #", "#  ##", "#   #"),
        'O' to listOf(" ### ", "#   #", "#   #", "#   #", " ### "),
        'P' to listOf("#### ", "#   #", "#### ", "#    ", "#    "),
        'Q' to listOf(" ### ", "#   #", "# # #", "#  # ", " ## #"),
        'R' to listOf("#### ", "#   #", "#### ", "#  # ", "#   #"),
        'S' to listOf(" ####", "#    ", " ### ", "    #", "#### "),
        'T' to listOf("#####", "  #  ", "  #  ", "  #  ", "  #  "),
        'U' to listOf("#   #", "#   #", "#   #", "#   #", " ### "),
        'V' to listOf("#   #", "#   #", "#   #", " # # ", "  #  "),
        'W' to listOf("#   #", "#   #", "# # #", "## ##", "#   #"),
        'X' to listOf("#   #", " # # ", "  #  ", " # # ", "#   #"),
        'Y' to listOf("#   #", " # # ", "  #  ", "  #  ", "  #  "),
        'Z' to listOf("#####", "   # ", "  #  ", " #   ", "#####"),
        ' ' to listOf("     ", "     ", "     ", "     ", "     "),
        '!' to listOf("  #  ", "  #  ", "  #  ", "     ", "  #  "),
        '.' to listOf("     ", "     ", "     ", "     ", "  #  "),
        '\'' to listOf("  #  ", "  #  ", "     ", "     ", "     ")
    )

    /** Renders a word using the block font. Returns 5 lines. */
    fun blockWord(word: String): List<String> {
        val lines = MutableList(5) { StringBuilder() }
        word.uppercase().forEachIndexed { index, ch ->
            val glyph = font[ch] ?: font[' ']!!
            for (r in 0 until 5) {
                if (index > 0) lines[r].append(' ')
                lines[r].append(glyph[r])
            }
        }
        return lines.map { it.toString() }
    }

    /** Draws a list of lines as art starting at (x,y). Returns the row after the art. */
    fun draw(screen: Screen, x: Int, y: Int, art: List<String>,
             fg: Palette = Palette.DEFAULT, bold: Boolean = false): Int {
        art.forEachIndexed { i, line -> screen.text(x, y + i, line, fg, bold = bold) }
        return y + art.size
    }

    fun width(art: List<String>): Int = art.maxOfOrNull { it.length } ?: 0

    fun height(art: List<String>): Int = art.size

    // ------------------------------------------------------------------
    // Scenery
    // ------------------------------------------------------------------

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
    val camp: List<String> = listOf(
        "   *    .        *     .",
        "        .    *        .",
        "           ___",
        "  *      .'   '.   *",
        "        /  ___  \\",
        "       |  /   \\  |",
        "        \\ \\___/ /",
        "     ____'.___.'____",
        "    |_______________|",
        "   .    (@)   (@)    .",
        "       \\  |  /",
        "        \\ | /",
        "       .--*--.",
        "        \\___/"
    )

    /** The second campfire frame, for a flickering flame. */
    val campFlicker: List<String> = listOf(
        "   .    *        .     *",
        "        *    .        *",
        "           ___",
        "  .      .'   '.   .",
        "        /  ___  \\",
        "       |  /   \\  |",
        "        \\ \\___/ /",
        "     ____'.___.'____",
        "    |_______________|",
        "   *    (@)   (@)    *",
        "        \\ /|\\ /",
        "         \\ | /",
        "        .-*.*-.",
        "         \\_/_/"
    )

    /** Chimney Rock: a tall clay spire. */
    val chimneyRock: List<String> = listOf(
        "          ___",
        "         / _ \\",
        "        | | | |",
        "        | |_| |",
        "       /   |   \\",
        "      /    |    \\",
        "     /     |     \\",
        "    /______|______\\",
        "   /_______________\\"
    )

    /** South Pass: a broad gap in the Rockies. */
    val southPass: List<String> = listOf(
        "      /\\              /\\",
        "     /  \\    ____    /  \\",
        "    /    \\  /    \\  /    \\",
        "   /      \\/      \\/      \\",
        "  /________________________\\"
    )

    /** Independence, Missouri: a town of cabins. */
    val town: List<String> = listOf(
        "    _____      _____",
        "   |  _  |    |  _  |",
        "   | |_| |    | |_| |",
        "   |  |  |____|  |  |",
        "   |__|__|____|__|__|",
        "     |  |      |  |"
    )

    /** The Dalles: cliffs and a river gorge. */
    val dalles: List<String> = listOf(
        "   /|                  |\\",
        "  / |     ~~~~~~~~     | \\",
        " /  |   ~~~~~~~~~~~~   |  \\",
        "/___|__________________|___\\"
    )

    /** The Willamette Valley: pines, a cabin and a river. */
    val valley: List<String> = listOf(
        "     /\\        /\\        /\\",
        "    /  \\      /  \\      /  \\",
        "   /    \\    /    \\    /    \\",
        "  /______\\  /______\\  /______\\",
        "         ___________",
        "        |  [] [] [] |",
        "        |___________|",
        "   ~~~~~~~~~~~~~~~~~~~~~~~~"
    )

    /** A few twinkling stars for title/sky decoration. */
    val stars: List<String> = listOf(
        "  *      .        *      .     *",
        "      .       *        ."
    )

    // ------------------------------------------------------------------
    // Sky props (animated)
    // ------------------------------------------------------------------

    val sun: List<String> = listOf(
        "\\ | /",
        "-(o)-",
        "/ | \\"
    )

    /** One frame of a small cloud. */
    val cloud: List<String> = listOf("( .. )")

    // ------------------------------------------------------------------
    // Event illustrations
    // ------------------------------------------------------------------

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

    val snowflake: List<String> = listOf(
        "  *",
        " */\\*",
        "  *"
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

    const val TREE_A: Char = '^'
    const val TREE_B: Char = 'f'
}
