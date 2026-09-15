package com.oregontrail.engine

/** Draws a single-line box with an optional title. */
/** Draws a single-line box with an optional title. */
fun Screen.box(x: Int, y: Int, w: Int, h: Int, fg: Palette = Palette.DEFAULT,
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
/**
 * Word-wraps [s] into [maxWidth] and writes it starting at (x,y).
 * Returns the row after the last written line.
 */
fun Screen.wrap(x: Int, y: Int, maxWidth: Int, s: String, fg: Palette = Palette.DEFAULT,
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
/**
 * Registers a tappable region, clipping it to the screen. Returns null if the
 * region lies entirely off-screen (which can happen on very short viewports).
 */
fun Screen.hotspot(id: String, x: Int, y: Int, len: Int, height: Int = 1): Hotspot? {
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
/** Returns the tappable region id at (x,y), if any. */
fun Screen.hotspotAt(x: Int, y: Int): String? {
    for (i in hotspots.indices.reversed()) {
        if (hotspots[i].contains(x, y)) return hotspots[i].id
    }
    return null
}
