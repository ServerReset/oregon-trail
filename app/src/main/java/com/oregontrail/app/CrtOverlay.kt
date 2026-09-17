package com.oregontrail.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader

/** CRT "glass": scanlines, grille/RGB mask, rolling bar, flicker, static,
 *  rounded vignette and the power-on sweep. Scales with the CRT level. */
internal object CrtOverlay {

    private const val SWEEP_FRAMES = 13
    private val scanPaint = Paint().apply { color = Color.argb(30, 0, 0, 0) }
    private val grillePaint = Paint().apply { color = Color.argb(16, 0, 0, 0) }
    private val triadR = Paint().apply { color = Color.argb(13, 255, 60, 60) }
    private val triadG = Paint().apply { color = Color.argb(13, 60, 255, 60) }
    private val triadB = Paint().apply { color = Color.argb(13, 70, 120, 255) }
    private val rollPaint = Paint()
    private val flickerPaint = Paint().apply { color = Color.argb(8, 0, 0, 0) }
    private val noisePaint = Paint().apply { color = Color.argb(22, 200, 255, 200) }
    private val sweepPaint = Paint()
    private val vignettePaint = Paint()
    private val vignettePath = Path()

    private var rollShader: Shader? = null
    private var vignette: Shader? = null
    private var sweepFrame = -1
    private var rng = 12345
    fun resize(w: Int, h: Int) {
        val band = (h * 0.16f).coerceAtLeast(8f)
        rollShader = LinearGradient(
            0f, 0f, 0f, band,
            intArrayOf(0x00000000, 0x1EBeffD2, 0x00000000),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP
        )
        vignette = RadialGradient(
            w / 2f, h / 2f, maxOf(w, h) * 0.72f,
            intArrayOf(0x00000000, 0x2B000000, 0x7A000000),
            floatArrayOf(0f, 0.6f, 1f), Shader.TileMode.CLAMP
        )
        val r = minOf(w, h) * 0.06f
        vignettePath.reset()
        vignettePath.addRoundRect(0f, 0f, w.toFloat(), h.toFloat(), r, r, Path.Direction.CW)
    }

    fun powerOn() { sweepFrame = 0 }

    val isAnimating: Boolean get() = sweepFrame in 0 until SWEEP_FRAMES

    fun draw(
        canvas: Canvas,
        width: Int,
        height: Int,
        dark: Boolean,
        scanlines: Boolean,
        level: Int,
        frame: Int,
        grid: TerminalGrid,
        cols: Int,
        rows: Int
    ) {
        if (dark && level >= 1) {
            if (scanlines) scanlines(canvas, width, height)
            mask(canvas, level, grid, cols, rows)
            roll(canvas, width, height, frame)
        }
        if (dark && level >= 2) {
            flicker(canvas, width, height, frame)
            staticNoise(canvas, width, height)
        }
        if (dark) {
            vignette?.let {
                vignettePaint.shader = it
                canvas.drawPath(vignettePath, vignettePaint)
            }
        }
        if (isAnimating) {
            if (dark) {
                val t = sweepFrame / SWEEP_FRAMES.toFloat()
                sweepPaint.color =
                    Color.argb((150 * (1f - t)).toInt().coerceIn(0, 255), 190, 255, 210)
                val y = t * height
                canvas.drawRect(0f, y - 2f, width.toFloat(), y + 2f, sweepPaint)
            }
            sweepFrame++
        }
    }

    private fun scanlines(canvas: Canvas, width: Int, height: Int) {
        var yy = 0f
        while (yy < height) {
            canvas.drawRect(0f, yy, width.toFloat(), yy + 1f, scanPaint)
            yy += 3f
        }
    }

    /** Level 1: dark grille lines. Level 2: a coloured RGB triad mask. */
    private fun mask(canvas: Canvas, level: Int, grid: TerminalGrid, cols: Int, rows: Int) {
        val x0 = grid.marginX
        val x1 = grid.marginX + cols * grid.cellW
        val y0 = grid.marginY
        val y1 = grid.marginY + rows * grid.lineH
        if (level >= 2) {
            var gx = x0
            while (gx < x1) {
                canvas.drawLine(gx, y0, gx, y1, triadR)
                canvas.drawLine(gx + 1f, y0, gx + 1f, y1, triadG)
                canvas.drawLine(gx + 2f, y0, gx + 2f, y1, triadB)
                gx += 3f
            }
        } else {
            grillePaint.strokeWidth = (grid.cellW * 0.08f).coerceAtLeast(1f)
            var gx = x0
            while (gx < x1) {
                canvas.drawLine(gx, y0, gx, y1, grillePaint)
                gx += grid.cellW * 3f
            }
        }
    }

    /** A slow bright band drifting down the screen. */
    private fun roll(canvas: Canvas, width: Int, height: Int, frame: Int) {
        val shader = rollShader ?: return
        val band = (height * 0.16f).coerceAtLeast(8f)
        val period = height + band * 2
        val y = ((frame * 3) % period) - band
        rollPaint.shader = shader
        canvas.drawRect(0f, y, width.toFloat(), y + band, rollPaint)
    }

    private fun flicker(canvas: Canvas, width: Int, height: Int, frame: Int) {
        flickerPaint.color = Color.argb(4 + (frame % 3) * 2, 0, 0, 0)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), flickerPaint)
    }

    private fun staticNoise(canvas: Canvas, width: Int, height: Int) {
        var i = 0
        val count = (width * height) / 14000
        while (i < count) {
            rng = rng * 1103515245 + 12345
            val x = ((rng ushr 8) % width).toFloat()
            val y = ((rng ushr 20) % height).toFloat()
            canvas.drawRect(x, y, x + 1f, y + 1f, noisePaint)
            i++
        }
    }
}
