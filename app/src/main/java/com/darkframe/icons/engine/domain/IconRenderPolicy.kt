package com.darkframe.icons.engine.domain

import kotlin.math.max
import kotlin.math.min

/**
 * How much work a single icon render is allowed to cost.
 *
 * This exists because of a measured problem, not a theoretical one: browsing the app list made a
 * Galaxy Z Fold8 noticeably warm. Rendering one icon is cheap; rendering a few hundred of them at
 * full resolution, on every core at once, is not.
 *
 * Four rules, all enforced here so they are testable without a device:
 *
 *  1. **Previews are never rendered at export resolution.** A grid cell is at most
 *     [MAX_PREVIEW_PX]; 512px output is reserved for an actual export or apply.
 *  2. **Sizes are quantised into buckets.** A 180px cell and a 228px cell both resolve to the same
 *     bucket, so unfolding a Fold — which changes the cell size — re-uses the cached bitmaps
 *     instead of re-rendering the whole grid.
 *  3. **The work buffer scales with the target.** The source used to be rasterised at a fixed 288px
 *     whatever the output size, which allocated two ~330KB bitmaps per icon regardless of need.
 *  4. **Concurrency is bounded well below the core count.** Saturatingeight cores with bitmap work
 *     is exactly what heats a phone up; a small pool keeps the grid filling quickly without it.
 */
object IconRenderPolicy {

    /** Largest bitmap the browser may ask for, whatever the cell size works out to. */
    const val MAX_PREVIEW_PX = 192

    /** Resolution used for an export or a launcher hand-off, where quality actually matters. */
    const val EXPORT_PX = 512

    /** Smallest useful work buffer. Below this, normalisation has too little to measure. */
    const val MIN_WORK_PX = 96

    /** Hard ceiling on the source raster, matching the old fixed size. */
    const val MAX_WORK_PX = 288

    /**
     * Size buckets a request snaps up into.
     *
     * Deliberately coarse. Every distinct size is a separate cache entry and a separate render, so
     * two layouts that differ by a few pixels must not become two populations of bitmaps — which is
     * precisely what a fold/unfold does to a width-derived grid.
     */
    val SIZE_BUCKETS = intArrayOf(48, 64, 96, 128, 192)

    /**
     * The size the browser should actually request for a cell of [requestedPx].
     *
     * Rounds *up* to the next bucket so an icon is never upscaled into its cell, and clamps at
     * [MAX_PREVIEW_PX].
     */
    fun previewSizePx(requestedPx: Int): Int {
        if (requestedPx <= 0) return SIZE_BUCKETS.first()
        val capped = min(requestedPx, MAX_PREVIEW_PX)
        return SIZE_BUCKETS.firstOrNull { it >= capped } ?: MAX_PREVIEW_PX
    }

    /** True for a size that is only legitimate for export/apply, not for on-screen browsing. */
    fun isExportSize(sizePx: Int): Boolean = sizePx > MAX_PREVIEW_PX

    /**
     * Edge length to rasterise the source artwork at, for a given output size.
     *
     * 1.5x the target: enough headroom that downscaling stays clean and that the alpha scan has
     * real pixels to measure, without paying for a 288px buffer to fill a 64px cell.
     */
    fun workSizePx(targetPx: Int): Int {
        if (targetPx <= 0) return MIN_WORK_PX
        val scaled = (targetPx * 3) / 2
        return max(MIN_WORK_PX, min(MAX_WORK_PX, scaled))
    }

    /**
     * How many icons may render at once.
     *
     * Capped at two regardless of core count. The grid shows at most a dozen cells, each render is
     * tens of milliseconds, and two workers fill a screen faster than a user can scroll — while
     * leaving the rest of the CPU idle and cool.
     */
    fun maxParallelRenders(cpuCount: Int): Int = max(1, min(2, cpuCount))

    /**
     * Whether a cell size change should force new renders.
     *
     * False when both sizes land in the same bucket, which is the fold/unfold case the heat
     * complaint came from.
     */
    fun requiresRerender(oldCellPx: Int, newCellPx: Int): Boolean =
        previewSizePx(oldCellPx) != previewSizePx(newCellPx)
}
