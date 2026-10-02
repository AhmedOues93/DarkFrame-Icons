package com.darkframe.icons.engine.render

import android.graphics.Bitmap
import androidx.annotation.WorkerThread
import com.darkframe.icons.engine.domain.ColorMatrices
import com.darkframe.icons.engine.domain.ContentBounds

/**
 * Everything the engine learns from one look at a source bitmap's pixels.
 *
 * All of it comes from a single strided pass. That matters: the browser resolves several hundred
 * icons in a burst, and a second pass to answer "how saturated is this?" would double the pixel
 * work that the performance pass was built to contain.
 */
data class ScanResult(
    val bounds: ContentBounds,
    /** Share of sampled pixels *inside* [bounds] that are opaque enough to count, in 0..1. */
    val opaqueFraction: Float,
    /** Alpha-weighted centre of the content, in source pixels. Falls back to the box centre. */
    val massCentreX: Float,
    val massCentreY: Float,
    /** Mean gamma-encoded luma of the opaque pixels, in 0..1. */
    val meanLuma: Float,
    /** Mean HSV saturation of the opaque pixels, in 0..1. */
    val meanSaturation: Float,
)

/**
 * Measures source artwork: its opaque extent, how densely it fills it, where its visual mass sits,
 * and how bright and how colourful it is.
 *
 * The extent is what lets the engine normalise optical size at all — without it an adaptive
 * foreground with 35% coverage and a full-bleed PNG end up visibly different sizes in the same
 * grid. The other three feed the optical corrections in
 * [com.darkframe.icons.engine.domain.OpticalMetrics]: density drives area compensation, the mass
 * centre drives centring, and luma and saturation decide whether a colour-preserving collection
 * needs to separate the source from its container or pull its vibrancy back.
 *
 * Sampled rather than exhaustive. A stride keeps the scan at a few thousand reads regardless of
 * source resolution — and the bounds it produces are then padded outward by one stride, so
 * sampling can only ever *over*-report the content box, never clip real artwork.
 */
object ContentBoundsScanner {

    /**
     * Alpha below this is treated as empty. Above zero on purpose: a lot of artwork carries faint
     * ghost pixels, soft drop shadows or near-transparent bounding boxes, and trusting alpha > 0
     * would report almost every such icon as full-bleed.
     */
    const val ALPHA_THRESHOLD = 16

    /** Target samples along each axis. ~96x96 reads is plenty to locate a content box. */
    const val MAX_SAMPLES_PER_AXIS = 96

    @WorkerThread
    fun scan(bitmap: Bitmap): ScanResult {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= 0 || height <= 0) return empty()

        val stride = maxOf(1, maxOf(width, height) / MAX_SAMPLES_PER_AXIS)

        var minX = Int.MAX_VALUE
        var minY = Int.MAX_VALUE
        var maxX = Int.MIN_VALUE
        var maxY = Int.MIN_VALUE
        var opaque = 0

        // Weighted by alpha rather than counted, so a soft edge pulls the centre of mass less than
        // a solid one — which is the whole point of using mass rather than extent.
        var weight = 0f
        var weightedX = 0f
        var weightedY = 0f
        var lumaSum = 0f
        var saturationSum = 0f

        val row = IntArray(width)
        var y = 0
        while (y < height) {
            bitmap.getPixels(row, 0, width, 0, y, width, 1)
            var x = 0
            while (x < width) {
                val pixel = row[x]
                val alpha = pixel ushr 24
                if (alpha >= ALPHA_THRESHOLD) {
                    opaque++
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y

                    val a = alpha / 255f
                    weight += a
                    weightedX += x * a
                    weightedY += y * a
                    lumaSum += a * ColorMatrices.gammaLuma((pixel.toLong() and 0xFFFFFFL))
                    saturationSum += a * saturationOf(pixel)
                }
                x += stride
            }
            y += stride
        }

        if (maxX < minX || maxY < minY) return empty()

        // Pad outward by one stride so a sampled scan never crops real artwork, then clamp.
        val bounds = ContentBounds(
            left = (minX - stride).coerceAtLeast(0),
            top = (minY - stride).coerceAtLeast(0),
            right = (maxX + stride + 1).coerceAtMost(width),
            bottom = (maxY + stride + 1).coerceAtMost(height),
        )

        // Opaque share is measured against the samples that fell inside the content box, so an
        // icon is judged on how solid its artwork is rather than on how much padding surrounds it.
        val boxSamples = boxSampleCount(bounds, stride)
        val fraction = if (boxSamples > 0) {
            (opaque.toFloat() / boxSamples).coerceIn(0f, 1f)
        } else {
            0f
        }

        val hasWeight = weight > 0f
        return ScanResult(
            bounds = bounds,
            opaqueFraction = fraction,
            massCentreX = if (hasWeight) weightedX / weight else bounds.centreX,
            massCentreY = if (hasWeight) weightedY / weight else bounds.centreY,
            meanLuma = if (hasWeight) (lumaSum / weight).coerceIn(0f, 1f) else 0f,
            meanSaturation = if (hasWeight) (saturationSum / weight).coerceIn(0f, 1f) else 0f,
        )
    }

    /**
     * HSV saturation of one packed pixel: how far the channels spread, relative to the brightest.
     *
     * Open-coded rather than going through `android.graphics.Color`, which allocates a float array
     * per call — several hundred thousand of them across one browser load.
     */
    private fun saturationOf(pixel: Int): Float {
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        val max = maxOf(r, g, b)
        if (max == 0) return 0f
        val min = minOf(r, g, b)
        return (max - min).toFloat() / max
    }

    private fun empty() = ScanResult(ContentBounds.EMPTY, 0f, 0f, 0f, 0f, 0f)

    private fun boxSampleCount(bounds: ContentBounds, stride: Int): Int {
        if (bounds.isEmpty) return 0
        val cols = (bounds.width + stride - 1) / stride
        val rows = (bounds.height + stride - 1) / stride
        return cols * rows
    }
}
