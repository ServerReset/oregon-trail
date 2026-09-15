package com.oregontrail.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import com.oregontrail.engine.Palette
import com.oregontrail.engine.Screen

/** Draws a terminal [Screen] onto a canvas: glyphs, selection, scanlines and vignette. */
class TerminalPainter {

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.MONOSPACE
        isSubpixelText = true
    }
    private val highlightPaint = Paint().apply { color = 0x887CFF7C.toInt() }
    private val scanPaint = Paint().apply { color = Color.argb(30, 0, 0, 0) }
    private val vignettePaint = Paint()
    private var vignette: Shader? = null
    private val glowRadius = 2.5f

    fun resize(w: Int, h: Int) {
        vignette = RadialGradient(
            w / 2f, h / 2f, maxOf(w, h) * 0.75f,
            intArrayOf(0x00000000, 0x22000000, 0x66000000),
            floatArrayOf(0f, 0.7f, 1f),
            Shader.TileMode.CLAMP
        )
    }

    fun draw(
        canvas: Canvas,
        width: Int,
        height: Int,
        screen: Screen?,
        colors: ThemeColors,
        highContrast: Boolean,
        scanlines: Boolean,
        selectionEnabled: Boolean,
        selectedIndex: Int,
        grid: TerminalGrid
    ) {
        val bg = if (screen != null) colors.ambientBackground(screen.ambient, highContrast)
        else colors.defaultBackground
        canvas.drawColor(bg)
        if (screen == null) return

        val highlight = if (selectionEnabled) screen.hotspots.getOrNull(selectedIndex) else null
        if (highlight != null) {
            val accent = colors.foreground(Palette.BRIGHT_GREEN, false)
            highlightPaint.color = Color.argb(140, Color.red(accent), Color.green(accent), Color.blue(accent))
            canvas.drawRect(
                grid.marginX + highlight.x0 * grid.cellW - 1f,
                grid.marginY + highlight.y0 * grid.lineH,
                grid.marginX + (highlight.x1 + 1) * grid.cellW + 1f,
                grid.marginY + (highlight.y1 + 1) * grid.lineH,
                highlightPaint
            )
        }

        for (y in 0 until minOf(screen.height, grid.rows)) {
            for (x in 0 until minOf(screen.width, grid.cols)) {
                val cell = screen.cell(x, y) ?: continue
                if (cell.ch == ' ') continue
                val selected = highlight != null && x in highlight.x0..highlight.x1 &&
                    y in highlight.y0..highlight.y1
                paint.color = if (selected) bg else colors.foreground(cell.fg, highContrast)
                paint.isFakeBoldText = cell.bold || colors.isBoldDefault(cell.fg)
                // A soft phosphor bloom so the display reads as a CRT.
                if (colors.dark && !selected) {
                    paint.setShadowLayer(
                        glowRadius, 0f, 0f, (paint.color and 0x00FFFFFF) or 0x55000000
                    )
                } else {
                    paint.clearShadowLayer()
                }
                canvas.drawText(
                    cell.ch.toString(),
                    grid.marginX + x * grid.cellW,
                    grid.marginY + y * grid.lineH + grid.baseline,
                    paint
                )
            }
        }

        if (scanlines && colors.dark) {
            var yy = 0f
            while (yy < height) {
                canvas.drawRect(0f, yy, width.toFloat(), yy + 1f, scanPaint)
                yy += 3f
            }
        }
        if (colors.dark) {
            vignette?.let {
                vignettePaint.shader = it
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), vignettePaint)
            }
        }
    }
}
