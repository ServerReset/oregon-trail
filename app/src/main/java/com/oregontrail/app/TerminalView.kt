package com.oregontrail.app

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import com.oregontrail.engine.Screen

/**
 * Renders an engine [Screen] as a grid of monospace characters and turns taps
 * into hotspot ids. The character grid is recalculated on every size change so
 * the game adapts to any screen size or orientation.
 */
class TerminalView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val grid = TerminalGrid()
    private val painter = TerminalPainter()
    private val input = TerminalInput(this)

    var screen: Screen?
        get() = input.screen
        set(value) { input.screen = value }

    var tapListener: ((String) -> Unit)?
        get() = input.tapListener
        set(value) { input.tapListener = value }

    /** Called whenever the available character grid changes. */
    var viewportListener: ((cols: Int, rows: Int) -> Unit)? = null

    /**
     * Enables the watch-style selection cursor: the rotary bezel/crown and
     * D-pad move a highlighted target, and a tap or the center button
     * activates it. Off on phones so the existing direct-tap UX is unchanged.
     */
    var selectionEnabled: Boolean
        get() = input.selectionEnabled
        set(value) {
            if (input.selectionEnabled == value) return
            input.selectionEnabled = value
            invalidate()
        }

    var scanlinesEnabled = true

    /** Accessibility scale: <1 fits more columns, >1 makes glyphs larger. */
    var textScale: Float = 1f
        set(value) {
            val clamped = value.coerceIn(0.6f, 1.6f)
            if (field == clamped) return
            field = clamped
            if (width > 0 && height > 0) {
                recomputeGrid(width, height)
                viewportListener?.invoke(grid.cols, grid.rows)
            }
            invalidate()
        }

    /** Switches to a bright, high-contrast palette for readability. */
    var highContrast: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            invalidate()
        }

    /** The active colour scheme: Terminal, Classic or Material You. */
    var colors: ThemeColors = TerminalTheme

    /** Whether taps vibrate. */
    var hapticsEnabled: Boolean
        get() = input.hapticsEnabled
        set(value) { input.hapticsEnabled = value }

    init {
        isFocusable = true
        isFocusableInTouchMode = true
    }

    /** Moves the selection cursor (rotary bezel, D-pad or trackball). */
    fun moveSelection(delta: Int) = input.moveSelection(delta)

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w == 0 || h == 0) return
        painter.resize(w, h)
        recomputeGrid(w, h)
        viewportListener?.invoke(grid.cols, grid.rows)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Density and font scale can change without the view size changing.
        refreshGrid()
    }

    /** Recomputes the character grid from the current size and configuration. */
    fun refreshGrid() {
        if (width > 0 && height > 0) {
            recomputeGrid(width, height)
            viewportListener?.invoke(grid.cols, grid.rows)
            invalidate()
        }
    }

    private fun recomputeGrid(w: Int, h: Int) {
        grid.recompute(
            w, h, resources.displayMetrics.density,
            resources.configuration.isScreenRound, textScale, painter.paint
        )
    }

    override fun onDraw(canvas: Canvas) {
        painter.draw(
            canvas, width, height, screen, colors, highContrast,
            scanlinesEnabled, input.selectionEnabled, input.selectedIndex, grid
        )
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean =
        input.onGenericMotion(event) || super.onGenericMotionEvent(event)

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean =
        input.onKeyDown(keyCode) || super.onKeyDown(keyCode, event)

    override fun onTouchEvent(event: MotionEvent): Boolean =
        input.onTouch(event, grid) || super.onTouchEvent(event)

    /** The grid currently used, exposed for tests and diagnostics. */
    fun currentGrid(): Pair<Int, Int> = grid.cols to grid.rows

    /** Layout metrics [cellW, lineH, marginX, marginY] for diagnostics. */
    fun metrics(): FloatArray = floatArrayOf(grid.cellW, grid.lineH, grid.marginX, grid.marginY)
}
