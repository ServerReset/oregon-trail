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

    const val TREE_A: Char = '^'
    const val TREE_B: Char = 'f'
}
