package com.oregontrail.engine

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
