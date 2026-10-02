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

    /**
     * Full-bleed artwork is framed by the collection, not nested inside it.
     *
     * The margin used to be 22% of the tile, which is what a "double background" looks like: the
     * app's own tile clearly visible inside DarkFrame's. The frame is now thin enough to read as
     * one tile with an edge — which also means every tile in a grid is the same size, whether the
     * app shipped a glyph or a pre-adaptive square.
     */
    @Test
    fun fullBleedArtworkIsFramedRatherThanNested() {
        IconStyleCatalog.all.forEach { style ->
            val glyph = SourceClassifier.targetFraction(SourceShape.GLYPH, style)
            val full = SourceClassifier.targetFraction(SourceShape.FULL_BLEED, style)
            assertEquals(style.glyphScale, glyph, 0.0001f)
            assertTrue("${style.id}: full-bleed must be seated larger", full > glyph)
            assertTrue("${style.id}: the frame must stay visible", full < 0.97f)
            assertTrue("${style.id}: the frame must be a frame, not an inset tile", full > 0.9f)
        }
    }

    @Test
    fun onlyGlyphsAreAreaCompensated() {
        // Full-bleed artwork fills its own box by definition, so compensating it would only break
        // the one thing the frame treatment buys: every tile the same size.
        assertTrue(SourceClassifier.allowsAreaCompensation(SourceShape.GLYPH))
        assertFalse(SourceClassifier.allowsAreaCompensation(SourceShape.FULL_BLEED))
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
