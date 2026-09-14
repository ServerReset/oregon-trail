package com.oregontrail.app

import android.content.Context
import android.graphics.Color
import android.os.Build
import com.oregontrail.engine.Palette

/** A colour scheme for the terminal: the retro CRT or a Material You theme. */
interface ThemeColors {
    val name: String

    /** The base background colour (also used for the system bars). */
    val defaultBackground: Int

    fun foreground(p: Palette, highContrast: Boolean): Int
    fun ambientBackground(ambient: Palette, highContrast: Boolean): Int
    fun isBoldDefault(p: Palette): Boolean
}

/** The classic green phosphor terminal. */
object RetroPalette : ThemeColors {

    const val BACKGROUND: Int = 0xFF08130A.toInt()

    override val name: String = "Retro Green"
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
    }

    /** Dark background tints used for the ambient mood. */
    fun bg(ambient: Palette, highContrast: Boolean = false): Int {
        if (highContrast) return 0xFF000000.toInt()
        return when (ambient) {
            Palette.BLACK -> BACKGROUND
            Palette.GREEN, Palette.BRIGHT_GREEN -> 0xFF0A1C0E.toInt()
            Palette.BLUE, Palette.CYAN -> 0xFF08101F.toInt()
            Palette.BROWN, Palette.YELLOW -> 0xFF1A1208.toInt()
            Palette.RED, Palette.MAGENTA -> 0xFF1A0A0A.toInt()
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
 * a Material 3 dark baseline on older devices.
 */
class MaterialYouTheme(private val context: Context) : ThemeColors {

    override val name: String = "Material You"

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

    private val primary = sys(android.R.color.system_accent1_200, 0xFFD0BCFF.toInt())
    private val primaryBright = sys(android.R.color.system_accent1_100, 0xFFEADDFF.toInt())
    private val secondary = sys(android.R.color.system_accent2_200, 0xFFCCC2DC.toInt())
    private val tertiary = sys(android.R.color.system_accent3_200, 0xFFEFB8C8.toInt())
    private val onBackground = sys(android.R.color.system_neutral1_100, 0xFFE6E1E5.toInt())
    private val outline = sys(android.R.color.system_neutral2_400, 0xFF938F99.toInt())
    private val surface = sys(android.R.color.system_neutral1_900, 0xFF1C1B1F.toInt())
    private val error = 0xFFF2B8B5.toInt()

    override val defaultBackground: Int = surface

    override fun foreground(p: Palette, highContrast: Boolean): Int = when (p) {
        Palette.DEFAULT, Palette.GREEN, Palette.WHITE, Palette.BRIGHT_WHITE -> onBackground
        Palette.BRIGHT_GREEN -> primaryBright
        Palette.BLUE, Palette.CYAN -> secondary
        Palette.YELLOW, Palette.BROWN, Palette.BRIGHT_YELLOW -> tertiary
        Palette.RED, Palette.MAGENTA -> error
        Palette.GRAY, Palette.DIM -> outline
        Palette.BLACK -> surface
    }

    override fun ambientBackground(ambient: Palette, highContrast: Boolean): Int {
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
