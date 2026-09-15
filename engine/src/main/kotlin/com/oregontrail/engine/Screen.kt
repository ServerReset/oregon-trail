package com.oregontrail.engine

/**
 * A rendered, fixed-size character screen. This is the entire contract between
 * the engine and any front-end: a grid of styled characters plus tappable regions.
 */
class Screen(val width: Int, val height: Int) {

    private val cells: Array<Array<Cell>> =
        Array(height) { Array(width) { Cell() } }

    val hotspots: MutableList<Hotspot> = ArrayList()

    /**
     * Background tint for the screen. The front-end blends this into the
     * background so weather and region change the mood of the display.
     */
    var ambient: Palette = Palette.BLACK

    fun cell(x: Int, y: Int): Cell? =
        if (x in 0 until width && y in 0 until height) cells[y][x] else null

    fun clear() {
        for (row in cells) for (c in row) c.clear()
        hotspots.clear()
        ambient = Palette.BLACK
    }

    /** Fills the entire screen with the given background. */
    fun fillBackground(bg: Palette) {
        for (row in cells) for (c in row) c.bg = bg
    }

    fun put(x: Int, y: Int, ch: Char, fg: Palette = Palette.DEFAULT,
            bg: Palette = Palette.DEFAULT, bold: Boolean = false) {
        val c = cell(x, y) ?: return
        c.ch = ch
        c.fg = fg
        c.bg = bg
        c.bold = bold
    }

    /** Writes a string starting at (x,y), clipping at the screen edges. */
    fun text(x: Int, y: Int, s: String, fg: Palette = Palette.DEFAULT,
             bg: Palette = Palette.DEFAULT, bold: Boolean = false) {
        var cx = x
        for (ch in s) {
            if (cx >= width) break
            if (cx >= 0) put(cx, y, ch, fg, bg, bold)
            cx++
        }
    }

    /** Draws text horizontally centered on [row]. */
    fun center(row: Int, s: String, fg: Palette = Palette.DEFAULT,
               bg: Palette = Palette.DEFAULT, bold: Boolean = false) {
        text(((width - s.length) / 2).coerceAtLeast(0), row, s, fg, bg, bold)
    }

    fun hline(x: Int, y: Int, len: Int, ch: Char = '-', fg: Palette = Palette.DEFAULT) {
        for (i in 0 until len) put(x + i, y, ch, fg)
    }

    /** Clears one row (used by the reveal transition). */
    fun blankRow(y: Int) {
        if (y < 0 || y >= height) return
        for (x in 0 until width) cells[y][x].clear()
    }

    /** Draws a character only where the cell is currently blank. */
    fun putIfBlank(x: Int, y: Int, ch: Char, fg: Palette = Palette.DEFAULT) {
        val c = cell(x, y) ?: return
        if (c.ch == ' ') put(x, y, ch, fg)
    }

    fun vline(x: Int, y: Int, len: Int, ch: Char = '|', fg: Palette = Palette.DEFAULT) {
        for (i in 0 until len) put(x, y + i, ch, fg)
    }


    /** Every row as a list of strings, preserving blank rows and row order. */
    fun toLines(): List<String> {
        val lines = ArrayList<String>(height)
        for (y in 0 until height) {
            val line = StringBuilder()
            for (x in 0 until width) line.append(cells[y][x].ch)
            lines.add(line.toString().trimEnd())
        }
        return lines
    }

    /** All non-blank text rows joined by newlines. Handy for logging and tests. */
    fun toText(): String = toLines().joinToString("\n").trimEnd('\n')
}
