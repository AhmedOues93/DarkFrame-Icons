package com.darkframe.icons.engine

import com.darkframe.icons.engine.domain.ColorMatrices
import com.darkframe.icons.engine.domain.GlyphMode
import com.darkframe.icons.engine.domain.IconStyle
import com.darkframe.icons.engine.domain.IconStyleCatalog
import com.darkframe.icons.engine.domain.SurfaceFinish
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Each collection has to be nameable from one icon.
 *
 * That is the commercial requirement behind "six collections" rather than "one renderer with six
 * colour constants", and it is the thing a palette edit breaks silently. These tests pin the
 * properties that make the six distinguishable — and, for the contrast gain, pin the limit on what
 * a gain is allowed to do to a collection's own ink.
 */
class CollectionIdentityTest {

    /**
     * How far the contrast gain may move an endpoint of the ramp, out of 255.
     *
     * The gain works by overshooting [low] and [high] and letting the channel clamp turn the
     * overshoot into a toe and a shoulder. That only leaves the collection's palette intact where
     * its ink and surface sit at or near the channel extremes. 16/255 is about 6%: enough for the
     * near-extreme collections to get a real shoulder, tight enough that a collection whose ink
     * sits in the middle of the range cannot quietly be given a gain that darkens it.
     */
    private val endpointTolerance = 16

    @Test
    fun `no contrast gain moves a collection's ink or surface`() {
        IconStyleCatalog.all.filter { it.glyphMode == GlyphMode.TINT }.forEach { style ->
            val surface = style.containerColor or 0xFF000000L
            assertChannelsWithin(style, rampAt(style, 0f), surface, "surface")
            assertChannelsWithin(style, rampAt(style, 1f), style.glyphTint, "ink")
        }
    }

    @Test
    fun `the near-extreme collections actually get a shoulder`() {
        // Not a cosmetic assertion: without the gain the two monochrome collections are the same
        // linear desaturate, which is exactly the "generic grayscale filter" look to avoid.
        listOf(IconStyleCatalog.noir, IconStyleCatalog.pureAmoled).forEach { style ->
            assertTrue(
                "${style.id} needs a gain to read as designed rather than filtered",
                style.glyphContrast > 1.05f,
            )
        }
    }

    @Test
    fun `frost is left linear because its palette cannot survive a gain`() {
        // Frost's ink is graphite and its surface off-white: both well inside the channel range, so
        // an overshoot has nothing to clamp against and simply darkens the ink.
        assertEquals(1f, IconStyleCatalog.frost.glyphContrast, 0.0001f)
    }

    @Test
    fun `noir and pure amoled are not the same collection twice`() {
        val noir = IconStyleCatalog.noir
        val amoled = IconStyleCatalog.pureAmoled

        assertNotEquals("surfaces must differ", noir.containerColor, amoled.containerColor)
        assertNotEquals("ink must differ", noir.glyphTint, amoled.glyphTint)
        assertTrue("amoled must be the punchier of the two", amoled.glyphContrast > noir.glyphContrast)
        assertTrue("noir keeps a keyline", (noir.keylineColor ushr 24) > 0L)
        assertEquals("amoled cannot have one", 0L, amoled.keylineColor ushr 24)
        assertEquals("amoled must be true black", 0xFF000000L, amoled.containerColor)
    }

    @Test
    fun `every surface finish is used by exactly one collection`() {
        // A finish shared by two collections would mean two collections with the same material
        // identity, which is the failure this whole test class exists to catch.
        SurfaceFinish.entries.filter { it != SurfaceFinish.FLAT }.forEach { finish ->
            val owners = IconStyleCatalog.all.filter { it.finish == finish }
            assertEquals("$finish must belong to one collection, found $owners", 1, owners.size)
        }
    }

    @Test
    fun `frost has a surface of its own rather than being an inverted noir`() {
        val frost = IconStyleCatalog.frost
        assertEquals(SurfaceFinish.FROSTED, frost.finish)
        assertNotEquals("the bloom needs two stops", frost.containerColor, frost.containerEndColor)
        val delta = ColorMatrices.relativeLuminance(frost.containerEndColor) -
            ColorMatrices.relativeLuminance(frost.containerColor)
        assertTrue("the bloom must lighten upwards, was $delta", delta > 0f)
        assertTrue("and must stay subtle, was $delta", delta < 0.1f)
    }

    @Test
    fun `the colour-preserving collections keep their ground dark enough to read against`() {
        // Color Pop and Glass draw real brand colours, which only stay legible on a ground that is
        // clearly darker than the artwork it carries.
        IconStyleCatalog.all.filter { it.glyphMode == GlyphMode.PRESERVE }.forEach { style ->
            val luma = ColorMatrices.relativeLuminance(style.containerColor)
            assertTrue("${style.id} ground is too light at $luma", luma < 0.1f)
        }
    }

    /** Where the ramp lands for a source at [luma], per channel, after the matrix's own clamp. */
    private fun rampAt(style: IconStyle, luma: Float): IntArray {
        val matrix = ColorMatrices.lumaRamp(
            low = style.containerColor or 0xFF000000L,
            high = style.glyphTint,
            contrast = style.glyphContrast,
        )
        // A grey source of the requested luma, so the three luma coefficients sum to that luma.
        val channel = luma * 255f
        return IntArray(3) { row ->
            val base = row * 5
            val value = matrix[base] * channel + matrix[base + 1] * channel +
                matrix[base + 2] * channel + matrix[base + 4]
            value.roundToInt().coerceIn(0, 255)
        }
    }

    private fun assertChannelsWithin(style: IconStyle, actual: IntArray, expected: Long, what: String) {
        val channels = intArrayOf(
            ((expected shr 16) and 0xFF).toInt(),
            ((expected shr 8) and 0xFF).toInt(),
            (expected and 0xFF).toInt(),
        )
        channels.forEachIndexed { index, want ->
            val drift = abs(actual[index] - want)
            assertTrue(
                "${style.id} $what channel $index drifted $drift (${actual[index]} vs $want)",
                drift <= endpointTolerance,
            )
        }
    }
}
