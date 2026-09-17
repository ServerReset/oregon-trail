package com.oregontrail.app

import android.content.Context
import com.oregontrail.engine.UiSettings

/** Persisted presentation settings shared with the engine's management screen. */
class AppUiSettings(context: Context) : UiSettings {

    private val prefs = context.getSharedPreferences("oregon_trail_ui", Context.MODE_PRIVATE)

    override var textScaleIndex: Int
        get() = prefs.getInt(KEY_TEXT_SCALE, 1)
        set(value) {
            prefs.edit().putInt(KEY_TEXT_SCALE, value.coerceIn(0, 2)).apply()
        }

    override var highContrast: Boolean
        get() = prefs.getBoolean(KEY_CONTRAST, false)
        set(value) {
            prefs.edit().putBoolean(KEY_CONTRAST, value).apply()
        }

    override var scanlines: Boolean
        get() = prefs.getBoolean(KEY_SCANLINES, true)
        set(value) {
            prefs.edit().putBoolean(KEY_SCANLINES, value).apply()
        }

    override var haptics: Boolean
        get() = prefs.getBoolean(KEY_HAPTICS, true)
        set(value) {
            prefs.edit().putBoolean(KEY_HAPTICS, value).apply()
        }

    override var soundEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND, true)
        set(value) {
            prefs.edit().putBoolean(KEY_SOUND, value).apply()
        }

    override var crtMode: Int
        get() = prefs.getInt(KEY_CRT, 1)
        set(value) {
            prefs.edit().putInt(KEY_CRT, value.coerceIn(0, 2)).apply()
        }

    override var themeIndex: Int
        get() = prefs.getInt(KEY_THEME, 1)
        set(value) {
            prefs.edit().putInt(KEY_THEME, value.coerceIn(0, 2)).apply()
        }

    companion object {
        private const val KEY_TEXT_SCALE = "text_scale"
        private const val KEY_CONTRAST = "contrast"
        private const val KEY_SCANLINES = "scanlines"
        private const val KEY_HAPTICS = "haptics"
        private const val KEY_SOUND = "sound"
        private const val KEY_THEME = "theme"
        private const val KEY_CRT = "crt_mode"
    }
}
