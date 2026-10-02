package com.darkframe.icons.engine

import com.darkframe.icons.engine.domain.ContentBounds
import com.darkframe.icons.engine.domain.IconNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IconNormalizerTest {

    @Test
    fun squareContentFillsTargetFractionExactly() {
        val placement = IconNormalizer.place(
            content = ContentBounds(0, 0, 100, 100),
            sourceWidth = 100,
            sourceHeight = 100,
            canvasSize = 200,
            targetFraction = 0.5f,
        )
        assertEquals(100f, placement.width, 0.01f)
        assertEquals(100f, placement.height, 0.01f)
        assertEquals(50f, placement.left, 0.01f)
        assertEquals(50f, placement.top, 0.01f)
    }

    @Test
    fun paddedSourceIsScaledByItsContentNotItsCanvas() {
        // A typical adaptive foreground: 40px of artwork floating in a 108px canvas. Scaling by
        // the canvas would leave it ~37% smaller than a full-bleed icon beside it.
        val placement = IconNormalizer.place(
            content = ContentBounds(34, 34, 74, 74),
            sourceWidth = 108,
            sourceHeight = 108,
            canvasSize = 100,
            targetFraction = 0.6f,
        )
        assertEquals(60f, placement.width, 0.01f)
        assertEquals(1.5f, placement.scale, 0.01f)
    }

    @Test
    fun upscaleIsCappedForTinyContent() {
        val placement = IconNormalizer.place(
            content = ContentBounds(0, 0, 4, 4),
            sourceWidth = 108,
            sourceHeight = 108,
            canvasSize = 192,
            targetFraction = 0.6f,
        )
        assertEquals(IconNormalizer.MAX_UPSCALE, placement.scale, 0.001f)
        assertTrue("capped content must stay smaller than target", placement.width < 192 * 0.6f)
    }

    @Test
    fun nonSquareContentKeepsAspectRatioAndStaysCentred() {
        val placement = IconNormalizer.place(
            content = ContentBounds(0, 0, 100, 50),
            sourceWidth = 100,
            sourceHeight = 100,
            canvasSize = 200,
            targetFraction = 0.5f,
        )
        assertEquals(100f, placement.width, 0.01f)
        assertEquals(50f, placement.height, 0.01f)
        assertEquals(placement.left, 200f - placement.right, 0.01f)
        assertEquals(placement.top, 200f - placement.bottom, 0.01f)
    }

    @Test
    fun extremeAspectRatioIsFittedSlightlyTighter() {
        val wordmark = IconNormalizer.place(
            content = ContentBounds(0, 0, 100, 20),
            sourceWidth = 100, sourceHeight = 100, canvasSize = 200, targetFraction = 0.6f,
        )
        val normal = IconNormalizer.place(
            content = ContentBounds(0, 0, 100, 80),
            sourceWidth = 100, sourceHeight = 100, canvasSize = 200, targetFraction = 0.6f,
        )
        assertTrue("wordmark should not span as wide as a normal icon", wordmark.width < normal.width)
    }

    @Test
    fun fullyTransparentSourceFallsBackToSourceCanvas() {
        val placement = IconNormalizer.place(
            content = ContentBounds.EMPTY,
            sourceWidth = 80,
            sourceHeight = 80,
            canvasSize = 160,
            targetFraction = 0.5f,
        )
        assertEquals(80f, placement.width, 0.01f)
        assertEquals(80f, placement.height, 0.01f)
    }

    @Test
    fun unknownSourceSizeStillProducesAUsablePlacement() {
        val placement = IconNormalizer.place(
            content = ContentBounds.EMPTY,
            sourceWidth = 0,
            sourceHeight = 0,
            canvasSize = 100,
            targetFraction = 0.5f,
        )
        assertEquals(50f, placement.width, 0.01f)
        assertTrue(placement.left >= 0f)
    }

    @Test
    fun opticalLiftRaisesContentWithoutResizingIt() {
        val flat = IconNormalizer.place(
            ContentBounds(0, 0, 100, 100), 100, 100, 200, 0.5f, opticalLiftRatio = 0f,
        )
        val lifted = IconNormalizer.place(
            ContentBounds(0, 0, 100, 100), 100, 100, 200, 0.5f, opticalLiftRatio = 0.02f,
        )
        assertEquals(flat.width, lifted.width, 0.01f)
        assertEquals(flat.top - 4f, lifted.top, 0.01f)
    }

    @Test
    fun adaptiveCropMatchesThePlatformVisibleViewport() {
        // 108dp canvas, 72dp guaranteed visible => 18/108 of each edge is bleed.
        assertEquals(18, IconNormalizer.adaptiveCropInset(108))
        assertEquals(32, IconNormalizer.adaptiveCropInset(192))
    }
}
