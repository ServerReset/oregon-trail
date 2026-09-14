package com.oregontrail.app

import android.content.res.Configuration
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
import android.widget.Toast
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
    private lateinit var store: PrefsScoreStore
    private lateinit var ui: AppUiSettings
    private val handler = Handler(Looper.getMainLooper())
    private var tone: ToneGenerator? = null

    private val ticker = object : Runnable {
        override fun run() {
            when (game.phase) {
                Phase.HUNTING -> game.huntTick()
                Phase.RAFTING -> game.raftTick()
                else -> return
            }
            render()
            handler.postDelayed(this, 150L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        terminal = TerminalView(this)
        val root = FrameLayout(this).apply { addView(terminal) }
        setContentView(root)
        terminal.requestFocus()

        game = Game(DefaultRng(), PrefsScoreStore(this).also { store = it })
        ui = AppUiSettings(this)
        game.uiSettings = ui
        // The title screen offers Continue (autosave) and Load (named slots).
        game.saveSlots = store.listSlots()
        game.autosaveAvailable = store.loadState() != null
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
        terminal.selectionEnabled = isWatchLike()
        applyUi()
        render()
    }

    /** Watches and round displays get the rotary-driven selection cursor. */
    private fun isWatchLike(): Boolean {
        val uiMode = resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK
        if (uiMode == Configuration.UI_MODE_TYPE_WATCH || resources.configuration.isScreenRound) return true
        // Debug builds can force watch mode for testing on a phone emulator.
        return BuildConfig.DEBUG &&
            android.provider.Settings.Global.getInt(contentResolver, "oregon_force_watch", 0) == 1
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        terminal.selectionEnabled = isWatchLike()
        terminal.refreshGrid()
        applyUi()
        render()
    }

    private fun onHotspot(id: String) {
        if (id == "title:end") {
            finish()
            return
        }
        game.onTap(id)
        handleNameRequest()
        handleEpitaphRequest()
        handleAutosaveRequest()
        handleSaveRequest()
        handleLoadRequest()
        handleDeleteRequest()
        game.pendingUnlock?.let { name ->
            Toast.makeText(this, "Achievement unlocked: $name", Toast.LENGTH_LONG).show()
            game.pendingUnlock = null
        }
        applyUi()
        render()
        if (game.phase == Phase.HUNTING || game.phase == Phase.RAFTING) {
            handler.removeCallbacks(ticker)
            handler.post(ticker)
        } else {
            handler.removeCallbacks(ticker)
        }
    }

    private fun applyUi() {
        terminal.textScale = when (ui.textScaleIndex) {
            0 -> 0.8f
            1 -> 1.0f
            else -> 1.3f
        }
        terminal.highContrast = ui.highContrast
        terminal.scanlinesEnabled = ui.scanlines
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

    private fun handleEpitaphRequest() {
        if (!game.requestedEpitaphEdit) return
        val input = EditText(this).apply {
            setText(game.lastGravestone ?: "")
            setSelection(text.length)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            filters = arrayOf(InputFilter.LengthFilter(140))
            hint = "Epitaph"
        }
        AlertDialog.Builder(this)
            .setTitle("Write an epitaph")
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                game.setEpitaph(input.text.toString())
                render()
            }
            .setNegativeButton("Cancel") { _, _ ->
                game.clearEpitaphRequest()
                render()
            }
            .setOnCancelListener {
                game.clearEpitaphRequest()
                render()
            }
            .show()
    }

    private fun handleAutosaveRequest() {
        if (!game.requestedAutosaveLoad) return
        store.loadState()?.let { data -> game.load(data) }
        game.clearAutosaveRequest()
        game.autosaveAvailable = false
    }

    private fun handleSaveRequest() {
        if (!game.requestedSave) return
        val suggestion = "${game.party.firstOrNull()?.name ?: "Wagon"} - " +
            "${game.date.monthName} ${game.date.day}"
        val input = EditText(this).apply {
            setText(suggestion)
            setSelection(text.length)
            filters = arrayOf(InputFilter.LengthFilter(24))
            hint = "Save name"
        }
        AlertDialog.Builder(this)
            .setTitle("Save game")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val detail = "${game.date}, ${game.miles} mi, ${game.occupation.displayName}"
                store.saveSlot(input.text.toString(), detail, game.save())
                game.saveSlots = store.listSlots()
                game.clearSaveRequest()
                render()
            }
            .setNegativeButton("Cancel") { _, _ ->
                game.clearSaveRequest()
                render()
            }
            .setOnCancelListener {
                game.clearSaveRequest()
                render()
            }
            .show()
    }

    private fun handleLoadRequest() {
        val id = game.requestedLoadId ?: return
        store.loadSlotData(id)?.let { data -> game.load(data) }
        game.clearLoadRequest()
        game.saveSlots = store.listSlots()
    }

    private fun handleDeleteRequest() {
        val id = game.requestedDeleteId ?: return
        val slot = store.listSlots().firstOrNull { it.id == id }
        AlertDialog.Builder(this)
            .setTitle("Delete saved game")
            .setMessage(slot?.label ?: "this save")
            .setPositiveButton("Delete") { _, _ ->
                store.deleteSlot(id)
                game.saveSlots = store.listSlots()
                game.clearDeleteRequest()
                render()
            }
            .setNegativeButton("Keep") { _, _ ->
                game.clearDeleteRequest()
                render()
            }
            .setOnCancelListener {
                game.clearDeleteRequest()
                render()
            }
            .show()
    }

    private fun render() {
        val s = game.render()
        terminal.screen = s
        if (BuildConfig.DEBUG) {
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
        handler.removeCallbacks(ticker)
        when (game.phase) {
            Phase.DEATH, Phase.ARRIVED -> store.clearState()
            Phase.TITLE -> { /* keep any existing autosave so Continue still works */ }
            else -> store.saveState(game.save())
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
        terminal.selectionEnabled = isWatchLike()
        game.saveSlots = store.listSlots()
        game.autosaveAvailable = store.loadState() != null
        applyUi()
        render()
    }

    override fun onDestroy() {
        super.onDestroy()
        tone?.release()
        tone = null
    }
}
