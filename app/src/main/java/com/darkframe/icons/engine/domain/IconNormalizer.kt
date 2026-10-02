package com.darkframe.icons.engine.domain

import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Opaque content bounds of a source icon, in source pixels. [right] and [bottom] are exclusive.
 * An empty instance means "no opaque pixels were found".
 */
data class ContentBounds(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width: Int get() = max(0, right - left)
    val height: Int get() = max(0, bottom - top)
    val isEmpty: Boolean get() = width <= 0 || height <= 0

    companion object {
        val EMPTY = ContentBounds(0, 0, 0, 0)
        fun full(width: Int, height: Int) = ContentBounds(0, 0, width, height)
    }
}

/** Where normalized source content should be drawn inside a square icon canvas. */
data class GlyphPlacement(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val scale: Float,
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
}

/**
 * Turns wildly inconsistent source artwork into one optical size.
 *
 * Installed icons arrive in every imaginable shape: a legacy 48px bitmap with its own baked-in
 * tile and 10% bezel, an adaptive icon whose foreground floats in a 108dp canvas with only 40%
 * coverage, a full-bleed transparent PNG, a tall non-square logo. Scaling all of them to the same
 * *canvas* leaves them looking randomly sized; scaling them so their *opaque content* fills the
 * same fraction of the canvas is what makes a grid look designed.
 *
 * Pure math by design — the bitmap work lives in the render layer, the rules live here and are
 * covered by unit tests.
 */
object IconNormalizer {

    /**
     * Cap on enlargement. A source whose content is tiny (a 20px glyph in a 108px canvas, or a
     * badly authored icon) is lifted towards the target but never blown up past this factor,
     * because past it the result is visibly soft and looks worse than being slightly small.
     */
    const val MAX_UPSCALE = 2.6f

    /**
     * Shapes more elongated than this are treated as wordmarks rather than icons: they are fitted
     * against a slightly tighter target so they do not run edge to edge across the tile.
     *
     * Set well clear of 2:1, which is an ordinary logo proportion rather than a wordmark — pulling
     * those in would make a large number of perfectly normal icons read as undersized.
     */
    const val WORDMARK_ASPECT = 2.4f

    private const val WORDMARK_TARGET_FACTOR = 0.92f

    /**
     * @param content opaque bounds of the source.
     * @param sourceWidth/[sourceHeight] full source canvas, used when [content] is empty.
     * @param canvasSize edge length of the square output canvas, in pixels.
     * @param targetFraction desired long-edge coverage, i.e. [IconStyle.glyphScale].
     * @param opticalLiftRatio upward shift as a fraction of [canvasSize].
     */
    fun place(
        content: ContentBounds,
        sourceWidth: Int,
        sourceHeight: Int,
        canvasSize: Int,
        targetFraction: Float,
        opticalLiftRatio: Float = 0f,
    ): GlyphPlacement {
        require(canvasSize > 0) { "canvasSize must be positive" }
        require(targetFraction > 0f) { "targetFraction must be positive" }

        // No opaque pixels found (fully transparent source, or a scan that could not run):
        // fall back to the whole source canvas so we still produce a sanely sized result.
        val bounds = if (content.isEmpty) {
            if (sourceWidth > 0 && sourceHeight > 0) {
                ContentBounds.full(sourceWidth, sourceHeight)
            } else {
                ContentBounds.full(canvasSize, canvasSize)
            }
        } else {
            content
        }

        val longEdge = max(bounds.width, bounds.height).toFloat()
        val aspect = max(bounds.width, bounds.height).toFloat() /
            max(1f, kotlin.math.min(bounds.width, bounds.height).toFloat())

        val effectiveTarget = if (aspect >= WORDMARK_ASPECT) {
            targetFraction * WORDMARK_TARGET_FACTOR
        } else {
            targetFraction
        }

        val target = canvasSize * effectiveTarget
        val scale = (target / longEdge).coerceAtMost(MAX_UPSCALE)

        val destWidth = bounds.width * scale
        val destHeight = bounds.height * scale
        val left = (canvasSize - destWidth) / 2f
        val top = (canvasSize - destHeight) / 2f - canvasSize * opticalLiftRatio

        return GlyphPlacement(left, top, left + destWidth, top + destHeight, scale)
    }

    /**
     * Inset of an adaptive icon's visible area within its full 108dp canvas.
     *
     * Adaptive icons reserve an 18dp bleed on every side of a 108dp canvas for parallax and mask
     * animation; only the inner 72dp is guaranteed visible. Rendering the full canvas makes every
     * adaptive icon look ~33% too small, so the engine crops to the visible viewport first.
     */
    const val ADAPTIVE_VISIBLE_FRACTION = 72f / 108f

    /** Pixel inset to crop from each edge of a square adaptive render of [size] px. */
    fun adaptiveCropInset(size: Int): Int =
        (size * (1f - ADAPTIVE_VISIBLE_FRACTION) / 2f).roundToInt()
}
