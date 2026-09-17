package com.oregontrail.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import com.oregontrail.engine.Palette
import com.oregontrail.engine.Screen

/** Draws a terminal [Screen] onto a canvas: glyphs, selection, scanlines, CRT glass. */
class TerminalPainter {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.MONOSPACE; isSubpixelText = true
    }
    private val highlightPaint = Paint().apply { color = 0x887CFF7C.toInt() }
    private val scanPaint = Paint().apply { color = Color.argb(30, 0, 0, 0) }
    private val grillePaint = Paint().apply {
        color = Color.argb(13, 0, 0, 0); strokeWidth = 1.5f
    }
    private val sweepPaint = Paint(); private val vignettePaint = Paint()
    private val vignettePath = Path(); private val glyph = CharArray(1)
    private var vignette: Shader? = null
    private val glowRadius = 2.5f; private val bloomRadius = 5f; private val sweepFrames = 12
    private var frameCounter = 0; private var sweepFrame = -1

    fun resize(w: Int, h: Int) {
        vignette = RadialGradient(
            w / 2f, h / 2f, maxOf(w, h) * 0.72f,
            intArrayOf(0x00000000, 0x2B000000, 0x7A000000),
            floatArrayOf(0f, 0.6f, 1f),
            Shader.TileMode.CLAMP
        )
        // Rounded-rect vignette overlay: softens the bezel corners, never clips text.
        val r = minOf(w, h) * 0.06f
        vignettePath.reset()
        vignettePath.addRoundRect(0f, 0f, w.toFloat(), h.toFloat(), r, r, Path.Direction.CW)
    }

    /** Starts the short power-on scanline sweep. Content is never masked. */
    fun powerOn() { sweepFrame = 0 }

    /** True while the power-on sweep still needs frames (caller should repaint). */
    val isAnimating: Boolean get() = sweepFrame in 0 until sweepFrames

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
        frameCounter++
        val bg = if (screen != null) colors.ambientBackground(screen.ambient, highContrast)
        else colors.defaultBackground
        canvas.drawColor(bg)
        if (screen == null) { if (isAnimating) sweepFrame++; return }

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

        val dark = colors.dark
        val rows = minOf(screen.height, grid.rows); val cols = minOf(screen.width, grid.cols)
        for (y in 0 until rows) {
            // Faint per-line brightness shimmer (0..7 alpha); glyphs stay >= 248/255.
            val jitter = (y * 7 + frameCounter) and 7
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
                val tx = grid.marginX + x * grid.cellW
                val ty = grid.marginY + y * grid.lineH + grid.baseline
                canvas.drawText(glyph, 0, 1, tx, ty, paint)
                // Extra phosphor bloom on bold glyphs only, so legibility is untouched.
                if (dark && cell.bold && !selected) {
                    paint.alpha = 40
                    paint.setShadowLayer(
                        bloomRadius, 0f, 0f, (paint.color and 0x00FFFFFF) or 0x40000000
                    )
                    canvas.drawText(glyph, 0, 1, tx, ty, paint)
                }
            }
        }

        if (dark) {
            // Aperture-grille: a barely-visible dark line every third column.
            if (scanlines) {
                grillePaint.strokeWidth = (grid.cellW * 0.08f).coerceAtLeast(1f)
                val gxEnd = grid.marginX + cols * grid.cellW
                val gy1 = grid.marginY + rows * grid.lineH
                var gx = grid.marginX
                while (gx < gxEnd) {
                    canvas.drawLine(gx, grid.marginY, gx, gy1, grillePaint)
                    gx += grid.cellW * 3f
                }
            }
            var yy = 0f
            while (yy < height) {
                canvas.drawRect(0f, yy, width.toFloat(), yy + 1f, scanPaint)
                yy += 3f
            }
            vignette?.let {
                vignettePaint.shader = it
                canvas.drawPath(vignettePath, vignettePaint)
            }
        }

        if (isAnimating) {
            if (dark) {
                val t = sweepFrame / sweepFrames.toFloat()
                sweepPaint.color =
                    Color.argb((150 * (1f - t)).toInt().coerceIn(0, 255), 190, 255, 210)
                val y = t * height
                canvas.drawRect(0f, y - 2f, width.toFloat(), y + 2f, sweepPaint)
            }
            sweepFrame++
        }
    }
}
