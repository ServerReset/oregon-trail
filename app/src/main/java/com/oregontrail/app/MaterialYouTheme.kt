package com.oregontrail.app

import android.content.Context
import android.graphics.Color
import android.os.Build
import com.oregontrail.engine.Palette

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
