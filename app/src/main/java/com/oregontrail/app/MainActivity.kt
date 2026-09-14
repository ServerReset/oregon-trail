package com.oregontrail.app

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputFilter
import android.text.InputType
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.oregontrail.engine.DefaultRng
import com.oregontrail.engine.Game
import com.oregontrail.engine.Phase
import com.oregontrail.engine.Sound

class MainActivity : AppCompatActivity() {

    private lateinit var game: Game
    private lateinit var terminal: TerminalView
    private val handler = Handler(Looper.getMainLooper())
    private var tone: ToneGenerator? = null

    private val huntTicker = object : Runnable {
        override fun run() {
            if (game.phase == Phase.HUNTING) {
                game.huntTick()
                render()
                handler.postDelayed(this, 140L)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        terminal = TerminalView(this)
        val root = FrameLayout(this).apply { addView(terminal) }
        setContentView(root)

        game = Game(DefaultRng(), PrefsScoreStore(this))
        try {
            tone = ToneGenerator(AudioManager.STREAM_MUSIC, 60)
        } catch (_: Exception) {
            tone = null
        }

        terminal.viewportListener = { c, r ->
            game.setViewport(c, r)
            render()
        }
        terminal.tapListener = { id -> onHotspot(id) }

        hideSystemBars()
        render()
    }

    private fun onHotspot(id: String) {
        if (id == "title:end") {
            finish()
            return
        }
        game.onTap(id)
        handleNameRequest()
        render()
        if (game.phase == Phase.HUNTING) {
            handler.removeCallbacks(huntTicker)
            handler.post(huntTicker)
        } else {
            handler.removeCallbacks(huntTicker)
        }
    }

    private fun handleNameRequest() {
        val index = game.requestedNameEdit ?: return
        if (index !in game.party.indices) {
            game.clearNameRequest()
            return
        }
        val current = game.party[index].name
        val input = EditText(this).apply {
            setText(current)
            setSelection(text.length)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            filters = arrayOf(InputFilter.LengthFilter(12))
            hint = "Name"
        }
        AlertDialog.Builder(this)
            .setTitle("Name your traveler")
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                game.setName(index, input.text.toString())
                render()
            }
            .setNegativeButton("Cancel") { _, _ ->
                game.clearNameRequest()
                render()
            }
            .setOnCancelListener {
                game.clearNameRequest()
                render()
            }
            .show()
    }

    private fun render() {
        val s = game.render()
        terminal.screen = s
        if (DEBUG) {
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
        playPendingSound()
    }

    private fun playPendingSound() {
        val s = game.pendingSound ?: return
        game.pendingSound = null
        if (!game.soundEnabled) return
        val gen = tone ?: return
        val (type, duration) = when (s) {
            Sound.CLICK -> ToneGenerator.TONE_PROP_BEEP to 40
            Sound.GOOD -> ToneGenerator.TONE_PROP_ACK to 120
            Sound.BAD -> ToneGenerator.TONE_PROP_NACK to 180
            Sound.SHOOT -> ToneGenerator.TONE_CDMA_PIP to 60
            Sound.HIT -> ToneGenerator.TONE_PROP_ACK to 90
            Sound.DEATH -> ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD to 500
            Sound.ARRIVAL -> ToneGenerator.TONE_CDMA_ALERT_NETWORK_LITE to 400
        }
        try {
            gen.startTone(type, duration)
        } catch (_: Exception) {
            // Ignore audio failures.
        }
    }

    private fun hideSystemBars() {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(huntTicker)
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
        render()
    }

    override fun onDestroy() {
        super.onDestroy()
        tone?.release()
        tone = null
    }

    companion object {
        /** Emits a full screen dump to logcat for automated verification. */
        const val DEBUG = true
    }
}
