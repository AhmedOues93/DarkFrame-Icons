package com.darkframe.icons.engine.domain

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * The optical corrections that make icons from unrelated apps read as one pack.
 *
 * Normalising the *bounding box* of every source to the same size — which is what DarkFrame did
 * before this file existed — is only half the job, and it is the half that looks fine in a
 * screenshot of six hand-picked apps and wrong in a grid of two hundred real ones. Three specific
 * errors come out of it, and each one has a correction here:
 *
 *  1. **Area.** A circle inscribed in a box covers 78.5% of it, so a circular logo normalised to
 *     the same box as a square one reads visibly smaller. Android's own keyline grid encodes the
 *     same compensation: 176/192 for round shapes against 152/192 for square ones.
 *  2. **Centre.** An asymmetric glyph — a play triangle, a comma, a leaning wordmark — has its
 *     visual mass away from the centre of its bounding box, so box-centring leaves it looking
 *     pushed to one side.
 *  3. **Separation.** A colour-preserving collection has a fixed container, and an app whose
 *     artwork happens to sit at the container's own luminance disappears into it. Very dark icons
 *     on Glass and very bright ones on a light surface are the two cases that actually occur.
 *
 * All pure, all unit-tested. The thresholds here are design constants, not tuning knobs to be
 * nudged per device.
 */
object OpticalMetrics {

    // ---- 1. optical area ----------------------------------------------------------------------

    /**
     * Ceiling on area compensation.
     *
     * A circle needs 1.13 and is the case this exists for. A sparse glyph — thin strokes with a
     * lot of space between them — asks for much more, and giving it more is wrong: a hairline
     * wordmark blown up to match the ink mass of a solid square runs out of the tile. Capped just
     * above the circle case so the correction stays a correction.
     */
    const val MAX_AREA_COMPENSATION = 1.16f

    /**
     * Fill ratios above this are treated as solid and left alone. Just under 1.0 because
     * anti-aliased edges and the scanner's one-stride padding keep even a solid square slightly
     * short of a perfect 1.0.
     */
    const val SOLID_FILL = 0.94f

    /**
     * Scale factor to apply to a glyph's target size, from how densely it fills its own box.
     *
     * The ideal correction equalises ink *area*, which for a fill ratio `f` means scaling linear
     * dimensions by `1/sqrt(f)`. That is exactly right at the circle end and too aggressive at the
     * sparse end, so it is clamped rather than damped — a clamp keeps the circle case exact, where
     * a damping exponent would compromise it to improve a case that should not be corrected at all.
     *
     * @param opaqueFraction share of the content box that is opaque, in 0..1.
     */
    fun areaCompensation(opaqueFraction: Float): Float {
        if (opaqueFraction <= 0f) return 1f
        if (opaqueFraction >= SOLID_FILL) return 1f
        return (1f / sqrt(opaqueFraction)).coerceIn(1f, MAX_AREA_COMPENSATION)
    }

    // ---- 2. optical centre --------------------------------------------------------------------

    /**
     * How far to move a glyph from its box centre towards its centre of mass.
     *
     * Not 1.0 on purpose. Full centroid alignment over-corrects shapes with one heavy limb — a
     * magnifying glass or a location pin ends up looking pulled towards its handle. A third of the
     * way takes the obvious wrongness out of asymmetric glyphs and leaves symmetric ones where
     * they were, since for those the two centres coincide.
     */
    const val CENTROID_BLEND = 0.34f

    /**
     * Cap on the shift, as a fraction of the glyph's own extent on that axis. Guards against a
     * pathological source — one faint pixel in a far corner — dragging the glyph off the tile.
     */
    const val MAX_CENTROID_SHIFT = 0.08f

    /**
     * Offset to add to a placed glyph's position on one axis, in output pixels.
     *
     * @param boxCentre centre of the content box, in source pixels.
     * @param massCentre alpha-weighted centre of the content, in source pixels.
     * @param extentPx the glyph's extent on this axis in the *output*, in pixels.
     * @param sourceExtent the content box's extent on this axis, in source pixels.
     */
    fun centringShift(
        boxCentre: Float,
        massCentre: Float,
        extentPx: Float,
        sourceExtent: Float,
    ): Float {
        if (sourceExtent <= 0f || extentPx <= 0f) return 0f
        // Moving the glyph so its mass lands on the tile centre means moving it *against* the
        // offset of its mass from its box centre.
        val fraction = (boxCentre - massCentre) / sourceExtent
        val capped = fraction.coerceIn(-MAX_CENTROID_SHIFT, MAX_CENTROID_SHIFT)
        return capped * CENTROID_BLEND * extentPx
    }

    // ---- 3. separation from the container ------------------------------------------------------

    /**
     * Minimum gap, in gamma-encoded luma, between a colour-preserving source and its container.
     *
     * Stated in gamma space rather than in WCAG linear luminance because the correction is applied
     * by a `ColorMatrix`, which also works in gamma space; converting back and forth would make
     * the number mean something different from what the matrix does with it.
     */
    const val MIN_SEPARATION = 0.14f

    /** Luma the corrected source is never pushed past, so a lift cannot blow out to flat white. */
    const val MAX_LIFTED_LUMA = 0.96f

    /** And never pushed below, so a cut cannot crush to flat black. */
    const val MIN_LIFTED_LUMA = 0.04f

    /**
     * Signed luma offset, in 0..1, that separates a source from the surface it sits on.
     *
     * Returns 0 for the overwhelming majority of icons: this only engages when an app's artwork
     * genuinely sits within [MIN_SEPARATION] of the container's own luma, which on Glass's dark
     * translucent surface is a real population (dark-mode-first app icons) and on Color Pop's
     * graphite is a smaller one.
     *
     * The push is always *away* from the container, and when the source sits exactly on it the
     * direction is away from whichever side the container is on — a dark container lifts, a light
     * one cuts.
     */
    fun separationLift(sourceLuma: Float, containerLuma: Float): Float {
        val source = sourceLuma.coerceIn(0f, 1f)
        val container = containerLuma.coerceIn(0f, 1f)
        val gap = source - container
        if (abs(gap) >= MIN_SEPARATION) return 0f

        val direction = when {
            gap > 0f -> 1f
            gap < 0f -> -1f
            // Sitting exactly on the container: go wherever there is more room.
            container < 0.5f -> 1f
            else -> -1f
        }
        val target = (container + direction * MIN_SEPARATION)
            .coerceIn(MIN_LIFTED_LUMA, MAX_LIFTED_LUMA)
        return target - source
    }

    // ---- vibrancy -----------------------------------------------------------------------------

    /**
     * Saturation below which artwork is left completely alone, keeping all of its identity.
     *
     * Color Pop's whole premise is that the app is still recognisably itself, so a global
     * desaturation — even a gentle one — is the wrong instrument: it takes the most character out
     * of exactly the muted, carefully-chosen brand palettes that needed no help.
     */
    const val MUTED_SATURATION = 0.28f

    /** Saturation at and above which the style's full pull-back is applied. */
    const val NEON_SATURATION = 0.74f

    /**
     * The saturation multiplier to actually use for one source, given a style's nominal one.
     *
     * Interpolates between "untouched" for muted artwork and the style's own value for artwork
     * saturated enough to read as neon against a dark container. A style with no pull-back
     * (`base >= 1`) is returned unchanged, so this costs nothing where it is not wanted.
     *
     * @param meanSaturation mean HSV saturation of the source's opaque pixels, in 0..1.
     * @param base the style's nominal [IconStyle.glyphSaturation].
     */
    fun vibrancy(meanSaturation: Float, base: Float): Float {
        if (base >= 1f) return base
        val span = NEON_SATURATION - MUTED_SATURATION
        val t = ((meanSaturation - MUTED_SATURATION) / span).coerceIn(0f, 1f)
        return 1f + (base - 1f) * t
    }
}
