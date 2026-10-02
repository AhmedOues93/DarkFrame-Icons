package com.darkframe.icons.engine

import com.darkframe.icons.engine.domain.ColorMatrices
import com.darkframe.icons.engine.domain.GlyphMode
import com.darkframe.icons.engine.domain.IconStyleCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Guards the fix for the collapse a first visual pass found in Frost: ramping a source's luma from
 * black crushes a colourful icon into one dark value on a light container.
 */
class FrostLumaRampTest {

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

    private fun rampFor(styleId: String): FloatArray {
        val style = IconStyleCatalog.forId(styleId)
        return ColorMatrices.lumaRamp(style.containerColor or 0xFF000000L, style.glyphTint)
    }

    /** The two source values from the legacy-icon case that previously collapsed. */
    private val greenField = 0xFF1C6E4FL
    private val paleMark = 0xFFEAF6F0L

    @Test
    fun frostKeepsTwoDifferentSourceValuesApart() {
        val m = rampFor("frost")
        val field = ColorMatrices.relativeLuminance(apply(m, greenField))
        val mark = ColorMatrices.relativeLuminance(apply(m, paleMark))
        assertTrue(
            "Frost must separate a mid field from a bright mark, got $field vs $mark",
            abs(field - mark) > 0.12f,
        )
    }

    @Test
    fun theOldRampIsWhatCollapsedThemSoTheFixIsLoadBearing() {
        // Documents the defect: ramping from black put both values in the same dark band.
        val old = ColorMatrices.tintToLuma(IconStyleCatalog.frost.glyphTint)
        val field = ColorMatrices.relativeLuminance(apply(old, greenField))
        val mark = ColorMatrices.relativeLuminance(apply(old, paleMark))
        assertTrue("the old behaviour should collapse these", abs(field - mark) < 0.03f)
    }

    @Test
    fun everyTintedCollectionSeparatesThoseTwoValues() {
        IconStyleCatalog.all
            .filter { it.glyphMode == GlyphMode.TINT }
            .forEach { style ->
                val m = rampFor(style.id)
                val field = ColorMatrices.relativeLuminance(apply(m, greenField))
                val mark = ColorMatrices.relativeLuminance(apply(m, paleMark))
                assertTrue(
                    "${style.id} collapses a colour source: $field vs $mark",
                    abs(field - mark) > 0.12f,
                )
            }
    }

    @Test
    fun theFixIsNotAGlobalBrightening() {
        // The ink end is what a viewer reads as the glyph, and it must be untouched: every tinted
        // collection still puts a bright source exactly on its own tint, as before.
        IconStyleCatalog.all
            .filter { it.glyphMode == GlyphMode.TINT }
            .forEach { style ->
                val new = apply(rampFor(style.id), 0xFFFFFFFFL)
                val old = apply(ColorMatrices.tintToLuma(style.glyphTint), 0xFFFFFFFFL)
                for (shift in listOf(16, 8, 0)) {
                    val a = ((new shr shift) and 0xFF).toInt()
                    val b = ((old shr shift) and 0xFF).toInt()
                    assertTrue("${style.id} ink moved: $a vs $b", abs(a - b) <= 1)
                }
            }
    }

    @Test
    fun aBlackSourceLandsExactlyOnTheContainerAndSoDisappearsIntoIt() {
        // This is the whole point of ramping from the container rather than from black: the part of
        // a source that carries no ink becomes the surface, in every collection, light or dark.
        IconStyleCatalog.all
            .filter { it.glyphMode == GlyphMode.TINT }
            .forEach { style ->
                val got = apply(rampFor(style.id), 0xFF000000L) and 0xFFFFFFL
                assertEquals("${style.id}", style.containerColor and 0xFFFFFFL, got)
            }
    }

    @Test
    fun aRampEndsExactlyOnItsTwoColours() {
        val m = ColorMatrices.lumaRamp(0xFF102030L, 0xFFC0D0E0L)
        assertEquals(0x102030L, apply(m, 0xFF000000L) and 0xFFFFFFL)
        val white = apply(m, 0xFFFFFFFFL)
        for (shift in listOf(16, 8, 0)) {
            val got = ((white shr shift) and 0xFF).toInt()
            val want = ((0xC0D0E0L shr shift) and 0xFF).toInt()
            assertTrue("endpoint drift $got vs $want", abs(got - want) <= 1)
        }
    }

    @Test
    fun aRampPreservesAlphaSoAntiAliasedEdgesSurvive() {
        val m = rampFor("frost")
        assertEquals(0x40L, (apply(m, 0x40FFFFFFL) shr 24) and 0xFF)
    }

    @Test
    fun frostInkStaysDarkerThanItsContainerSoGlyphsReadAsInk() {
        val style = IconStyleCatalog.frost
        val m = rampFor("frost")
        val brightSource = ColorMatrices.relativeLuminance(apply(m, 0xFFFFFFFFL))
        val container = ColorMatrices.relativeLuminance(style.containerColor)
        assertTrue("Frost ink must be darker than its container", brightSource < container)
    }
}
