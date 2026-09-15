package com.oregontrail.app

import android.content.res.Configuration
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
import androidx.activity.result.contract.ActivityResultContracts
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
import com.oregontrail.engine.*

class MainActivity : AppCompatActivity() {

    internal lateinit var game: Game
    internal lateinit var terminal: TerminalView
    internal lateinit var store: PrefsScoreStore
    internal lateinit var ui: AppUiSettings
    internal val handler = Handler(Looper.getMainLooper())
    internal val sound = SoundPlayer()
    internal lateinit var saves: SaveFileHost
    internal var lastDump: String? = null

    internal val animator = object : Runnable {
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

        saves = SaveFileHost(this)
        game = Game(DefaultRng(), PrefsScoreStore(this).also { store = it })
        ui = AppUiSettings(this)
        game.uiSettings = ui
        // The title screen offers Continue (autosave) and Load (named slots).
        refreshSlots()
        game.autosaveAvailable = store.loadState() != null
        game.dailySeed = dailySeed()
        game.transitions = true
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

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        terminal.selectionEnabled = isWatchLike()
        terminal.refreshGrid()
        applyUi()
        render()
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
        sound.release()
    }
}
