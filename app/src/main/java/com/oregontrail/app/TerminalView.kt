package com.oregontrail.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
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
            invalidate()
        }

    var tapListener: ((String) -> Unit)? = null

    /** Called whenever the available character grid changes. */
    var viewportListener: ((cols: Int, rows: Int) -> Unit)? = null

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

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w == 0 || h == 0) return
        recomputeGrid(w, h)
        viewportListener?.invoke(cols, rows)
    }

    private fun recomputeGrid(w: Int, h: Int) {
        val density = resources.displayMetrics.density
        val pad = 3f * density
        val contentW = (w - 2 * pad).coerceAtLeast(1f)
        val contentH = h.toFloat()

        // Target a readable character width, then derive the column count.
        // Very wide (often landscape) screens get more columns, which keeps the
        // glyphs a comfortable size while still yielding enough rows.
        val desiredCellW = 9f * density
        var c = (contentW / desiredCellW).roundToInt().coerceIn(28, 110)

        // Measure the monospace advance ratio for this typeface.
        paint.textSize = 100f
        val advanceRatio = (paint.measureText("M") / 100f).coerceAtLeast(0.3f)
        cellW = contentW / c
        val textSize = cellW / advanceRatio
        paint.textSize = textSize
        val fm = paint.fontMetrics
        lineH = (fm.descent - fm.ascent)
        baseline = -fm.ascent

        var r = (contentH / lineH).toInt().coerceIn(16, 70)
        cols = c
        rows = r
        marginX = pad
        marginY = ((contentH - r * lineH) / 2f).coerceAtLeast(0f)
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(RetroPalette.BACKGROUND)
        val s = screen ?: return
        val fm = paint.fontMetrics

        for (y in 0 until minOf(s.height, rows)) {
            for (x in 0 until minOf(s.width, cols)) {
                val cell = s.cell(x, y) ?: continue
                if (cell.ch == ' ') continue
                val fg = RetroPalette.fg(cell.fg)
                paint.color = fg
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
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val cx = ((event.x - marginX) / cellW).toInt()
                val cy = ((event.y - marginY) / lineH).toInt()
                val id = screen?.hotspotAt(cx, cy)
                if (DEBUG_TOUCH) {
                    android.util.Log.i(
                        "OTS", "touch x=${event.x} y=${event.y} cell=($cx,$cy) id=$id"
                    )
                }
                if (id != null) {
                    performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                    tapListener?.invoke(id)
                    return true
                }
                return false
            }
        }
        return super.onTouchEvent(event)
    }

    /** The grid currently used, exposed for tests and diagnostics. */
    fun currentGrid(): Pair<Int, Int> = cols to rows

    /** Layout metrics [cellW, lineH, marginX, marginY] for diagnostics. */
    fun metrics(): FloatArray = floatArrayOf(cellW, lineH, marginX, marginY)

    companion object {
        const val DEBUG_TOUCH = false
    }
}
