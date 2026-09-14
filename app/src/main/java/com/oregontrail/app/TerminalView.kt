package com.oregontrail.app

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import com.oregontrail.engine.Palette
import com.oregontrail.engine.Screen
import kotlin.math.roundToInt

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

    var screen: Screen? = null
        set(value) {
            field = value
            val count = value?.hotspots?.size ?: 0
            selectedIndex = if (count == 0) 0 else selectedIndex.coerceIn(0, count - 1)
            invalidate()
        }

    var tapListener: ((String) -> Unit)? = null

    /** Called whenever the available character grid changes. */
    var viewportListener: ((cols: Int, rows: Int) -> Unit)? = null

    /**
     * Enables the watch-style selection cursor: the rotary bezel/crown and
     * D-pad move a highlighted target, and a tap or the center button
     * activates it. Off on phones so the existing direct-tap UX is unchanged.
     */
    var selectionEnabled: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            invalidate()
        }

    private var selectedIndex = 0
    private val highlightPaint = Paint().apply { color = 0x887CFF7C.toInt() }
    private val vignettePaint = Paint()
    private var vignette: Shader? = null

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.MONOSPACE
        isSubpixelText = true
    }
    private val scanPaint = Paint().apply { color = Color.argb(30, 0, 0, 0) }

    private var cols = 40
    private var rows = 30
    private var cellW = 0f
    private var lineH = 0f
    private var baseline = 0f
    private var marginX = 0f
    private var marginY = 0f

    var scanlinesEnabled = true

    init {
        isFocusable = true
        isFocusableInTouchMode = true
    }

    /** Moves the selection cursor (rotary bezel, D-pad or trackball). */
    fun moveSelection(delta: Int) {
        val count = screen?.hotspots?.size ?: 0
        if (count == 0) return
        selectedIndex = ((selectedIndex + delta) % count + count) % count
        performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
        invalidate()
    }

    private fun activateSelected() {
        val id = screen?.hotspots?.getOrNull(selectedIndex)?.id ?: return
        performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
        tapListener?.invoke(id)
    }

    /** Held controls (store steppers, hunting D-pad, raft steering) auto-repeat. */
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

    /** Accessibility scale: <1 fits more columns, >1 makes glyphs larger. */
    var textScale: Float = 1f
        set(value) {
            val clamped = value.coerceIn(0.6f, 1.6f)
            if (field == clamped) return
            field = clamped
            if (width > 0 && height > 0) {
                recomputeGrid(width, height)
                viewportListener?.invoke(cols, rows)
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

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w == 0 || h == 0) return
        vignette = RadialGradient(
            w / 2f, h / 2f, maxOf(w, h) * 0.75f,
            intArrayOf(0x00000000, 0x22000000, 0x66000000),
            floatArrayOf(0f, 0.7f, 1f),
            Shader.TileMode.CLAMP
        )
        recomputeGrid(w, h)
        viewportListener?.invoke(cols, rows)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Density and font scale can change without the view size changing
        // (display settings, accessibility, emulator overrides).
        refreshGrid()
    }

    /** Recomputes the character grid from the current size and configuration. */
    fun refreshGrid() {
        if (width > 0 && height > 0) {
            recomputeGrid(width, height)
            viewportListener?.invoke(cols, rows)
            invalidate()
        }
    }

    private fun recomputeGrid(w: Int, h: Int) {
        val density = resources.displayMetrics.density
        val round = resources.configuration.isScreenRound
        val basePad = 3f * density
        // On a round watch, keep all text inside the inscribed square.
        val pad = if (round) {
            maxOf(basePad, 0.16f * minOf(w, h))
        } else {
            basePad
        }
        val contentW = (w - 2 * pad).coerceAtLeast(1f)
        val contentH = (if (round) (h - 2 * pad) else h.toFloat()).coerceAtLeast(1f)

        // Target a readable character width, then derive the column count.
        // Very wide (often landscape) screens get more columns, which keeps the
        // glyphs a comfortable size while still yielding enough rows. Tiny
        // screens (watches, cover displays) drop to fewer columns so glyphs stay
        // legible.
        val desiredCellW = 9f * density * textScale
        val c = (contentW / desiredCellW).roundToInt().coerceIn(16, 120)

        // Measure the monospace advance ratio for this typeface.
        paint.textSize = 100f
        val advanceRatio = (paint.measureText("M") / 100f).coerceAtLeast(0.3f)
        cellW = contentW / c
        val textSize = cellW / advanceRatio
        paint.textSize = textSize
        val fm = paint.fontMetrics
        lineH = (fm.descent - fm.ascent)
        baseline = -fm.ascent

        val r = (contentH / lineH).toInt().coerceIn(10, 80)
        cols = c
        rows = r
        marginX = pad
        marginY = ((contentH - r * lineH) / 2f).coerceAtLeast(0f) + (if (round) pad else 0f)
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(RetroPalette.BACKGROUND)
        val s = screen ?: return

        val highlight = if (selectionEnabled) s.hotspots.getOrNull(selectedIndex) else null
        if (highlight != null) {
            canvas.drawRect(
                marginX + highlight.x0 * cellW - 1f,
                marginY + highlight.y0 * lineH,
                marginX + (highlight.x1 + 1) * cellW + 1f,
                marginY + (highlight.y1 + 1) * lineH,
                highlightPaint
            )
        }

        for (y in 0 until minOf(s.height, rows)) {
            for (x in 0 until minOf(s.width, cols)) {
                val cell = s.cell(x, y) ?: continue
                if (cell.ch == ' ') continue
                val selected = highlight != null && x in highlight.x0..highlight.x1 &&
                    y in highlight.y0..highlight.y1
                paint.color = if (selected) RetroPalette.BACKGROUND else RetroPalette.fg(cell.fg, highContrast)
                paint.isFakeBoldText = cell.bold || RetroPalette.isBoldDefault(cell.fg)
                val px = marginX + x * cellW
                val py = marginY + y * lineH + baseline
                canvas.drawText(cell.ch.toString(), px, py, paint)
            }
        }

        if (scanlinesEnabled) {
            var yy = 0f
            while (yy < height) {
                canvas.drawRect(0f, yy, width.toFloat(), yy + 1f, scanPaint)
                yy += 3f
            }
        }

        vignette?.let {
            vignettePaint.shader = it
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), vignettePaint)
        }
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (event.isFromSource(InputDevice.SOURCE_ROTARY_ENCODER)) {
            val scroll = event.getAxisValue(MotionEvent.AXIS_SCROLL)
            if (scroll != 0f) {
                moveSelection(if (scroll > 0f) 1 else -1)
                return true
            }
        }
        return super.onGenericMotionEvent(event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
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
        return super.onKeyDown(keyCode, event)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val cx = ((event.x - marginX) / cellW).toInt()
                val cy = ((event.y - marginY) / lineH).toInt()
                val id = screen?.hotspotAt(cx, cy)
                if (BuildConfig.DEBUG) {
                    android.util.Log.i(
                        "OTS", "touch x=${event.x} y=${event.y} cell=($cx,$cy) id=$id"
                    )
                }
                if (id != null) {
                    screen?.hotspots?.indexOfFirst { it.id == id && it.contains(cx, cy) }
                        ?.takeIf { it >= 0 }?.let { selectedIndex = it }
                    performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
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
        return super.onTouchEvent(event)
    }

    /** The grid currently used, exposed for tests and diagnostics. */
    fun currentGrid(): Pair<Int, Int> = cols to rows

    /** Layout metrics [cellW, lineH, marginX, marginY] for diagnostics. */
    fun metrics(): FloatArray = floatArrayOf(cellW, lineH, marginX, marginY)
}
