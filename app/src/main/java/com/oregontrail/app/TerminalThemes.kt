package com.oregontrail.app

import com.oregontrail.engine.Palette

object TerminalTheme : ThemeColors {
    const val BLACK: Int = 0xFF000000.toInt()

    override val name: String = "Terminal"
    override val dark: Boolean = true
    override val defaultBackground: Int = BLACK

    override fun foreground(p: Palette, highContrast: Boolean): Int = when (p) {
        Palette.BLACK -> BLACK
        Palette.DIM -> 0xFF1E8E1E.toInt()
        Palette.GRAY -> 0xFF2FB02F.toInt()
        Palette.DEFAULT, Palette.GREEN -> 0xFF33FF33.toInt()
        Palette.BRIGHT_GREEN, Palette.BRIGHT_WHITE, Palette.WHITE -> 0xFFB6FFB6.toInt()
        Palette.YELLOW, Palette.BRIGHT_YELLOW, Palette.BROWN -> 0xFF9BFF33.toInt()
        Palette.CYAN, Palette.BLUE -> 0xFF4FE0A0.toInt()
        Palette.RED, Palette.MAGENTA -> 0xFF33FF99.toInt()
    }

    override fun ambientBackground(ambient: Palette, highContrast: Boolean): Int =
        if (ambient == Palette.BLACK) BLACK else 0xFF031003.toInt()

    override fun isBoldDefault(p: Palette): Boolean =
        p == Palette.BRIGHT_GREEN || p == Palette.BRIGHT_YELLOW || p == Palette.BRIGHT_WHITE
}

/** Classic mode: full-colour ASCII art on black. */
object RetroPalette : ThemeColors {

    const val BACKGROUND: Int = 0xFF000000.toInt()

    override val name: String = "Classic"
    override val dark: Boolean = true
    override val defaultBackground: Int = BACKGROUND

    fun fg(p: Palette, highContrast: Boolean = false): Int {
        if (highContrast) {
            return when (p) {
                Palette.DEFAULT, Palette.GREEN, Palette.BRIGHT_GREEN -> 0xFFFFFFFF.toInt()
                Palette.BLACK -> BACKGROUND
                Palette.DIM, Palette.GRAY -> 0xFFC8C8C8.toInt()
                Palette.YELLOW, Palette.BRIGHT_YELLOW, Palette.BROWN -> 0xFFFFE000.toInt()
                Palette.CYAN, Palette.BLUE -> 0xFF00E5FF.toInt()
                Palette.RED, Palette.MAGENTA -> 0xFFFF5C5C.toInt()
                Palette.WHITE, Palette.BRIGHT_WHITE -> 0xFFFFFFFF.toInt()
            }
        }
        return when (p) {
            Palette.DEFAULT -> 0xFF7CFF7C.toInt()
            Palette.BLACK -> BACKGROUND
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
    }

    /** Near-black background tints used for the ambient mood. */
    fun bg(ambient: Palette, highContrast: Boolean = false): Int {
        if (highContrast) return 0xFF000000.toInt()
        return when (ambient) {
            Palette.BRIGHT_WHITE -> 0xFF303030.toInt() // lightning flash
            Palette.GREEN, Palette.BRIGHT_GREEN -> 0xFF031403.toInt()
            Palette.BLUE, Palette.CYAN -> 0xFF020617.toInt()
            Palette.BROWN, Palette.YELLOW -> 0xFF160C02.toInt()
            Palette.RED, Palette.MAGENTA -> 0xFF170202.toInt()
            else -> BACKGROUND
        }
    }

    override fun foreground(p: Palette, highContrast: Boolean): Int = fg(p, highContrast)
    override fun ambientBackground(ambient: Palette, highContrast: Boolean): Int = bg(ambient, highContrast)
    override fun isBoldDefault(p: Palette): Boolean =
        p == Palette.BRIGHT_GREEN || p == Palette.BRIGHT_YELLOW || p == Palette.BRIGHT_WHITE
}

/**
 * Material You: colours derived from the system wallpaper on Android 12+, with
 * a Material 3 baseline on older devices. Follows the system light/dark mode.
 */
