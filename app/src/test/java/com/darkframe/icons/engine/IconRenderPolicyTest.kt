package com.darkframe.icons.engine

import com.darkframe.icons.engine.domain.IconRenderPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IconRenderPolicyTest {

    @Test
    fun browsingNeverRequestsExportResolution() {
        // The release blocker: a grid must not produce 512px bitmaps for every installed app.
        listOf(1, 60, 180, 228, 400, 512, 4096).forEach { requested ->
            val size = IconRenderPolicy.previewSizePx(requested)
            assertTrue(
                "preview of $requested resolved to $size, above the preview ceiling",
                size <= IconRenderPolicy.MAX_PREVIEW_PX,
            )
        }
    }

    @Test
    fun aCellSizeRoundsUpSoIconsAreNeverUpscaled() {
        assertEquals(48, IconRenderPolicy.previewSizePx(40))
        assertEquals(64, IconRenderPolicy.previewSizePx(49))
        assertEquals(96, IconRenderPolicy.previewSizePx(96))
        assertEquals(128, IconRenderPolicy.previewSizePx(100))
        assertEquals(192, IconRenderPolicy.previewSizePx(129))
    }

    @Test
    fun foldingDoesNotTriggerARegenerationStorm() {
        // A Fold8's grid cell grows when the inner screen opens. Both sizes must land in the same
        // bucket, so the already-rendered icons stay valid.
        val folded = 180
        val unfolded = 190
        assertEquals(
            IconRenderPolicy.previewSizePx(folded),
            IconRenderPolicy.previewSizePx(unfolded),
        )
        assertFalse(IconRenderPolicy.requiresRerender(folded, unfolded))
    }

    @Test
    fun aGenuinelyDifferentCellSizeStillRerenders() {
        assertTrue(IconRenderPolicy.requiresRerender(60, 180))
    }

    @Test
    fun theWorkBufferScalesWithTheTargetInsteadOfBeingFixed() {
        // The old pipeline rasterised every source at 288px whatever the output, which is what
        // allocated hundreds of megabytes while scrolling.
        assertTrue(IconRenderPolicy.workSizePx(64) < IconRenderPolicy.MAX_WORK_PX)
        assertTrue(IconRenderPolicy.workSizePx(64) < IconRenderPolicy.workSizePx(192))
    }

    @Test
    fun theWorkBufferStaysWithinItsBounds() {
        listOf(0, 1, 48, 192, 512, 4096).forEach { target ->
            val work = IconRenderPolicy.workSizePx(target)
            assertTrue("$target -> $work below the floor", work >= IconRenderPolicy.MIN_WORK_PX)
            assertTrue("$target -> $work above the ceiling", work <= IconRenderPolicy.MAX_WORK_PX)
        }
    }

    @Test
    fun theWorkBufferLeavesHeadroomOverTheTarget() {
        listOf(96, 128).forEach { target ->
            assertTrue(IconRenderPolicy.workSizePx(target) > target)
        }
    }

    @Test
    fun renderConcurrencyStaysWellBelowTheCoreCount() {
        // Saturating every core with bitmap work is what made the device warm.
        assertEquals(2, IconRenderPolicy.maxParallelRenders(8))
        assertEquals(2, IconRenderPolicy.maxParallelRenders(16))
        assertEquals(1, IconRenderPolicy.maxParallelRenders(1))
        assertTrue(IconRenderPolicy.maxParallelRenders(0) >= 1)
    }

    @Test
    fun exportResolutionIsRecognisedAsSuchAndIsNotAPreview() {
        assertTrue(IconRenderPolicy.isExportSize(IconRenderPolicy.EXPORT_PX))
        assertFalse(IconRenderPolicy.isExportSize(IconRenderPolicy.MAX_PREVIEW_PX))
    }
}
