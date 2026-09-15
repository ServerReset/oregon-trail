package com.oregontrail.app

import android.content.res.Configuration
import androidx.core.view.WindowInsetsControllerCompat

internal fun MainActivity.applyUi() {
        terminal.textScale = when (ui.textScaleIndex) {
            0 -> 0.8f
            1 -> 1.0f
            else -> 1.3f
        }
        terminal.highContrast = ui.highContrast
        terminal.scanlinesEnabled = ui.scanlines
        terminal.hapticsEnabled = ui.haptics
        terminal.colors = when (ui.themeIndex) {
            1 -> RetroPalette
            2 -> MaterialYouTheme(this, light = isSystemLight())
            else -> TerminalTheme
        }
        val bg = terminal.colors.defaultBackground
        window.statusBarColor = bg
        window.navigationBarColor = bg
        window.decorView.setBackgroundColor(bg)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.isAppearanceLightStatusBars = !terminal.colors.dark
        controller.isAppearanceLightNavigationBars = !terminal.colors.dark
    }


    /** Whether the system is in light mode (used by Material You). */
internal fun MainActivity.isSystemLight(): Boolean =
        (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_NO

