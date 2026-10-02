package com.darkframe.icons.engine.domain

/**
 * Colour matrices used to apply [GlyphMode] to source artwork.
 *
 * These are plain 4x5 row-major arrays in the layout `android.graphics.ColorMatrix` expects, but
 * they are built here with no framework dependency so the maths is unit-tested rather than
 * eyeballed on a device.
 */
object ColorMatrices {

    // Rec. 709 luma weights. Perceptual rather than naive averaging, which matters a lot when
    // flattening saturated brand colours: a naive average turns red and blue into the same grey.
    const val LUMA_R = 0.2126f
    const val LUMA_G = 0.7152f
    const val LUMA_B = 0.0722f

    /**
     * Scales saturation around the luma axis. `1f` is identity, `0f` is fully desaturated.
     */
    fun saturation(level: Float): FloatArray {
        val s = level.coerceIn(0f, 2f)
        val inv = 1f - s
        val r = LUMA_R * inv
        val g = LUMA_G * inv
        val b = LUMA_B * inv
        return floatArrayOf(
            r + s, g, b, 0f, 0f,
            r, g + s, b, 0f, 0f,
            r, g, b + s, 0f, 0f,
            0f, 0f, 0f, 1f, 0f,
        )
    }

    /**
     * Collapses colour to luma and maps it onto a single tint, preserving alpha.
     *
     * Opaque white source pixels land exactly on [tint]; fully black pixels land on black. That
     * keeps interior detail in a monochrome glyph instead of flattening it to a silhouette, which
     * is what makes Noir and Frost read as designed rather than stamped.
     */
    fun tintToLuma(tint: Long): FloatArray {
        val tr = ((tint shr 16) and 0xFF) / 255f
        val tg = ((tint shr 8) and 0xFF) / 255f
        val tb = (tint and 0xFF) / 255f
        return floatArrayOf(
            LUMA_R * tr, LUMA_G * tr, LUMA_B * tr, 0f, 0f,
            LUMA_R * tg, LUMA_G * tg, LUMA_B * tg, 0f, 0f,
            LUMA_R * tb, LUMA_G * tb, LUMA_B * tb, 0f, 0f,
            0f, 0f, 0f, 1f, 0f,
        )
    }

    /**
     * Flat tint that discards source luma entirely, keeping only alpha. Used for adaptive
     * monochrome layers, which are already authored as a single-colour mask.
     */
    fun flatTint(tint: Long): FloatArray {
        val tr = ((tint shr 16) and 0xFF) / 255f
        val tg = ((tint shr 8) and 0xFF) / 255f
        val tb = (tint and 0xFF) / 255f
        return floatArrayOf(
            0f, 0f, 0f, 0f, tr * 255f,
            0f, 0f, 0f, 0f, tg * 255f,
            0f, 0f, 0f, 0f, tb * 255f,
            0f, 0f, 0f, 1f, 0f,
        )
    }

    /**
     * WCAG relative luminance of an ARGB value, in 0..1, ignoring alpha.
     *
     * Note the deliberate difference from the matrices above: a `ColorMatrix` operates on
     * gamma-encoded sRGB values, so [LUMA_R]/[LUMA_G]/[LUMA_B] are applied there directly. Design
     * contrast, by contrast, is only meaningful on *linear* luminance, so each channel is
     * linearised first here. Mixing the two up is what produces palettes that pass a numeric check
     * and still look muddy on a device.
     */
    fun relativeLuminance(argb: Long): Float {
        val r = linearise(((argb shr 16) and 0xFF) / 255f)
        val g = linearise(((argb shr 8) and 0xFF) / 255f)
        val b = linearise((argb and 0xFF) / 255f)
        return LUMA_R * r + LUMA_G * g + LUMA_B * b
    }

    private fun linearise(channel: Float): Float =
        if (channel <= 0.04045f) {
            channel / 12.92f
        } else {
            Math.pow(((channel + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()
        }

    /**
     * WCAG contrast ratio between two opaque ARGB values, in the range 1..21.
     *
     * Used as a design guard rail in tests: it is what stops a future palette edit from shipping a
     * collection whose glyphs have quietly lost their separation from the container.
     */
    fun contrastRatio(a: Long, b: Long): Float {
        val la = relativeLuminance(a) + 0.05f
        val lb = relativeLuminance(b) + 0.05f
        return if (la > lb) la / lb else lb / la
    }
}
