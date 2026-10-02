package com.darkframe.icons.engine.domain

/** How a piece of source artwork wants to be treated on a DarkFrame container. */
enum class SourceShape {
    /** Artwork on transparency: a glyph that should sit *on* the container. */
    GLYPH,

    /**
     * Artwork that fills its own canvas opaquely — a legacy pre-adaptive icon that already has
     * its own tile, border and padding baked in.
     */
    FULL_BLEED,
}

/**
 * Decides how to seat source artwork on a container.
 *
 * The awkward case the engine has to get right is the legacy opaque icon. Plenty of installed
 * apps still ship a pre-adaptive square or circular icon with its own background baked in. Scaled
 * like a glyph, it produces the classic amateur result: a small tile floating inside a bigger tile.
 *
 * DarkFrame handles it by recognising the case and committing to it — the artwork is seated much
 * larger and clipped to DarkFrame's own corner geometry, so it reads as one deliberate inset card
 * rather than an accident. That is an honest treatment of artwork we cannot cut a glyph out of
 * without guessing.
 *
 * Pure so the thresholds are pinned by tests rather than tuned by eye on one device.
 */
object SourceClassifier {

    /** Opaque-pixel share above which artwork is treated as having its own background. */
    const val FULL_BLEED_OPAQUE_FRACTION = 0.85f

    /** Minimum share of the canvas the content bounds must span to count as full-bleed. */
    const val FULL_BLEED_COVERAGE = 0.9f

    /**
     * Canvas coverage used for full-bleed artwork, replacing the style's glyph scale.
     *
     * Raised from 0.78 after looking at what 0.78 actually produces: a visible second tile inside
     * ours, which is the "double background" result this treatment exists to avoid. At 0.94 the
     * artwork is framed by a thin margin of the collection's own surface and the keyline, which
     * reads as a deliberate edge rather than as one icon dropped inside another — and every tile in
     * the grid is then the same size, which is what makes a pack look like a pack.
     */
    const val FULL_BLEED_TARGET = 0.94f

    /**
     * @param opaqueFraction share of sampled pixels inside [bounds] whose alpha is above the
     *   scanner's threshold, in 0..1.
     * @param bounds opaque content bounds of the source.
     * @param canvasEdge edge length of the (square) source canvas the bounds were measured in.
     */
    fun classify(opaqueFraction: Float, bounds: ContentBounds, canvasEdge: Int): SourceShape {
        if (canvasEdge <= 0 || bounds.isEmpty) return SourceShape.GLYPH
        val coverage = maxOf(bounds.width, bounds.height).toFloat() / canvasEdge
        return if (opaqueFraction >= FULL_BLEED_OPAQUE_FRACTION && coverage >= FULL_BLEED_COVERAGE) {
            SourceShape.FULL_BLEED
        } else {
            SourceShape.GLYPH
        }
    }

    /** Canvas coverage to normalise this artwork to. */
    fun targetFraction(shape: SourceShape, style: IconStyle): Float = when (shape) {
        SourceShape.GLYPH -> style.glyphScale
        SourceShape.FULL_BLEED -> FULL_BLEED_TARGET
    }

    /**
     * Whether the seated artwork must be clipped to DarkFrame's corner geometry.
     *
     * Only full-bleed artwork needs it — and it needs it badly, since that is what turns a
     * borrowed square tile into something that belongs to the collection.
     */
    fun requiresCornerClip(shape: SourceShape): Boolean = shape == SourceShape.FULL_BLEED

    /**
     * Corner radius for the clipped artwork, as a fraction of the *artwork's* edge length.
     *
     * Scaled up from the container's ratio so the inner and outer curves stay visually concentric
     * instead of the inner one looking too square against the larger outer radius.
     */
    fun innerCornerRadiusRatio(style: IconStyle): Float =
        (style.cornerRadiusRatio / FULL_BLEED_TARGET).coerceAtMost(0.5f)

    /**
     * Whether a source should be area-compensated at all.
     *
     * Full-bleed artwork must not be: it fills its own box by definition, so the compensation would
     * be 1.0 anyway, and seating it by anything other than [FULL_BLEED_TARGET] is what produces the
     * mismatched tile sizes this classifier exists to prevent.
     */
    fun allowsAreaCompensation(shape: SourceShape): Boolean = shape == SourceShape.GLYPH
}
