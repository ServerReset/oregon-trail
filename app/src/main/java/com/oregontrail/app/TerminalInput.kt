package com.oregontrail.app

import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import com.oregontrail.engine.Screen
import com.oregontrail.engine.*

/**
 * Handles all input for a [TerminalView]: the selection cursor (rotary bezel,
 * D-pad, trackball), tap-to-hotspot mapping and held-control auto-repeat.
 */
class TerminalInput(private val view: View) {

    var screen: Screen? = null
        set(value) {
            field = value
            val count = value?.hotspots?.size ?: 0
            selectedIndex = if (count == 0) 0 else selectedIndex.coerceIn(0, count - 1)
            view.invalidate()
        }

    var tapListener: ((String) -> Unit)? = null
    var selectionEnabled: Boolean = false
    var hapticsEnabled: Boolean = true
    var selectedIndex: Int = 0
        private set

    private val repeatHandler = Handler(Looper.getMainLooper())
    private var repeatId: String? = null
    private val repeatRunnable = object : Runnable {
        override fun run() {
            val id = repeatId ?: return
            if (BuildConfig.DEBUG) android.util.Log.i("OTS", "repeat $id")
            tapListener?.invoke(id)
            repeatHandler.postDelayed(this, 90L)
        }
    }

    private fun isRepeatable(id: String): Boolean =
        id.startsWith("store:inc:") || id.startsWith("store:dec:") ||
            id.startsWith("hunt:") || id.startsWith("raft:")

    private fun vibrate(constant: Int) {
        if (hapticsEnabled) view.performHapticFeedback(constant)
    }

    /** Moves the selection cursor (rotary bezel, D-pad or trackball). */
    fun moveSelection(delta: Int) {
        val count = screen?.hotspots?.size ?: 0
        if (count == 0) return
        selectedIndex = ((selectedIndex + delta) % count + count) % count
        vibrate(HapticFeedbackConstants.CLOCK_TICK)
        view.invalidate()
    }

    fun activateSelected() {
        val id = screen?.hotspots?.getOrNull(selectedIndex)?.id ?: return
        vibrate(HapticFeedbackConstants.KEYBOARD_TAP)
        tapListener?.invoke(id)
    }

    fun onGenericMotion(event: MotionEvent): Boolean {
        if (event.isFromSource(InputDevice.SOURCE_ROTARY_ENCODER)) {
            val scroll = event.getAxisValue(MotionEvent.AXIS_SCROLL)
            if (scroll != 0f) {
                moveSelection(if (scroll > 0f) 1 else -1)
                return true
            }
        }
        return false
    }

    fun onKeyDown(keyCode: Int): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_SPACE,
            KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_SELECT, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                activateSelected()
                return true
            }
            KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT -> {
                moveSelection(1); return true
            }
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_LEFT -> {
                moveSelection(-1); return true
            }
        }
        return false
    }

    fun onTouch(event: MotionEvent, grid: TerminalGrid): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val cx = ((event.x - grid.marginX) / grid.cellW).toInt()
                val cy = ((event.y - grid.marginY) / grid.lineH).toInt()
                val id = screen?.hotspotAt(cx, cy)
                if (BuildConfig.DEBUG) {
                    android.util.Log.i("OTS", "touch x=${event.x} y=${event.y} cell=($cx,$cy) id=$id")
                }
                if (id != null) {
                    screen?.hotspots?.indexOfFirst { it.id == id && it.contains(cx, cy) }
                        ?.takeIf { it >= 0 }?.let { selectedIndex = it }
                    vibrate(HapticFeedbackConstants.KEYBOARD_TAP)
                    tapListener?.invoke(id)
                    if (isRepeatable(id)) {
                        repeatId = id
                        repeatHandler.postDelayed(repeatRunnable, 420L)
                    }
                    return true
                }
                // On a watch, a tap anywhere activates the highlighted target.
                if (selectionEnabled) {
                    activateSelected()
                    return true
                }
                return false
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                repeatHandler.removeCallbacks(repeatRunnable)
                repeatId = null
            }
        }
        return false
    }
}
