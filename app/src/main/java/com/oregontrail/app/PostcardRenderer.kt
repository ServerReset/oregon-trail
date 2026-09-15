package com.oregontrail.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import com.oregontrail.engine.Palette
import kotlin.math.max

/**
 * Draws an engine postcard (a list of ASCII rows) into a shareable bitmap,
 * using the active terminal colours so the image matches the game.
 */
object PostcardRenderer {

    fun render(rows: List<String>, colors: ThemeColors, density: Float): Bitmap {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.MONOSPACE
            textSize = 13f * density
            color = colors.foreground(Palette.GREEN, false)
        }
        val fm = paint.fontMetrics
        val lineH = fm.descent - fm.ascent
        val charW = paint.measureText("M")
        val pad = 18f * density
        val width = ((rows.maxOfOrNull { it.length } ?: 1) * charW + 2 * pad).toInt().coerceAtLeast(1)
        val height = (rows.size * lineH + 2 * pad).toInt().coerceAtLeast(1)

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(colors.defaultBackground)
        rows.forEachIndexed { i, row ->
            val baseline = pad + (-fm.ascent) + i * lineH
            canvas.drawText(row, pad, baseline, paint)
        }
        val border = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = max(1f, density)
            color = paint.color
        }
        canvas.drawRect(pad / 2, pad / 2, width - pad / 2, height - pad / 2, border)
        return bitmap
    }
}
