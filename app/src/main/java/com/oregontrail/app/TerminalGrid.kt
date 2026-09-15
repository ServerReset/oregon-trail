package com.oregontrail.app

import android.graphics.Paint
import kotlin.math.roundToInt

/**
 * The character grid for a terminal view: how many columns and rows fit, the
 * cell size and the margins. Recalculated on every size or scale change.
 */
class TerminalGrid {
    var cols = 40
    var rows = 30
    var cellW = 0f
    var lineH = 0f
    var baseline = 0f
    var marginX = 0f
    var marginY = 0f

    /** Recomputes the grid for a [w]x[h] view. Returns cols to rows. */
    fun recompute(w: Int, h: Int, density: Float, round: Boolean, textScale: Float, paint: Paint) {
        val basePad = 3f * density
        // On a round watch, keep all text inside the inscribed square.
        val pad = if (round) maxOf(basePad, 0.16f * minOf(w, h)) else basePad
        val contentW = (w - 2 * pad).coerceAtLeast(1f)
        val contentH = (if (round) (h - 2 * pad) else h.toFloat()).coerceAtLeast(1f)

        // Target a readable character width, then derive the column count. Very
        // wide screens get more columns; tiny ones (watches, cover displays) drop
        // to fewer so glyphs stay legible.
        val desiredCellW = 9f * density * textScale
        val c = (contentW / desiredCellW).roundToInt().coerceIn(16, 120)

        paint.textSize = 100f
        val advanceRatio = (paint.measureText("M") / 100f).coerceAtLeast(0.3f)
        cellW = contentW / c
        paint.textSize = cellW / advanceRatio
        val fm = paint.fontMetrics
        lineH = fm.descent - fm.ascent
        baseline = -fm.ascent

        val r = (contentH / lineH).toInt().coerceIn(10, 80)
        cols = c
        rows = r
        marginX = pad
        marginY = ((contentH - r * lineH) / 2f).coerceAtLeast(0f) + (if (round) pad else 0f)
    }
}
