package com.oregontrail.app

import android.content.Context
import android.graphics.Color
import android.os.Build
import com.oregontrail.engine.Palette

/** A colour scheme for the terminal. */
interface ThemeColors {
    val name: String

    /** True for dark backgrounds (scanlines and vignette apply). */
    val dark: Boolean

    /** The base background colour (also used for the system bars). */
    val defaultBackground: Int

    fun foreground(p: Palette, highContrast: Boolean): Int
    fun ambientBackground(ambient: Palette, highContrast: Boolean): Int
    fun isBoldDefault(p: Palette): Boolean
}

/**
 * Terminal mode: a single green phosphor on black. Hues are kept in the green
 * family so the whole display feels like an old teleprinter.
 */
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
class MaterialYouTheme(private val context: Context, val light: Boolean = false) : ThemeColors {

    override val name: String = "Material You"
    override val dark: Boolean = !light

    private fun sys(resId: Int, fallback: Int): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                context.getColor(resId)
            } catch (_: Exception) {
                fallback
            }
        } else {
            fallback
        }

    private val primary = if (light) {
        sys(android.R.color.system_accent1_600, 0xFF6750A4.toInt())
    } else {
        sys(android.R.color.system_accent1_200, 0xFFD0BCFF.toInt())
    }
    private val primaryStrong = if (light) {
        sys(android.R.color.system_accent1_700, 0xFF54408D.toInt())
    } else {
        sys(android.R.color.system_accent1_100, 0xFFEADDFF.toInt())
    }
    private val secondary = if (light) {
        sys(android.R.color.system_accent2_600, 0xFF625B71.toInt())
    } else {
        sys(android.R.color.system_accent2_200, 0xFFCCC2DC.toInt())
    }
    private val tertiary = if (light) {
        sys(android.R.color.system_accent3_600, 0xFF7D5260.toInt())
    } else {
        sys(android.R.color.system_accent3_200, 0xFFEFB8C8.toInt())
    }
    private val onBackground = if (light) {
        sys(android.R.color.system_neutral1_900, 0xFF1C1B1F.toInt())
    } else {
        sys(android.R.color.system_neutral1_100, 0xFFE6E1E5.toInt())
    }
    private val outline = if (light) {
        sys(android.R.color.system_neutral2_600, 0xFF79747E.toInt())
    } else {
        sys(android.R.color.system_neutral2_400, 0xFF938F99.toInt())
    }
    private val surface = if (light) {
        sys(android.R.color.system_neutral1_10, 0xFFFFFBFE.toInt())
    } else {
        sys(android.R.color.system_neutral1_900, 0xFF1C1B1F.toInt())
    }
    private val error = if (light) 0xFFB3261E.toInt() else 0xFFF2B8B5.toInt()

    override val defaultBackground: Int = surface

    override fun foreground(p: Palette, highContrast: Boolean): Int = when (p) {
        Palette.DEFAULT, Palette.GREEN -> onBackground
        Palette.WHITE, Palette.BRIGHT_WHITE -> onBackground
        Palette.BRIGHT_GREEN -> primaryStrong
        Palette.BLUE, Palette.CYAN -> secondary
        Palette.YELLOW, Palette.BROWN, Palette.BRIGHT_YELLOW -> tertiary
        Palette.RED, Palette.MAGENTA -> error
        Palette.GRAY, Palette.DIM -> outline
        Palette.BLACK -> surface
    }

    override fun ambientBackground(ambient: Palette, highContrast: Boolean): Int {
        if (ambient == Palette.BRIGHT_WHITE) return blend(surface, onBackground, 0.4)
        val tint = when (ambient) {
            Palette.BLUE, Palette.CYAN -> secondary
            Palette.BROWN, Palette.YELLOW -> tertiary
            Palette.GREEN, Palette.BRIGHT_GREEN -> primary
            Palette.RED, Palette.MAGENTA -> error
            else -> return surface
        }
        return blend(surface, tint, 0.12)
    }

    override fun isBoldDefault(p: Palette): Boolean =
        p == Palette.BRIGHT_GREEN || p == Palette.BRIGHT_YELLOW || p == Palette.BRIGHT_WHITE

    private fun blend(a: Int, b: Int, f: Double): Int {
        val rf = f.coerceIn(0.0, 1.0)
        val r = (Color.red(a) * (1 - rf) + Color.red(b) * rf).toInt()
        val g = (Color.green(a) * (1 - rf) + Color.green(b) * rf).toInt()
        val bl = (Color.blue(a) * (1 - rf) + Color.blue(b) * rf).toInt()
        return Color.rgb(r, g, bl)
    }
}
