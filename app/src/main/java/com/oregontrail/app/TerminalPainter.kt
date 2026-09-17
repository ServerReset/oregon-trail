package com.oregontrail.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.oregontrail.engine.Palette
import com.oregontrail.engine.Screen

/**
 * Draws the glyphs of a terminal [Screen] (with phosphor glow, a bold bloom and
 * subtle RGB fringing), then hands off to [CrtOverlay] for the glass effects.
 */
class TerminalPainter {

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.MONOSPACE
        isSubpixelText = true
    }
    private val highlightPaint = Paint().apply { color = 0x887CFF7C.toInt() }
    private val fringeR = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(46, 255, 70, 70) }
    private val fringeB = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(46, 70, 150, 255) }
    private val glyph = CharArray(1)
    private var frameCounter = 0

    fun resize(w: Int, h: Int) = CrtOverlay.resize(w, h)

    /** Starts the short power-on scanline sweep. Content is never masked. */
    fun powerOn() = CrtOverlay.powerOn()

    /** True while the power-on sweep still needs frames (caller should repaint). */
    val isAnimating: Boolean get() = CrtOverlay.isAnimating

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
        grid: TerminalGrid,
        crtMode: Int
    ) {
        frameCounter++
        val dark = colors.dark
        val level = if (dark) crtMode.coerceIn(0, 2) else 0
        val bg = if (screen != null) colors.ambientBackground(screen.ambient, highContrast)
        else colors.defaultBackground
        canvas.drawColor(bg)
        if (screen == null) {
            CrtOverlay.draw(canvas, width, height, dark, scanlines, level, frameCounter, grid, 0, 0)
            return
        }

        val highlight = if (selectionEnabled) screen.hotspots.getOrNull(selectedIndex) else null
        if (highlight != null) {
            val accent = colors.foreground(Palette.BRIGHT_GREEN, false)
            highlightPaint.color =
                Color.argb(140, Color.red(accent), Color.green(accent), Color.blue(accent))
            canvas.drawRect(
                grid.marginX + highlight.x0 * grid.cellW - 1f,
                grid.marginY + highlight.y0 * grid.lineH,
                grid.marginX + (highlight.x1 + 1) * grid.cellW + 1f,
                grid.marginY + (highlight.y1 + 1) * grid.lineH,
                highlightPaint
            )
        }

        val glowRadius = 2.2f + level * 0.5f
        val bloomRadius = 4.5f + level * 0.6f
        val rows = minOf(screen.height, grid.rows)
        val cols = minOf(screen.width, grid.cols)
        for (y in 0 until rows) {
            // Faint per-line brightness shimmer; glyphs never drop below ~248/255.
            val jitter = (y * 7 + frameCounter) and 7
            // Occasional one-pixel row wobble, as if the sync is drifting.
            val wobble = if (level >= 2 && (y * 31 + frameCounter / 7) % 127 < 2) {
                grid.cellW * 0.14f
            } else 0f
            for (x in 0 until cols) {
                val cell = screen.cell(x, y) ?: continue
                if (cell.ch == ' ') continue
                val selected = highlight != null && x in highlight.x0..highlight.x1 &&
                    y in highlight.y0..highlight.y1
                paint.color = if (selected) bg else colors.foreground(cell.fg, highContrast)
                if (!selected) paint.alpha = (paint.alpha - jitter).coerceIn(0, 255)
                paint.isFakeBoldText = cell.bold || colors.isBoldDefault(cell.fg)
                if (dark && !selected) {
                    paint.setShadowLayer(
                        glowRadius, 0f, 0f, (paint.color and 0x00FFFFFF) or 0x55000000
                    )
                } else {
                    paint.clearShadowLayer()
                }
                glyph[0] = cell.ch
                val tx = grid.marginX + x * grid.cellW + wobble
                val ty = grid.marginY + y * grid.lineH + grid.baseline
                // RGB fringing on high CRT, on bold glyphs only (keeps text crisp).
                if (level >= 2 && cell.bold && !selected) {
                    canvas.drawText(glyph, 0, 1, tx + 1.2f, ty, fringeR)
                    canvas.drawText(glyph, 0, 1, tx - 1.2f, ty, fringeB)
                }
                canvas.drawText(glyph, 0, 1, tx, ty, paint)
                if (dark && cell.bold && !selected) {
                    paint.alpha = 40
                    paint.setShadowLayer(
                        bloomRadius, 0f, 0f, (paint.color and 0x00FFFFFF) or 0x40000000
                    )
                    canvas.drawText(glyph, 0, 1, tx, ty, paint)
                    paint.alpha = 255
                }
            }
        }

        CrtOverlay.draw(canvas, width, height, dark, scanlines, level, frameCounter, grid, cols, rows)
    }
}
