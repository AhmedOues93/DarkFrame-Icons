package com.darkframe.icons.engine.wallpaper

import com.darkframe.icons.model.ContentTier
import com.darkframe.icons.model.WallpaperCategory

/** How a wallpaper's surface is constructed. */
enum class WallpaperPattern {
    /** Nothing but the base colour. The only honest way to draw a true-black AMOLED wallpaper. */
    SOLID,

    /** A soft off-centre radial, so the screen has a light source rather than a flat wash. */
    GLOW,

    /** Fine diagonal weave, scaled to the screen. Carbon. */
    WEAVE,

    /** Brushed metal: many low-contrast parallel strokes along one axis. */
    BRUSHED,

    /** Two broad translucent panes crossing, with one highlight edge each. Glass. */
    PANES,

    /** A single hairline frame inset from the edges. Minimal. */
    FRAME,

    /** Slow overlapping arcs. Abstract, still restrained. */
    ARCS,

    /** A vertical seam with a subtle tonal shift either side, for the fold. */
    SEAM,
}

/**
 * A wallpaper, described rather than stored.
 *
 * DarkFrame's wallpapers are drawn on the device at exactly the size the screen needs. That is a
 * deliberate choice over shipping image files: a Fold8 needs a 2176x1812 inner-screen wallpaper and
 * a 968x2376 cover-screen one, and shipping bitmaps for every panel of every device would add tens
 * of megabytes to the APK to deliver flat colour fields and fine gradients that compress badly and
 * band visibly. Drawing them means every wallpaper is pin-sharp at any size, on any panel, for a few
 * kilobytes of code.
 *
 * Pure data so the catalog's coverage and palette rules are unit-testable.
 */
data class WallpaperSpec(
    val id: String,
    val title: String,
    val category: WallpaperCategory,
    val pattern: WallpaperPattern,
    /** Base fill, ARGB. */
    val base: Long,
    /** Secondary tone used by the pattern; equal to [base] for [WallpaperPattern.SOLID]. */
    val accent: Long,
    /** Pattern strength, 0..1. Low by design: a wallpaper sits behind icons, not in front of them. */
    val intensity: Float,
    val tier: ContentTier,
) {
    init {
        require(intensity in 0f..1f) { "intensity out of range for $id" }
    }
}
