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
import com.darkframe.icons.engine.domain.OpticalMetrics
import com.darkframe.icons.engine.domain.SourceClassifier
import com.darkframe.icons.engine.domain.SourceShape
import com.darkframe.icons.engine.domain.SurfaceFinish

/**
 * Draws one DarkFrame icon.
 *
 * All composition lives here: no Activity, Fragment, View or Adapter in DarkFrame draws an icon.
 * That boundary is what makes the same icon identical in the browser grid, in a pinned shortcut and
 * in an exported PNG — they all come through this one method.
 *
 * Two jobs, in order. The **finish** gives a collection its material identity, and each one is kept
 * to the smallest number of marks that reads as that material: one sweep for metal, one top
 * highlight and one floor edge for glass, one bloom and one bright edge for frost. The moment a
 * second reflection or a stacked gradient is added it stops reading as a surface and starts reading
 * as a 2014 skeuomorphic button. The **glyph** is then seated using the optical corrections in
 * [OpticalMetrics], which are what make artwork from unrelated apps the same apparent size and
 * weight.
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
        // Over the glyph, not under it. On Glass the content has to look like it is behind the
        // pane, and on metal the bevel is the surface's own edge — drawing either first would put
        // the material underneath its own contents.
        drawSurfaceEdges(canvas, style, edge, radius)
        drawKeyline(canvas, style, edge, radius)
        return bitmap
    }

    // ---- container ------------------------------------------------------------------------------

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
                // the metallic fill: stacked gradients and specular streaks are what make "metal"
                // effects look cheap, and a narrow neutral ramp reads as brushed graphite.
                containerPaint.shader = LinearGradient(
                    0f, 0f, edge, edge,
                    style.containerColor.toInt(), style.containerEndColor.toInt(),
                    Shader.TileMode.CLAMP,
                )
            }
            SurfaceFinish.FROSTED -> {
                // Vertical, and only across the top two thirds: a light surface needs its gradient
                // to fall the way light does, or it reads as a printing error rather than a bloom.
                containerPaint.shader = LinearGradient(
                    0f, 0f, 0f, edge * 0.68f,
                    style.containerEndColor.toInt(), style.containerColor.toInt(),
                    Shader.TileMode.CLAMP,
                )
            }
        }
        containerRect.set(0f, 0f, edge, edge)
        canvas.drawRoundRect(containerRect, radius, radius, containerPaint)
    }

    /**
     * The one or two marks that turn a filled shape into a material, drawn over the glyph.
     *
     * Each finish gets its own small vocabulary, and the point of keeping them this far apart is
     * that a user should be able to name the collection from one icon: metal has a lit top edge and
     * a shaded floor, glass has a pane highlight falling off before its midpoint, frost has a
     * bright rim and no shading at all.
     */
    private fun drawSurfaceEdges(canvas: Canvas, style: IconStyle, edge: Float, radius: Float) {
        when (style.finish) {
            SurfaceFinish.FLAT -> Unit
            SurfaceFinish.GLASS -> drawGlassPane(canvas, edge, radius)
            SurfaceFinish.METALLIC -> drawMetallicBevel(canvas, edge, radius)
            SurfaceFinish.FROSTED -> drawFrostedRim(canvas, edge, radius)
        }
    }

    /**
     * Glass: one top-down highlight falling off before the midpoint, plus a faint floor line.
     *
     * The floor is what gives the pane thickness. Without it the highlight alone reads as a
     * gradient someone forgot to finish; with it the content underneath looks genuinely enclosed.
     */
    private fun drawGlassPane(canvas: Canvas, edge: Float, radius: Float) {
        highlightPaint.reset()
        highlightPaint.isAntiAlias = true
        highlightPaint.shader = LinearGradient(
            0f, 0f, 0f, edge * 0.45f,
            GLASS_HIGHLIGHT, TRANSPARENT_WHITE,
            Shader.TileMode.CLAMP,
        )
        containerRect.set(0f, 0f, edge, edge)
        canvas.drawRoundRect(containerRect, radius, radius, highlightPaint)
        highlightPaint.shader = null

        strokeInnerArc(canvas, edge, radius, GLASS_FLOOR, atTop = false)
    }

    /**
     * Metal: a lit inner edge along the top and a shaded one along the bottom.
     *
     * This is the mark that separates Titanium from a grey tile. A machined edge catches light on
     * its upper chamfer and loses it on the lower one, and those two hairlines do more for the
     * material than any amount of gradient stacking in the fill.
     */
    private fun drawMetallicBevel(canvas: Canvas, edge: Float, radius: Float) {
        strokeInnerArc(canvas, edge, radius, METAL_BEVEL_TOP, atTop = true)
        strokeInnerArc(canvas, edge, radius, METAL_BEVEL_BOTTOM, atTop = false)
    }

    /** Frost: a bright rim all the way round, which is how a light surface catches its own edge. */
    private fun drawFrostedRim(canvas: Canvas, edge: Float, radius: Float) {
        val width = edge * INNER_EDGE_WIDTH_RATIO
        keylinePaint.shader = null
        keylinePaint.color = FROST_RIM
        keylinePaint.strokeWidth = width
        val inset = width * 1.5f
        containerRect.set(inset, inset, edge - inset, edge - inset)
        canvas.drawRoundRect(containerRect, radius - inset, radius - inset, keylinePaint)
    }

    /**
     * Strokes the top or bottom half of the container's inner outline.
     *
     * Clipped to a half rather than drawn as an arc because the shape is a rounded rectangle: the
     * stroke has to follow the corner curves to look like the surface's own edge, and half of a
     * stroked round-rect does that for free.
     */
    private fun strokeInnerArc(
        canvas: Canvas,
        edge: Float,
        radius: Float,
        color: Int,
        atTop: Boolean,
    ) {
        val width = edge * INNER_EDGE_WIDTH_RATIO
        val inset = width * 1.5f
        val saveCount = canvas.save()
        if (atTop) {
            canvas.clipRect(0f, 0f, edge, edge * 0.5f)
        } else {
            canvas.clipRect(0f, edge * 0.5f, edge, edge)
        }
        keylinePaint.shader = null
        keylinePaint.color = color
        keylinePaint.strokeWidth = width
        containerRect.set(inset, inset, edge - inset, edge - inset)
        canvas.drawRoundRect(containerRect, radius - inset, radius - inset, keylinePaint)
        canvas.restoreToCount(saveCount)
    }

    // ---- glyph ----------------------------------------------------------------------------------

    private fun drawGlyph(canvas: Canvas, style: IconStyle, source: SourceArtwork, sizePx: Int) {
        val target = SourceClassifier.targetFraction(source.shape, style)
        val compensation = if (SourceClassifier.allowsAreaCompensation(source.shape)) {
            OpticalMetrics.areaCompensation(source.opaqueFraction)
        } else {
            1f
        }
        val placement = IconNormalizer.place(
            content = source.bounds,
            sourceWidth = source.bitmap.width,
            sourceHeight = source.bitmap.height,
            canvasSize = sizePx,
            targetFraction = target,
            opticalLiftRatio = style.opticalLiftRatio,
            areaCompensation = compensation,
            massCentreX = source.massCentreX,
            massCentreY = source.massCentreY,
        )

        glyphPaint.colorFilter = colorFilterFor(style, source)

        val saveCount = canvas.save()
        if (SourceClassifier.requiresCornerClip(source.shape)) {
            // Borrowed artwork that brought its own square tile is cut to DarkFrame's geometry, so
            // it reads as a deliberate edge inside ours instead of a foreign tile.
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
                // rather than ramped — otherwise a mask authored in mid-grey comes out dim.
                ColorMatrices.flatTint(style.glyphTint)

            style.glyphMode == GlyphMode.TINT ->
                // Ramped from the container, not from black: the receding end of a monochrome
                // treatment is whatever surface the glyph sits on. On a light collection like Frost
                // ramping from black instead crushes a colourful source into one dark value.
                ColorMatrices.lumaRamp(
                    low = opaque(style.containerColor),
                    high = style.glyphTint,
                    contrast = style.glyphContrast,
                )

            else -> preserveMatrix(style, source) ?: return null
        }
        return ColorMatrixColorFilter(ColorMatrix(matrix))
    }

    /**
     * The colour-preserving path, which is where "recognisably still the app" is won or lost.
     *
     * Two per-icon decisions, both from what the scan measured rather than from a global constant:
     * how much vibrancy to pull back (none at all for the muted palettes that needed no help), and
     * whether the artwork sits close enough to the container's own luminance to need separating
     * from it. Returns null when neither applies, which is the common case and skips the filter
     * entirely.
     */
    private fun preserveMatrix(style: IconStyle, source: SourceArtwork): FloatArray? {
        val saturation = OpticalMetrics.vibrancy(source.meanSaturation, style.glyphSaturation)
        // Curated artwork is DarkFrame's own and is already drawn for its container, so it is not
        // second-guessed; only borrowed artwork gets separated.
        val lift = if (source.fromCurated) {
            0f
        } else {
            OpticalMetrics.separationLift(
                sourceLuma = source.meanLuma,
                containerLuma = ColorMatrices.gammaLuma(style.containerColor),
            )
        }
        val adjustsSaturation = kotlin.math.abs(saturation - 1f) > 0.001f
        val adjustsLuma = kotlin.math.abs(lift) > 0.001f
        return when {
            adjustsSaturation || adjustsLuma -> ColorMatrices.preserve(saturation, lift)
            else -> null
        }
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

    /** Container colours may be translucent (Glass); a ramp endpoint must not be. */
    private fun opaque(color: Long): Long = color or 0xFF000000L

    private fun drawKeyline(canvas: Canvas, style: IconStyle, edge: Float, radius: Float) {
        if (style.keylineWidthRatio <= 0f) return
        if ((style.keylineColor ushr 24) == 0L) return
        val width = edge * style.keylineWidthRatio
        keylinePaint.shader = null
        keylinePaint.color = style.keylineColor.toInt()
        keylinePaint.strokeWidth = width
        // Inset by half the stroke so the keyline sits fully inside the bitmap; stroking on the
        // exact edge clips its outer half and halves its apparent weight.
        val inset = width / 2f
        containerRect.set(inset, inset, edge - inset, edge - inset)
        canvas.drawRoundRect(containerRect, radius - inset, radius - inset, keylinePaint)
    }

    private companion object {
        const val TRANSPARENT_WHITE = 0x00FFFFFF
        const val GLASS_HIGHLIGHT = 0x26FFFFFF
        const val GLASS_FLOOR = 0x1F000000
        const val METAL_BEVEL_TOP = 0x2EFFFFFF
        const val METAL_BEVEL_BOTTOM = 0x24000000
        const val FROST_RIM = 0x3DFFFFFF
        const val INNER_EDGE_WIDTH_RATIO = 0.009f
        const val DERIVED_LIGHT_GLYPH = 0xFFF2F1EEL
        const val DERIVED_DARK_GLYPH = 0xFF23262BL
    }
}
