package com.darkframe.icons.engine

import com.darkframe.icons.engine.domain.ColorMatrices
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorMatricesTest {

    /** Applies a 4x5 colour matrix the way android.graphics.ColorMatrix does. */
    private fun apply(matrix: FloatArray, argb: Long): Long {
        val a = ((argb shr 24) and 0xFF).toFloat()
        val r = ((argb shr 16) and 0xFF).toFloat()
        val g = ((argb shr 8) and 0xFF).toFloat()
        val b = (argb and 0xFF).toFloat()
        fun row(i: Int): Int {
            val o = i * 5
            val v = matrix[o] * r + matrix[o + 1] * g + matrix[o + 2] * b + matrix[o + 3] * a + matrix[o + 4]
            return v.toInt().coerceIn(0, 255)
        }
        return (row(3).toLong() shl 24) or (row(0).toLong() shl 16) or
            (row(1).toLong() shl 8) or row(2).toLong()
    }

    @Test
    fun identitySaturationLeavesColoursUntouched() {
        val m = ColorMatrices.saturation(1f)
        listOf(0xFFE1306CL, 0xFF1DB954L, 0xFF4285F4L).forEach { colour ->
            assertEquals(colour, apply(m, colour))
        }
    }

    @Test
    fun zeroSaturationCollapsesToGreyWhileKeepingAlpha() {
        val result = apply(ColorMatrices.saturation(0f), 0x80E1306CL)
        val r = (result shr 16) and 0xFF
        val g = (result shr 8) and 0xFF
        val b = result and 0xFF
        assertEquals(r, g)
        assertEquals(g, b)
        assertEquals(0x80L, (result shr 24) and 0xFF)
    }

    @Test
    fun perceptualWeightsKeepDistinctBrandColoursDistinct() {
        // A naive (r+g+b)/3 average maps pure red and pure blue to the same grey, which would make
        // two different apps render as identical Noir glyphs.
        val m = ColorMatrices.saturation(0f)
        val red = apply(m, 0xFFFF0000L) and 0xFF
        val blue = apply(m, 0xFF0000FFL) and 0xFF
        assertTrue("red and blue must not flatten to the same grey", red != blue)
        assertTrue("red is perceptually brighter than blue", red > blue)
    }

    @Test
    fun tintToLumaMapsWhiteOntoTheTintAndBlackOntoBlack() {
        val tint = 0xFFF2F1EEL
        val m = ColorMatrices.tintToLuma(tint)
        val white = apply(m, 0xFFFFFFFFL)
        // Within one 8-bit step: the luma weights sum to 1.0 only to float precision, and the
        // real pipeline rounds where this helper truncates.
        assertEquals(((tint shr 16) and 0xFF).toFloat(), ((white shr 16) and 0xFF).toFloat(), 1f)
        assertEquals(((tint shr 8) and 0xFF).toFloat(), ((white shr 8) and 0xFF).toFloat(), 1f)
        assertEquals((tint and 0xFF).toFloat(), (white and 0xFF).toFloat(), 1f)

        val black = apply(m, 0xFF000000L)
        assertEquals(0L, black and 0xFFFFFFL)
    }

    @Test
    fun tintToLumaPreservesInteriorDetailRatherThanFlatteningToASilhouette() {
        val m = ColorMatrices.tintToLuma(0xFFFFFFFFL)
        val mid = apply(m, 0xFF808080L) and 0xFF
        assertTrue("mid grey must land between black and white, was $mid", mid in 1..254)
    }

    @Test
    fun tintToLumaPreservesAlphaSoAntiAliasedEdgesSurvive() {
        val result = apply(ColorMatrices.tintToLuma(0xFFFFFFFFL), 0x40FFFFFFL)
        assertEquals(0x40L, (result shr 24) and 0xFF)
    }

    @Test
    fun flatTintIgnoresSourceLumaAndKeepsOnlyAlpha() {
        val m = ColorMatrices.flatTint(0xFFE4E7EBL)
        // An adaptive monochrome layer is already a single-colour mask; its luma carries no
        // information, so both a dark and a light source pixel must land on the same tint.
        assertEquals(apply(m, 0xFF000000L) and 0xFFFFFFL, apply(m, 0xFFFFFFFFL) and 0xFFFFFFL)
        assertEquals(0xE4E7EBL, apply(m, 0xFF123456L) and 0xFFFFFFL)
        assertEquals(0x33L, (apply(m, 0x33000000L) shr 24) and 0xFF)
    }

    @Test
    fun luminanceAndContrastMatchKnownReferenceValues() {
        assertEquals(0f, ColorMatrices.relativeLuminance(0xFF000000L), 0.001f)
        assertEquals(1f, ColorMatrices.relativeLuminance(0xFFFFFFFFL), 0.001f)
        assertEquals(21f, ColorMatrices.contrastRatio(0xFFFFFFFFL, 0xFF000000L), 0.05f)
        assertEquals(1f, ColorMatrices.contrastRatio(0xFF808080L, 0xFF808080L), 0.001f)
        // Order must not matter.
        assertEquals(
            ColorMatrices.contrastRatio(0xFFFFFFFFL, 0xFF444444L),
            ColorMatrices.contrastRatio(0xFF444444L, 0xFFFFFFFFL),
            0.001f,
        )
    }
}
