package com.darkframe.icons.engine.domain

import com.darkframe.icons.model.ContentTier
import com.darkframe.icons.model.IconCollection

/** How source artwork is recoloured before it is placed on a DarkFrame container. */
enum class GlyphMode {
    /** Keep the original brand colours (Color Pop, Glass). */
    PRESERVE,

    /** Collapse to luminance, then tint (Noir, Titanium, Pure AMOLED, Frost). */
    TINT,
}

/** Container treatment. Intentionally small — DarkFrame avoids decorative gradient stacking. */
enum class SurfaceFinish {
    /** Single flat fill. */
    FLAT,

    /** Two-stop neutral sweep at 135°, low contrast, no colour shift. */
    METALLIC,

    /** Flat fill plus a single restrained top highlight and a hairline inner edge. */
    GLASS,
}

/**
 * The complete, resolution-independent recipe for one DarkFrame collection.
 *
 * Everything here is a ratio or an ARGB value so the same style renders identically at launcher
 * size, in the browser grid, and in an exported 512px asset. Colours are [Long] rather than
 * `@ColorInt Int` so the type stays usable from plain JVM tests.
 */
data class IconStyle(
    val id: String,
    val collection: IconCollection,
    val displayName: String,
    val description: String,
    val tier: ContentTier,

    /** Container fill start colour. */
    val containerColor: Long,
    /** Container fill end colour; equal to [containerColor] for [SurfaceFinish.FLAT]. */
    val containerEndColor: Long,
    val finish: SurfaceFinish,

    /** Corner radius as a fraction of the icon's edge length. */
    val cornerRadiusRatio: Float,
    /** Hairline keyline colour; alpha 0 disables the keyline. */
    val keylineColor: Long,
    /** Keyline stroke width as a fraction of the icon's edge length. */
    val keylineWidthRatio: Float,

    val glyphMode: GlyphMode,
    /** Tint applied when [glyphMode] is [GlyphMode.TINT]. */
    val glyphTint: Long,
    /** Saturation multiplier applied when [glyphMode] is [GlyphMode.PRESERVE]. 1f keeps it as-is. */
    val glyphSaturation: Float,
    /** Longest glyph edge as a fraction of the icon's edge length — the optical size anchor. */
    val glyphScale: Float,
    /** Upward optical shift as a fraction of the edge length; compensates for label weight below. */
    val opticalLiftRatio: Float,
    /**
     * Prefer an adaptive icon's monochrome layer (API 33+) as the source when one exists.
     * Only meaningful for [GlyphMode.TINT] styles, where it gives a far cleaner result than
     * luminance-flattening a full-colour icon.
     */
    val preferMonochromeLayer: Boolean,
) {
    init {
        require(glyphScale in 0.2f..0.95f) { "glyphScale out of range for $id" }
        require(cornerRadiusRatio in 0f..0.5f) { "cornerRadiusRatio out of range for $id" }
    }

    /**
     * Bumped whenever the renderer or any style value changes in a way that alters output.
     * It is part of every cache key, so incrementing it invalidates every cached bitmap.
     */
    companion object {
        const val RENDER_VERSION = 3
    }
}
