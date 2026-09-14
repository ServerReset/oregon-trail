package com.oregontrail.app

import android.graphics.Color
import com.oregontrail.engine.Palette

/** Maps engine logical colors to the retro phosphor palette used on screen. */
object RetroPalette {

    const val BACKGROUND: Int = 0xFF08130A.toInt()

    fun fg(p: Palette): Int = when (p) {
        Palette.DEFAULT -> 0xFF7CFF7C.toInt()
        Palette.BLACK -> 0xFF08130A.toInt()
        Palette.DIM -> 0xFF3E8E3E.toInt()
        Palette.RED -> 0xFFFF6B5E.toInt()
        Palette.GREEN -> 0xFF7CFF7C.toInt()
        Palette.YELLOW, Palette.BROWN -> 0xFFFFD24A.toInt()
        Palette.BLUE -> 0xFF6B9BFF.toInt()
        Palette.MAGENTA -> 0xFFFF7CE0.toInt()
        Palette.CYAN -> 0xFF6BE8FF.toInt()
        Palette.WHITE -> 0xFFE8FFE8.toInt()
        Palette.GRAY -> 0xFF9AA89A.toInt()
        Palette.BRIGHT_GREEN -> 0xFFB6FFB6.toInt()
        Palette.BRIGHT_YELLOW -> 0xFFFFE58A.toInt()
        Palette.BRIGHT_WHITE -> 0xFFFFFFFF.toInt()
    }

    fun isBoldDefault(p: Palette): Boolean = p == Palette.BRIGHT_GREEN || p == Palette.BRIGHT_YELLOW || p == Palette.BRIGHT_WHITE
}
