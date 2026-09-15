package com.oregontrail.app

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
