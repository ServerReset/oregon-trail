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
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.oregontrail.engine.DefaultRng
import com.oregontrail.engine.Game
import com.oregontrail.engine.Phase
import com.oregontrail.engine.SaveSlot
import com.oregontrail.engine.Sound

class MainActivity : AppCompatActivity() {

    private lateinit var game: Game
    private lateinit var terminal: TerminalView
    private lateinit var store: PrefsScoreStore
    private lateinit var ui: AppUiSettings
    private val handler = Handler(Looper.getMainLooper())
    private var tone: ToneGenerator? = null
    private var lastDump: String? = null

    private val animator = object : Runnable {
        override fun run() {
            when (game.phase) {
                Phase.HUNTING -> game.huntTick()
                Phase.RAFTING -> game.raftTick()
                Phase.BARLOW -> game.barlowTick()
                else -> game.animate()
            }
            render()
            val delay = if (game.phase == Phase.HUNTING || game.phase == Phase.RAFTING ||
                game.phase == Phase.BARLOW
            ) 150L else 280L
            handler.postDelayed(this, delay)
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
        refreshSlots()
        game.autosaveAvailable = store.loadState() != null
        game.dailySeed = dailySeed()
        game.transitions = true
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

        // Back opens the pause menu (or leaves the title/end screens).
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    game.phase == Phase.TITLE -> finish()
                    game.phase == Phase.PAUSE -> { game.onTap("pause:resume"); render() }
                    game.canPause() -> { game.onTap("pause:open"); render() }
                    else -> finish()
                }
            }
        })

        hideSystemBars()
        terminal.selectionEnabled = isWatchLike()
        applyUi()
        render()
    }

    /** A seed that is the same for everyone on a given day. */
    private fun dailySeed(): Long {
        val c = java.util.Calendar.getInstance()
        return c.get(java.util.Calendar.YEAR) * 10000L +
            (c.get(java.util.Calendar.MONTH) + 1) * 100L +
            c.get(java.util.Calendar.DAY_OF_MONTH)
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
        if (id == "title:end" || id == "pause:quit") {
            finish()
            return
        }
        // Save the current position before leaving the pause menu to the title.
        if (id == "pause:title") store.saveState(game.save())
        game.onTap(id)
        handleNameRequest()
        handleEpitaphRequest()
        handleAutosaveRequest()
        handleQuickSave()
        handleQuickLoad()
        handleSaveRequest()
        handleLoadRequest()
        handleRenameRequest()
        handleDeleteRequest()
        refreshSlots()
        game.pendingUnlock?.let { name ->
            Toast.makeText(this, "Achievement unlocked: $name", Toast.LENGTH_LONG).show()
            game.pendingUnlock = null
        }
        applyUi()
        render()
        handler.removeCallbacks(animator)
        handler.post(animator)
    }

    private fun applyUi() {
        terminal.textScale = when (ui.textScaleIndex) {
            0 -> 0.8f
            1 -> 1.0f
            else -> 1.3f
        }
        terminal.highContrast = ui.highContrast
        terminal.scanlinesEnabled = ui.scanlines
        terminal.colors = if (ui.themeIndex == 1) MaterialYouTheme(this) else RetroPalette
        val bg = terminal.colors.defaultBackground
        window.statusBarColor = bg
        window.navigationBarColor = bg
        window.decorView.setBackgroundColor(bg)
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
        MaterialAlertDialogBuilder(this)
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
        MaterialAlertDialogBuilder(this)
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

    private fun handleQuickSave() {
        if (!game.requestedQuickSave) return
        store.saveState(game.save())
        game.clearQuickSaveRequest()
        game.autosaveAvailable = true
        Toast.makeText(this, "Quick saved", Toast.LENGTH_SHORT).show()
    }

    private fun handleQuickLoad() {
        if (!game.requestedQuickLoad) return
        store.loadState()?.let { data -> game.load(data) }
        game.clearQuickLoadRequest()
        Toast.makeText(this, "Quick loaded", Toast.LENGTH_SHORT).show()
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
        MaterialAlertDialogBuilder(this)
            .setTitle("Save game")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val detail = "${game.date}, ${game.miles} mi, ${game.occupation.displayName}"
                store.saveSlot(input.text.toString(), detail, game.save())
                refreshSlots()
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

    /** Rebuilds the slot list, including the autosave as a "Quick save" entry. */
    private fun refreshSlots() {
        val slots = store.listSlots().toMutableList()
        val auto = store.loadState()
        if (auto != null) {
            slots.removeAll { it.id == AUTO_ID }
            slots.add(
                0,
                SaveSlot(AUTO_ID, "Quick save (auto)", "Resumes exactly where you left off", Long.MAX_VALUE, auto)
            )
        } else {
            slots.removeAll { it.id == AUTO_ID }
        }
        game.saveSlots = slots.sortedByDescending { it.savedAt }
    }

    private fun handleLoadRequest() {
        val id = game.requestedLoadId ?: return
        if (id == AUTO_ID) store.loadState()?.let { data -> game.load(data) }
        else store.loadSlotData(id)?.let { data -> game.load(data) }
        game.clearLoadRequest()
        refreshSlots()
    }

    private fun handleRenameRequest() {
        val id = game.requestedRenameId ?: return
        val current = game.saveSlots.firstOrNull { it.id == id }
        if (current == null || id == AUTO_ID) {
            game.clearRenameRequest()
            return
        }
        val input = EditText(this).apply {
            setText(current.label)
            setSelection(text.length)
            filters = arrayOf(InputFilter.LengthFilter(24))
            hint = "Save name"
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("Rename saved game")
            .setView(input)
            .setPositiveButton("Rename") { _, _ ->
                store.renameSlot(id, input.text.toString())
                game.clearRenameRequest()
                refreshSlots()
                render()
            }
            .setNegativeButton("Cancel") { _, _ ->
                game.clearRenameRequest()
                render()
            }
            .setOnCancelListener {
                game.clearRenameRequest()
                render()
            }
            .show()
    }

    private fun handleDeleteRequest() {
        val id = game.requestedDeleteId ?: return
        val slot = game.saveSlots.firstOrNull { it.id == id }
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete saved game")
            .setMessage(slot?.label ?: "this save")
            .setPositiveButton("Delete") { _, _ ->
                if (id == AUTO_ID) store.clearState() else store.deleteSlot(id)
                refreshSlots()
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
        handler.removeCallbacks(animator)
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
        refreshSlots()
        game.autosaveAvailable = store.loadState() != null
        applyUi()
        render()
        handler.removeCallbacks(animator)
        handler.post(animator)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(animator)
        tone?.release()
        tone = null
    }

    private companion object {
        /** Pseudo-id for the autosave shown in the saved-games list. */
        const val AUTO_ID = "__auto__"
    }
}
