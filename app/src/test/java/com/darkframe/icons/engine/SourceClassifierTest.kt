package com.darkframe.icons.engine

import com.darkframe.icons.engine.domain.ContentBounds
import com.darkframe.icons.engine.domain.IconStyleCatalog
import com.darkframe.icons.engine.domain.SourceClassifier
import com.darkframe.icons.engine.domain.SourceShape
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceClassifierTest {

    @Test
    fun legacyOpaqueIconIsRecognisedAsFullBleed() {
        assertEquals(
            SourceShape.FULL_BLEED,
            SourceClassifier.classify(0.99f, ContentBounds(0, 0, 108, 108), 108),
        )
    }

    @Test
    fun transparentGlyphArtworkStaysAGlyph() {
        assertEquals(
            SourceShape.GLYPH,
            SourceClassifier.classify(0.42f, ContentBounds(20, 20, 88, 88), 108),
        )
    }

    @Test
    fun aSolidButSmallLogoIsAGlyphNotABackground() {
        // Dense artwork that does not span the canvas is a mark, not a baked-in tile.
        assertEquals(
            SourceShape.GLYPH,
            SourceClassifier.classify(0.97f, ContentBounds(30, 30, 78, 78), 108),
        )
    }

    @Test
    fun aFullCanvasOfSparseArtworkIsAGlyphNotABackground() {
        // Spans the canvas but is mostly transparent — a wide wordmark or a thin outline frame.
        assertEquals(
            SourceShape.GLYPH,
            SourceClassifier.classify(0.25f, ContentBounds(0, 0, 108, 108), 108),
        )
    }

    @Test
    fun degenerateInputsFallBackToGlyph() {
        assertEquals(SourceShape.GLYPH, SourceClassifier.classify(1f, ContentBounds.EMPTY, 108))
        assertEquals(SourceShape.GLYPH, SourceClassifier.classify(1f, ContentBounds(0, 0, 10, 10), 0))
    }

    @Test
    fun fullBleedArtworkIsSeatedLargerThanAGlyph() {
        IconStyleCatalog.all.forEach { style ->
            val glyph = SourceClassifier.targetFraction(SourceShape.GLYPH, style)
            val full = SourceClassifier.targetFraction(SourceShape.FULL_BLEED, style)
            assertEquals(style.glyphScale, glyph, 0.0001f)
            assertTrue("${style.id}: full-bleed must be seated larger", full > glyph)
            assertTrue("${style.id}: full-bleed must still leave a visible container margin", full < 0.88f)
        }
    }

    @Test
    fun onlyFullBleedArtworkIsCornerClipped() {
        assertTrue(SourceClassifier.requiresCornerClip(SourceShape.FULL_BLEED))
        assertFalse(SourceClassifier.requiresCornerClip(SourceShape.GLYPH))
    }

    @Test
    fun innerCornerRadiusStaysConcentricAndLegal() {
        IconStyleCatalog.all.forEach { style ->
            val inner = SourceClassifier.innerCornerRadiusRatio(style)
            assertTrue("${style.id}: inner radius must exceed the outer ratio", inner > style.cornerRadiusRatio)
            assertTrue("${style.id}: inner radius must stay a legal ratio", inner <= 0.5f)
        }
    }
}
