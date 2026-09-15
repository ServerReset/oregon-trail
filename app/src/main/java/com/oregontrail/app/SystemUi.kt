package com.oregontrail.app

import android.content.res.Configuration
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/** A seed that is the same for everyone on a given day. */
internal fun MainActivity.dailySeed(): Long {
    val c = java.util.Calendar.getInstance()
    return c.get(java.util.Calendar.YEAR) * 10000L +
        (c.get(java.util.Calendar.MONTH) + 1) * 100L +
        c.get(java.util.Calendar.DAY_OF_MONTH)
}

/** Watches and round displays get the rotary-driven selection cursor. */
internal fun MainActivity.isWatchLike(): Boolean {
    val uiMode = resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK
    if (uiMode == Configuration.UI_MODE_TYPE_WATCH || resources.configuration.isScreenRound) return true
    // Debug builds can force watch mode for testing on a phone emulator.
    return BuildConfig.DEBUG &&
        android.provider.Settings.Global.getInt(contentResolver, "oregon_force_watch", 0) == 1
}

/** Immersive full-screen: hides the system bars, swipe to reveal them. */
internal fun MainActivity.hideSystemBars() {
    val controller = WindowInsetsControllerCompat(window, window.decorView)
    controller.hide(WindowInsetsCompat.Type.systemBars())
    controller.systemBarsBehavior =
        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
}
