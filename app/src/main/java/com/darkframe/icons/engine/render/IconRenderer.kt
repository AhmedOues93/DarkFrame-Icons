package com.darkframe.icons.engine.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.annotation.WorkerThread
import com.darkframe.icons.engine.domain.ColorMatrices
import com.darkframe.icons.engine.domain.GlyphMode
import com.darkframe.icons.engine.domain.IconNormalizer
import com.darkframe.icons.engine.domain.IconStyle
import com.darkframe.icons.engine.domain.SourceClassifier
import com.darkframe.icons.engine.domain.SourceShape
import com.darkframe.icons.engine.domain.SurfaceFinish
import kotlin.math.abs

/**
 * Draws one DarkFrame icon.
 *
 * All composition lives here: no Activity, Fragment, View or Adapter in DarkFrame draws an icon.
 * That boundary is what makes the same icon identical in the browser grid, in a pinned shortcut and
 * in an exported PNG — they all come through this one method.
 *
 * The renderer is stateless apart from reusable `Paint` objects, and is **not** thread-safe: the
 * engine holds one instance per worker rather than sharing one across threads.
 */
class IconRenderer {

    private val containerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glyphPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isFilterBitmap = true
        isDither = true
    }
    private val keylinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val monogramPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }

    // Reused across renders: the browser resolves hundreds of icons in a burst, and allocating a
    // Path and two Rects per icon is pure garbage-collector pressure on a scroll.
    private val clipPath = Path()
    private val sourceRect = Rect()
    private val destRect = RectF()
    private val containerRect = RectF()

    /**
     * @param source rasterised artwork, or null to fall back to [monogramText].
     * @param sizePx edge length of the square result.
     */
    @WorkerThread
    fun render(
        style: IconStyle,
        source: SourceArtwork?,
        sizePx: Int,
        monogramText: String,
    ): Bitmap {
        require(sizePx > 0) { "sizePx must be positive" }
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val edge = sizePx.toFloat()
        val radius = edge * style.cornerRadiusRatio

        drawContainer(canvas, style, edge, radius)
        if (source != null) {
            drawGlyph(canvas, style, source, sizePx)
        } else {
            drawMonogram(canvas, style, monogramText, edge)
        }
        drawKeyline(canvas, style, edge, radius)
        return bitmap
    }

    private fun drawContainer(canvas: Canvas, style: IconStyle, edge: Float, radius: Float) {
        containerPaint.reset()
        containerPaint.isAntiAlias = true
        containerPaint.style = Paint.Style.FILL

        when (style.finish) {
            SurfaceFinish.FLAT, SurfaceFinish.GLASS -> {
                containerPaint.shader = null
                containerPaint.color = style.containerColor.toInt()
            }
            SurfaceFinish.METALLIC -> {
                // A single two-stop sweep on the 135 degree diagonal. Deliberately the whole of
                // the metallic treatment: stacked gradients and specular streaks are what make
                // "metal" effects look cheap, and a narrow neutral ramp reads as brushed graphite.
                containerPaint.shader = LinearGradient(
                    0f, 0f, edge, edge,
                    style.containerColor.toInt(), style.containerEndColor.toInt(),
                    Shader.TileMode.CLAMP,
                )
            }
        }
        containerRect.set(0f, 0f, edge, edge)
        canvas.drawRoundRect(containerRect, radius, radius, containerPaint)

        if (style.finish == SurfaceFinish.GLASS) {
            drawGlassHighlight(canvas, edge, radius)
        }
    }

    /**
     * One top-down highlight falling off before the midpoint — the entire glass treatment.
     *
     * Restraint is the brief: a real glass surface catches light at its top edge and nowhere else,
     * and the moment a second reflection or a bottom glow is added it stops reading as a material
     * and starts reading as a 2014 skeuomorphic button.
     */
    private fun drawGlassHighlight(canvas: Canvas, edge: Float, radius: Float) {
        highlightPaint.reset()
        highlightPaint.isAntiAlias = true
        highlightPaint.shader = LinearGradient(
            0f, 0f, 0f, edge * 0.45f,
            HIGHLIGHT_TOP_COLOR, HIGHLIGHT_FADE_COLOR,
            Shader.TileMode.CLAMP,
        )
        containerRect.set(0f, 0f, edge, edge)
        canvas.drawRoundRect(containerRect, radius, radius, highlightPaint)
        highlightPaint.shader = null
    }

    private fun drawGlyph(canvas: Canvas, style: IconStyle, source: SourceArtwork, sizePx: Int) {
        val target = SourceClassifier.targetFraction(source.shape, style)
        val placement = IconNormalizer.place(
            content = source.bounds,
            sourceWidth = source.bitmap.width,
            sourceHeight = source.bitmap.height,
            canvasSize = sizePx,
            targetFraction = target,
            opticalLiftRatio = style.opticalLiftRatio,
        )

        glyphPaint.colorFilter = colorFilterFor(style, source)

        val saveCount = canvas.save()
        if (SourceClassifier.requiresCornerClip(source.shape)) {
            // Borrowed artwork that brought its own square tile is cut to DarkFrame's geometry, so
            // it reads as a deliberately inset card instead of a foreign tile inside ours.
            val innerRadius = minOf(placement.width, placement.height) *
                SourceClassifier.innerCornerRadiusRatio(style)
            destRect.set(placement.left, placement.top, placement.right, placement.bottom)
            clipPath.reset()
            clipPath.addRoundRect(destRect, innerRadius, innerRadius, Path.Direction.CW)
            canvas.clipPath(clipPath)
        }

        sourceRect.set(
            source.bounds.left.coerceIn(0, source.bitmap.width),
            source.bounds.top.coerceIn(0, source.bitmap.height),
            source.bounds.right.coerceIn(1, source.bitmap.width),
            source.bounds.bottom.coerceIn(1, source.bitmap.height),
        )
        destRect.set(placement.left, placement.top, placement.right, placement.bottom)
        if (sourceRect.width() <= 0 || sourceRect.height() <= 0) {
            canvas.drawBitmap(source.bitmap, null, destRect, glyphPaint)
        } else {
            canvas.drawBitmap(source.bitmap, sourceRect, destRect, glyphPaint)
        }
        canvas.restoreToCount(saveCount)
        glyphPaint.colorFilter = null
    }

    private fun colorFilterFor(style: IconStyle, source: SourceArtwork): ColorMatrixColorFilter? {
        val matrix = when {
            style.glyphMode == GlyphMode.TINT && source.isMask ->
                // A monochrome layer carries no colour information, so its luma must be discarded
                // rather than multiplied into the tint — otherwise a mask authored in mid-grey
                // comes out as a dim glyph.
                ColorMatrices.flatTint(style.glyphTint)
            style.glyphMode == GlyphMode.TINT ->
                ColorMatrices.tintToLuma(style.glyphTint)
            abs(style.glyphSaturation - 1f) > 0.001f ->
                ColorMatrices.saturation(style.glyphSaturation)
            else -> return null
        }
        return ColorMatrixColorFilter(ColorMatrix(matrix))
    }

    /**
     * Last-resort glyph for an app whose own icon could not be loaded. Styled like everything else
     * so a broken install degrades to a plain-looking tile rather than a visible hole in the grid.
     */
    private fun drawMonogram(canvas: Canvas, style: IconStyle, text: String, edge: Float) {
        if (text.isEmpty()) return
        monogramPaint.color = monogramColor(style).toInt()
        monogramPaint.textSize = edge * if (text.length > 1) 0.30f else 0.40f
        val metrics = monogramPaint.fontMetrics
        val baseline = edge / 2f - (metrics.ascent + metrics.descent) / 2f -
            edge * style.opticalLiftRatio
        canvas.drawText(text, edge / 2f, baseline, monogramPaint)
    }

    /** Colour-preserving styles have no glyph tint of their own, so one is derived. */
    private fun monogramColor(style: IconStyle): Long = when (style.glyphMode) {
        GlyphMode.TINT -> style.glyphTint
        GlyphMode.PRESERVE ->
            if (ColorMatrices.relativeLuminance(style.containerColor) > 0.5f) {
                DERIVED_DARK_GLYPH
            } else {
                DERIVED_LIGHT_GLYPH
            }
    }

    private fun drawKeyline(canvas: Canvas, style: IconStyle, edge: Float, radius: Float) {
        if (style.keylineWidthRatio <= 0f) return
        if ((style.keylineColor ushr 24) == 0L) return
        val width = edge * style.keylineWidthRatio
        keylinePaint.color = style.keylineColor.toInt()
        keylinePaint.strokeWidth = width
        // Inset by half the stroke so the keyline sits fully inside the bitmap; stroking on the
        // exact edge clips its outer half and halves its apparent weight.
        val inset = width / 2f
        containerRect.set(inset, inset, edge - inset, edge - inset)
        canvas.drawRoundRect(containerRect, radius - inset, radius - inset, keylinePaint)
    }

    private companion object {
        const val HIGHLIGHT_TOP_COLOR = 0x26FFFFFF
        const val HIGHLIGHT_FADE_COLOR = 0x00FFFFFF
        const val DERIVED_LIGHT_GLYPH = 0xFFF2F1EEL
        const val DERIVED_DARK_GLYPH = 0xFF23262BL
    }
}
