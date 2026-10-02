package com.darkframe.icons.engine.render

import android.graphics.Bitmap
import androidx.annotation.WorkerThread
import com.darkframe.icons.engine.domain.ContentBounds

/** Result of inspecting a source bitmap's alpha channel. */
data class ScanResult(
    val bounds: ContentBounds,
    /** Share of sampled pixels *inside* [bounds] that are opaque enough to count, in 0..1. */
    val opaqueFraction: Float,
)

/**
 * Finds the opaque extent of source artwork.
 *
 * This is what lets the engine normalise optical size: without it, an adaptive foreground with 35%
 * coverage and a full-bleed PNG end up visually very different sizes on the same grid.
 *
 * Sampled rather than exhaustive. A stride keeps the scan at a few thousand reads regardless of
 * source resolution, which matters when the browser is resolving several hundred apps — and the
 * bounds it produces are then padded outward by one stride so sampling can only ever *over*-report
 * the content box, never clip real artwork.
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
        if (width <= 0 || height <= 0) return ScanResult(ContentBounds.EMPTY, 0f)

        val stride = maxOf(1, maxOf(width, height) / MAX_SAMPLES_PER_AXIS)

        var minX = Int.MAX_VALUE
        var minY = Int.MAX_VALUE
        var maxX = Int.MIN_VALUE
        var maxY = Int.MIN_VALUE
        var opaque = 0
        var sampled = 0

        val row = IntArray(width)
        var y = 0
        while (y < height) {
            bitmap.getPixels(row, 0, width, 0, y, width, 1)
            var x = 0
            while (x < width) {
                sampled++
                if ((row[x] ushr 24) >= ALPHA_THRESHOLD) {
                    opaque++
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
                x += stride
            }
            y += stride
        }

        if (maxX < minX || maxY < minY) return ScanResult(ContentBounds.EMPTY, 0f)

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
        return ScanResult(bounds, fraction)
    }

    private fun boxSampleCount(bounds: ContentBounds, stride: Int): Int {
        if (bounds.isEmpty) return 0
        val cols = (bounds.width + stride - 1) / stride
        val rows = (bounds.height + stride - 1) / stride
        return cols * rows
    }
}
