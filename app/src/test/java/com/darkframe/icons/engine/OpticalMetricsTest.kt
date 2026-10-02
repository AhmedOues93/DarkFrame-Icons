package com.darkframe.icons.engine

import com.darkframe.icons.engine.domain.ColorMatrices
import com.darkframe.icons.engine.domain.ContentBounds
import com.darkframe.icons.engine.domain.IconNormalizer
import com.darkframe.icons.engine.domain.IconStyleCatalog
import com.darkframe.icons.engine.domain.OpticalMetrics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs

/**
 * The optical corrections, pinned against the real source cases they exist for.
 *
 * These are the rules that decide whether a grid of two hundred unrelated apps looks like one pack,
 * so each test is written as the case it protects rather than as a numeric assertion: a circular
 * logo, a thin wordmark, an off-centre glyph, an app whose artwork sits exactly on the container's
 * own luminance.
 */
class OpticalMetricsTest {

    // ---- area -----------------------------------------------------------------------------------

    @Test
    fun `a circular logo is enlarged to match a square one`() {
        // A circle inscribed in its bounding box covers pi/4 of it.
        val circleFill = (PI / 4).toFloat()
        val compensation = OpticalMetrics.areaCompensation(circleFill)

        // 1/sqrt(pi/4) = 1.128. Android's own keyline grid uses 176/152 = 1.158 for the same job,
        // so this lands just inside the platform's convention rather than inventing one.
        assertTrue("circle compensation was $compensation", abs(compensation - 1.128f) < 0.01f)
    }

    @Test
    fun `a solid square logo is left alone`() {
        assertEquals(1f, OpticalMetrics.areaCompensation(1f), 0.0001f)
        // Anti-aliased edges and the scanner's one-stride padding keep even a solid shape short of
        // a perfect 1.0, so the solid band has to have some width to it.
        assertEquals(1f, OpticalMetrics.areaCompensation(0.96f), 0.0001f)
    }

    @Test
    fun `a hairline wordmark is not blown up to match a solid shape`() {
        // Thin strokes with a lot of space between them: equalising ink area here would run the
        // mark out of the tile, so the correction is capped instead.
        val sparse = OpticalMetrics.areaCompensation(0.08f)
        assertEquals(OpticalMetrics.MAX_AREA_COMPENSATION, sparse, 0.0001f)
        assertTrue("the cap must stay close to the circle case", sparse < 1.2f)
    }

    @Test
    fun `an empty scan asks for no compensation`() {
        assertEquals(1f, OpticalMetrics.areaCompensation(0f), 0.0001f)
    }

    // ---- centring -------------------------------------------------------------------------------

    @Test
    fun `a symmetric glyph is not moved`() {
        val shift = OpticalMetrics.centringShift(
            boxCentre = 50f,
            massCentre = 50f,
            extentPx = 120f,
            sourceExtent = 100f,
        )
        assertEquals(0f, shift, 0.0001f)
    }

    @Test
    fun `a glyph whose mass sits left of its box is nudged right`() {
        // A play triangle: its bounding box is centred but its ink leans towards the flat edge.
        val shift = OpticalMetrics.centringShift(
            boxCentre = 50f,
            massCentre = 44f,
            extentPx = 120f,
            sourceExtent = 100f,
        )
        assertTrue("must move away from the heavy side, was $shift", shift > 0f)
        // Partial on purpose: full centroid alignment over-corrects shapes with one heavy limb.
        val full = 0.06f * 120f
        assertTrue("must not fully centroid-align, was $shift of $full", shift < full)
    }

    @Test
    fun `one stray pixel cannot drag a glyph off the tile`() {
        val shift = OpticalMetrics.centringShift(
            boxCentre = 50f,
            massCentre = 5f,
            extentPx = 120f,
            sourceExtent = 100f,
        )
        val cap = OpticalMetrics.MAX_CENTROID_SHIFT * OpticalMetrics.CENTROID_BLEND * 120f
        assertEquals(cap, shift, 0.0001f)
    }

    // ---- separation -----------------------------------------------------------------------------

    @Test
    fun `an ordinary icon is not touched`() {
        // Mid-bright artwork on Color Pop's graphite: plenty of separation already.
        val lift = OpticalMetrics.separationLift(sourceLuma = 0.6f, containerLuma = 0.09f)
        assertEquals(0f, lift, 0.0001f)
    }

    @Test
    fun `a very dark icon on a dark container is lifted clear of it`() {
        val container = ColorMatrices.gammaLuma(IconStyleCatalog.glass.containerColor)
        val lift = OpticalMetrics.separationLift(sourceLuma = container + 0.02f, containerLuma = container)
        assertTrue("a dark-on-dark icon must be lifted, got $lift", lift > 0f)
        val separated = abs((container + 0.02f + lift) - container)
        assertTrue("must reach the floor, got $separated", separated >= OpticalMetrics.MIN_SEPARATION - 0.001f)
    }

    @Test
    fun `a very bright icon on a light container is cut rather than lifted`() {
        val container = ColorMatrices.gammaLuma(IconStyleCatalog.frost.containerColor)
        val lift = OpticalMetrics.separationLift(sourceLuma = container, containerLuma = container)
        assertTrue("a light container must push the source down, got $lift", lift < 0f)
    }

    @Test
    fun `separation never blows out to flat white or crushes to flat black`() {
        for (step in 0..100) {
            val container = step / 100f
            val lifted = container + OpticalMetrics.separationLift(container, container)
            assertTrue(
                "container $container produced $lifted",
                lifted in OpticalMetrics.MIN_LIFTED_LUMA..OpticalMetrics.MAX_LIFTED_LUMA,
            )
        }
    }

    // ---- vibrancy -------------------------------------------------------------------------------

    @Test
    fun `a muted brand palette keeps all of its identity`() {
        // The whole point of Color Pop: a carefully chosen muted palette needed no help, and a
        // global desaturation is exactly what would take its character out.
        val base = IconStyleCatalog.colorPop.glyphSaturation
        assertEquals(1f, OpticalMetrics.vibrancy(0.1f, base), 0.0001f)
    }

    @Test
    fun `neon artwork gets the full pull-back`() {
        val base = IconStyleCatalog.colorPop.glyphSaturation
        assertEquals(base, OpticalMetrics.vibrancy(0.95f, base), 0.0001f)
    }

    @Test
    fun `vibrancy is monotonic and never boosts saturation`() {
        val base = IconStyleCatalog.colorPop.glyphSaturation
        var previous = Float.MAX_VALUE
        for (step in 0..100) {
            val value = OpticalMetrics.vibrancy(step / 100f, base)
            assertTrue("must never boost, got $value", value <= 1f)
            assertTrue("must never drop below the style's own value", value >= base)
            assertTrue("must not rise with saturation", value <= previous + 0.0001f)
            previous = value
        }
    }

    @Test
    fun `a style with no pull-back is a no-op`() {
        assertEquals(1f, OpticalMetrics.vibrancy(0.9f, 1f), 0.0001f)
    }

    // ---- the corrections together ----------------------------------------------------------------

    @Test
    fun `a circle and a square end up the same apparent size`() {
        val canvas = 192
        val target = 0.56f

        // Both occupy the same 100x100 content box; only their fill differs.
        val box = ContentBounds(0, 0, 100, 100)
        val square = IconNormalizer.place(
            content = box,
            sourceWidth = 100,
            sourceHeight = 100,
            canvasSize = canvas,
            targetFraction = target,
            areaCompensation = OpticalMetrics.areaCompensation(1f),
        )
        val circle = IconNormalizer.place(
            content = box,
            sourceWidth = 100,
            sourceHeight = 100,
            canvasSize = canvas,
            targetFraction = target,
            areaCompensation = OpticalMetrics.areaCompensation((PI / 4).toFloat()),
        )

        assertTrue("the circle must be seated larger", circle.width > square.width)
        // Equal ink area is the goal, so compare areas rather than edges: the circle's own area is
        // pi/4 of the box it is drawn into.
        val squareInk = square.width * square.height
        val circleInk = circle.width * circle.height * (PI / 4).toFloat()
        val ratio = circleInk / squareInk
        assertTrue("ink areas should land within 10%, ratio was $ratio", abs(ratio - 1f) < 0.1f)
    }

    @Test
    fun `area compensation still respects the upscale cap`() {
        // A 20px glyph in a 108px canvas already wants more than the cap allows; compensation must
        // not become a way around it, or tiny artwork comes out visibly soft.
        val placement = IconNormalizer.place(
            content = ContentBounds(44, 44, 64, 64),
            sourceWidth = 108,
            sourceHeight = 108,
            canvasSize = 108,
            targetFraction = 0.56f,
            areaCompensation = OpticalMetrics.MAX_AREA_COMPENSATION,
        )
        assertEquals(IconNormalizer.MAX_UPSCALE, placement.scale, 0.0001f)
    }
}
