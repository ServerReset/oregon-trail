package com.oregontrail.engine

/**
 * Logical colors used by the engine. The Android layer maps these to actual ARGB
 * values, which keeps the engine free of any Android dependency.
 */
enum class Palette {
    DEFAULT,
    BLACK,
    DIM,
    RED,
    GREEN,
    YELLOW,
    BLUE,
    MAGENTA,
    CYAN,
    WHITE,
    GRAY,
    BRIGHT_GREEN,
    BRIGHT_YELLOW,
    BRIGHT_WHITE,
    BROWN
}

/** A single character cell on the terminal. */
class Cell(
    var ch: Char = ' ',
    var fg: Palette = Palette.DEFAULT,
    var bg: Palette = Palette.DEFAULT,
    var bold: Boolean = false
) {
    fun set(other: Cell) {
        ch = other.ch
        fg = other.fg
        bg = other.bg
        bold = other.bold
    }

    fun clear() {
        ch = ' '
        fg = Palette.DEFAULT
        bg = Palette.DEFAULT
        bold = false
    }
}

/** A rectangular, tappable region on the screen. Coordinates are inclusive. */
data class Hotspot(
    val id: String,
    val x0: Int,
    val y0: Int,
    val x1: Int,
    val y1: Int
) {
    fun contains(x: Int, y: Int): Boolean = x in x0..x1 && y in y0..y1
}

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

    /** Draws a single-line box with an optional title. */
    fun box(x: Int, y: Int, w: Int, h: Int, fg: Palette = Palette.DEFAULT,
            title: String? = null) {
        if (w < 2 || h < 2) return
        put(x, y, '+', fg); put(x + w - 1, y, '+', fg)
        put(x, y + h - 1, '+', fg); put(x + w - 1, y + h - 1, '+', fg)
        hline(x + 1, y, w - 2, '-', fg)
        hline(x + 1, y + h - 1, w - 2, '-', fg)
        vline(x, y + 1, h - 2, '|', fg)
        vline(x + w - 1, y + 1, h - 2, '|', fg)
        if (title != null) text(x + 2, y, " $title ", fg = Palette.BRIGHT_GREEN, bold = true)
    }

    /**
     * Word-wraps [s] into [maxWidth] and writes it starting at (x,y).
     * Returns the row after the last written line.
     */
    fun wrap(x: Int, y: Int, maxWidth: Int, s: String, fg: Palette = Palette.DEFAULT,
             bold: Boolean = false, bg: Palette = Palette.DEFAULT): Int {
        if (s.isEmpty()) return y + 1
        var row = y
        for (paragraph in s.split('\n')) {
            if (paragraph.isEmpty()) { row++; continue }
            var line = StringBuilder()
            for (word in paragraph.split(' ')) {
                if (line.isEmpty()) {
                    if (word.length > maxWidth) {
                        // Hard-break very long words.
                        var w = word
                        while (w.length > maxWidth) {
                            text(x, row, w.substring(0, maxWidth), fg, bg, bold); row++
                            w = w.substring(maxWidth)
                        }
                        line.append(w)
                    } else {
                        line.append(word)
                    }
                } else if (line.length + 1 + word.length <= maxWidth) {
                    line.append(' ').append(word)
                } else {
                    text(x, row, line.toString(), fg, bg, bold); row++
                    line = StringBuilder(word)
                }
            }
            if (line.isNotEmpty()) {
                text(x, row, line.toString(), fg, bg, bold); row++
            }
        }
        return row
    }

    /**
     * Registers a tappable region, clipping it to the screen. Returns null if the
     * region lies entirely off-screen (which can happen on very short viewports).
     */
    fun hotspot(id: String, x: Int, y: Int, len: Int, height: Int = 1): Hotspot? {
        if (len <= 0 || width <= 0 || this.height <= 0) return null
        val y1raw = y + height - 1
        if (y >= this.height || y1raw < 0) return null
        if (x >= width || x + len - 1 < 0) return null
        val h = Hotspot(
            id,
            x.coerceIn(0, width - 1),
            y.coerceIn(0, this.height - 1),
            (x + len - 1).coerceIn(0, width - 1),
            y1raw.coerceIn(0, this.height - 1)
        )
        hotspots.add(h)
        return h
    }

    /** Returns the tappable region id at (x,y), if any. */
    fun hotspotAt(x: Int, y: Int): String? {
        for (i in hotspots.indices.reversed()) {
            if (hotspots[i].contains(x, y)) return hotspots[i].id
        }
        return null
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
