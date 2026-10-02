package com.darkframe.icons.engine

import com.darkframe.icons.engine.domain.ColorMatrices
import com.darkframe.icons.engine.domain.GlyphMode
import com.darkframe.icons.engine.domain.IconStyleCatalog
import com.darkframe.icons.engine.domain.SurfaceFinish
import com.darkframe.icons.model.IconCollection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class IconStyleCatalogTest {

    @Test
    fun everyCollectionHasExactlyOneStyle() {
        assertEquals(IconCollection.entries.size, IconStyleCatalog.all.size)
        IconCollection.entries.forEach { assertNotNull(IconStyleCatalog.forCollection(it)) }
    }

    @Test
    fun styleIdsAreUniqueAndStable() {
        val ids = IconStyleCatalog.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        // Ids are persisted in user preferences and in cache paths; renaming one silently
        // invalidates saved state, so they are pinned here on purpose.
        assertEquals(
            listOf("noir", "color_pop", "frost", "titanium", "glass", "pure_amoled"),
            ids,
        )
    }

    @Test
    fun unknownStyleIdFallsBackInsteadOfCrashing() {
        assertSame(IconStyleCatalog.default, IconStyleCatalog.forId("deleted_style"))
        assertSame(IconStyleCatalog.default, IconStyleCatalog.forId(null))
        assertSame(IconStyleCatalog.frost, IconStyleCatalog.forId("frost"))
    }

    @Test
    fun allCollectionsShareOneOpticalGrid() {
        IconStyleCatalog.all.forEach { style ->
            assertEquals(
                "${style.id} must share DarkFrame's corner geometry",
                IconStyleCatalog.CORNER_RADIUS_RATIO,
                style.cornerRadiusRatio,
                0.0001f,
            )
            assertTrue(
                "${style.id} glyph size must stay inside the shared optical band",
                style.glyphScale in 0.48f..0.60f,
            )
        }
    }

    @Test
    fun tintedCollectionsSeparateGlyphFromContainer() {
        IconStyleCatalog.all
            .filter { it.glyphMode == GlyphMode.TINT }
            .forEach { style ->
                val contrast = ColorMatrices.contrastRatio(style.glyphTint, style.containerColor)
                assertTrue(
                    "${style.id} glyph/container contrast too low: $contrast",
                    contrast >= 7f,
                )
            }
    }

    @Test
    fun pureAmoledIsTrueBlackAndKeylineFree() {
        val amoled = IconStyleCatalog.pureAmoled
        assertEquals(0xFF000000L, amoled.containerColor)
        assertEquals(0xFF000000L, amoled.containerEndColor)
        assertEquals(SurfaceFinish.FLAT, amoled.finish)
        assertEquals("keyline must be fully transparent", 0L, amoled.keylineColor shr 24 and 0xFF)
        assertEquals(0f, amoled.keylineWidthRatio, 0f)
    }

    @Test
    fun frostIsTheLightCollectionAndEveryOtherOneIsDark() {
        val frostLuminance = ColorMatrices.relativeLuminance(IconStyleCatalog.frost.containerColor)
        assertTrue("Frost container must be light", frostLuminance > 0.8f)
        IconStyleCatalog.all
            .filter { it.id != "frost" }
            .forEach {
                assertTrue(
                    "${it.id} container must stay dark",
                    ColorMatrices.relativeLuminance(it.containerColor) < 0.2f,
                )
            }
    }

    @Test
    fun colorPopPreservesBrandColourWithoutLettingItGoNeon() {
        val style = IconStyleCatalog.colorPop
        assertEquals(GlyphMode.PRESERVE, style.glyphMode)
        assertTrue("must stay recognisably branded", style.glyphSaturation > 0.85f)
        assertTrue("must not boost saturation", style.glyphSaturation <= 1f)
    }

    @Test
    fun titaniumIsANeutralSweepWithNoColourCast() {
        val style = IconStyleCatalog.titanium
        assertEquals(SurfaceFinish.METALLIC, style.finish)
        assertTrue("metallic needs two distinct stops", style.containerColor != style.containerEndColor)
        // "Without cheap gradients": the two stops must stay close together, and both must be
        // neutral greys rather than tinted metal.
        val delta = ColorMatrices.relativeLuminance(style.containerEndColor) -
            ColorMatrices.relativeLuminance(style.containerColor)
        assertTrue("sweep must stay subtle, was $delta", delta in 0.01f..0.2f)
        listOf(style.containerColor, style.containerEndColor).forEach { colour ->
            val r = (colour shr 16) and 0xFF
            val g = (colour shr 8) and 0xFF
            val b = colour and 0xFF
            val spread = maxOf(r, g, b) - minOf(r, g, b)
            assertTrue("titanium stop $colour has a colour cast (spread $spread)", spread <= 12)
        }
    }

    @Test
    fun glassIsTranslucentButStillReadable() {
        val style = IconStyleCatalog.glass
        assertEquals(SurfaceFinish.GLASS, style.finish)
        val alpha = (style.containerColor shr 24) and 0xFF
        assertTrue("glass must be translucent, not opaque", alpha < 0xFF)
        assertTrue("glass must not be so sheer that glyphs lose their ground", alpha > 0x99)
    }

    @Test
    fun monochromeLayerIsOnlyPreferredWhereItMakesSense() {
        IconStyleCatalog.all.forEach { style ->
            if (style.preferMonochromeLayer) {
                assertEquals(
                    "${style.id} cannot prefer a monochrome layer while preserving colour",
                    GlyphMode.TINT,
                    style.glyphMode,
                )
            }
        }
    }
}
