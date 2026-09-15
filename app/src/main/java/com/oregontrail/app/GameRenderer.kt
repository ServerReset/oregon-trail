package com.oregontrail.app

import com.oregontrail.engine.Phase
import com.oregontrail.engine.*

internal fun MainActivity.render() {
    val s = game.render()
    terminal.screen = s
    if (BuildConfig.DEBUG) {
        // Only dump when the content actually changes, so animation does
        // not flood the log.
        val text = s.toText()
        if (text != lastDump) {
            lastDump = text
            val m = terminal.metrics()
            val loc = IntArray(2)
            terminal.getLocationOnScreen(loc)
            android.util.Log.i(
                "OTS",
                "grid=${terminal.currentGrid()} phase=${game.phase} " +
                    "origin=${loc[0]},${loc[1]} " +
                    "metrics=%.2f,%.2f,%.2f,%.2f".format(m[0], m[1], m[2], m[3])
            )
            s.toLines().forEachIndexed { y, line ->
                android.util.Log.i("OTS", "R%02d|%s".format(y, line))
            }
        }
    }
    playPendingSound()
}


internal fun MainActivity.playPendingSound() {
    val s = game.pendingSound ?: return
    game.pendingSound = null
    sound.haptic(window.decorView, s, ui.haptics)
    if (!game.soundEnabled) return
    sound.play(s)
}

