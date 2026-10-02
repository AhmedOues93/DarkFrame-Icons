package com.darkframe.icons.engine.wallpaper

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import androidx.annotation.WorkerThread
import kotlin.math.max
import kotlin.math.min

/**
 * Draws a [WallpaperSpec] at exactly the size asked for.
 *
 * Nothing here is sampled from a file. A Fold8 alone needs two very different panels, and shipping
 * bitmaps for every panel of every supported device would add tens of megabytes to deliver flat
 * fields and fine gradients — which are exactly the images that band badly once compressed. Drawn
 * output is sharp at any size and costs a few kilobytes of code.
 *
 * Not thread-safe: reuses its Paint objects, so each worker gets its own instance.
 */
class WallpaperRenderer {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val path = Path()

    @WorkerThread
    fun render(spec: WallpaperSpec, widthPx: Int, heightPx: Int): Bitmap {
        require(widthPx > 0 && heightPx > 0) { "wallpaper size must be positive" }
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(spec.base.toInt())

        val w = widthPx.toFloat()
        val h = heightPx.toFloat()
        when (spec.pattern) {
            WallpaperPattern.SOLID -> Unit
            WallpaperPattern.GLOW -> drawGlow(canvas, spec, w, h)
            WallpaperPattern.WEAVE -> drawWeave(canvas, spec, w, h)
            WallpaperPattern.BRUSHED -> drawBrushed(canvas, spec, w, h)
            WallpaperPattern.PANES -> drawPanes(canvas, spec, w, h)
            WallpaperPattern.FRAME -> drawFrame(canvas, spec, w, h)
            WallpaperPattern.ARCS -> drawArcs(canvas, spec, w, h)
            WallpaperPattern.SEAM -> drawSeam(canvas, spec, w, h)
        }
        return bitmap
    }

    private fun alphaOf(spec: WallpaperSpec, factor: Float): Int =
        (255 * spec.intensity * factor).toInt().coerceIn(0, 255)

    /** One off-centre light source, high and slightly left, so the screen has a direction. */
    private fun drawGlow(canvas: Canvas, spec: WallpaperSpec, w: Float, h: Float) {
        val radius = max(w, h) * 0.85f
        paint.reset()
        paint.isAntiAlias = true
        paint.shader = RadialGradient(
            w * 0.32f, h * 0.22f, radius,
            intArrayOf(
                withAlpha(spec.accent, alphaOf(spec, 1f)),
                withAlpha(spec.accent, 0),
            ),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null
    }

    /** Fine diagonal weave. Spacing scales with the screen so it reads the same on any panel. */
    private fun drawWeave(canvas: Canvas, spec: WallpaperSpec, w: Float, h: Float) {
        val step = max(6f, min(w, h) / 90f)
        paint.reset()
        paint.isAntiAlias = true
        paint.strokeWidth = step * 0.42f
        paint.color = withAlpha(spec.accent, alphaOf(spec, 0.55f))
        var x = -h
        while (x < w + h) {
            canvas.drawLine(x, 0f, x + h, h, paint)
            x += step * 2
        }
        paint.color = withAlpha(spec.accent, alphaOf(spec, 0.3f))
        x = -h
        while (x < w + h) {
            canvas.drawLine(x + h, 0f, x, h, paint)
            x += step * 2
        }
    }

    /** Brushed metal: many low-contrast strokes along the long axis. */
    private fun drawBrushed(canvas: Canvas, spec: WallpaperSpec, w: Float, h: Float) {
        paint.reset()
        paint.isAntiAlias = false
        paint.strokeWidth = 1f
        // Deterministic rather than random: the same wallpaper must redraw identically when the
        // screen rotates or the device unfolds.
        var seed = 0x9E3779B9.toInt()
        val lines = (h / 3f).toInt().coerceIn(80, 900)
        for (i in 0 until lines) {
            seed = seed * 1664525 + 1013904223
            val t = ((seed ushr 8) and 0xFFFF) / 65535f
            val y = h * (i / lines.toFloat())
            paint.color = withAlpha(spec.accent, alphaOf(spec, 0.08f + 0.22f * t))
            canvas.drawLine(0f, y, w, y + (t - 0.5f) * 6f, paint)
        }
        paint.isAntiAlias = true
        paint.shader = LinearGradient(
            0f, 0f, w, h,
            withAlpha(spec.accent, alphaOf(spec, 0.35f)),
            withAlpha(spec.base, 0),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null
    }

    /** Two broad translucent panes crossing, each with one lit edge. */
    private fun drawPanes(canvas: Canvas, spec: WallpaperSpec, w: Float, h: Float) {
        paint.reset()
        paint.isAntiAlias = true
        paint.color = withAlpha(spec.accent, alphaOf(spec, 0.5f))
        path.reset()
        path.moveTo(-w * 0.1f, h * 0.18f)
        path.lineTo(w * 1.1f, -h * 0.05f)
        path.lineTo(w * 1.1f, h * 0.42f)
        path.lineTo(-w * 0.1f, h * 0.66f)
        path.close()
        canvas.drawPath(path, paint)

        paint.color = withAlpha(spec.accent, alphaOf(spec, 0.3f))
        path.reset()
        path.moveTo(-w * 0.1f, h * 0.58f)
        path.lineTo(w * 1.1f, h * 0.36f)
        path.lineTo(w * 1.1f, h * 1.05f)
        path.lineTo(-w * 0.1f, h * 1.05f)
        path.close()
        canvas.drawPath(path, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = max(1f, w / 520f)
        paint.color = withAlpha(0xFFFFFFFFL, alphaOf(spec, 0.26f))
        canvas.drawLine(-w * 0.1f, h * 0.18f, w * 1.1f, -h * 0.05f, paint)
        paint.style = Paint.Style.FILL
    }

    /** A single hairline frame, inset. Minimal means one mark, not none. */
    private fun drawFrame(canvas: Canvas, spec: WallpaperSpec, w: Float, h: Float) {
        val inset = min(w, h) * 0.085f
        paint.reset()
        paint.isAntiAlias = true
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = max(1f, min(w, h) / 420f)
        paint.color = withAlpha(spec.accent, alphaOf(spec, 1f))
        rect.set(inset, inset, w - inset, h - inset)
        canvas.drawRoundRect(rect, inset * 0.35f, inset * 0.35f, paint)
        paint.style = Paint.Style.FILL
    }

    /** Overlapping wide arcs, low contrast. */
    private fun drawArcs(canvas: Canvas, spec: WallpaperSpec, w: Float, h: Float) {
        paint.reset()
        paint.isAntiAlias = true
        paint.style = Paint.Style.STROKE
        val radii = floatArrayOf(0.55f, 0.78f, 1.02f, 1.3f)
        radii.forEachIndexed { index, factor ->
            paint.strokeWidth = max(1.5f, min(w, h) * 0.012f)
            paint.color = withAlpha(spec.accent, alphaOf(spec, 0.75f - index * 0.15f))
            val r = max(w, h) * factor
            rect.set(w * 0.78f - r, h * 0.9f - r, w * 0.78f + r, h * 0.9f + r)
            canvas.drawArc(rect, 182f, 96f, false, paint)
        }
        paint.style = Paint.Style.FILL
    }

    /**
     * A vertical seam with a tonal shift either side.
     *
     * Placed at the middle of the *rendered* surface, so on a Fold's inner screen it lands on the
     * hinge and on the cover screen it reads as a centre line rather than a misplaced artefact.
     */
    private fun drawSeam(canvas: Canvas, spec: WallpaperSpec, w: Float, h: Float) {
        paint.reset()
        paint.isAntiAlias = true
        paint.shader = LinearGradient(
            0f, 0f, w, 0f,
            intArrayOf(
                withAlpha(spec.accent, alphaOf(spec, 0.8f)),
                withAlpha(spec.accent, 0),
                withAlpha(spec.accent, alphaOf(spec, 0.8f)),
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null
        paint.strokeWidth = max(1f, w / 900f)
        paint.color = withAlpha(0xFFFFFFFFL, alphaOf(spec, 0.14f))
        canvas.drawLine(w / 2f, 0f, w / 2f, h, paint)
    }

    private fun withAlpha(argb: Long, alpha: Int): Int =
        ((alpha.toLong() and 0xFF) shl 24 or (argb and 0xFFFFFF)).toInt()
}
