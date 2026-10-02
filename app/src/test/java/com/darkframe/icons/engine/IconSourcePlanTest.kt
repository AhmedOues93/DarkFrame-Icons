package com.darkframe.icons.engine

import com.darkframe.icons.engine.domain.IconSourceKind
import com.darkframe.icons.engine.domain.IconSourcePlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IconSourcePlanTest {

    @Test
    fun anAppWithNoCuratedArtworkIsThemedFromItsOwnIcon() {
        // The product's core promise: absence of curated artwork is not absence of support. An app
        // installed tomorrow takes this path.
        assertEquals(
            listOf(IconSourceKind.INSTALLED, IconSourceKind.MONOGRAM),
            IconSourcePlan.preferenceOrder(hasCuratedOverride = false),
        )
    }

    @Test
    fun curatedArtworkIsPreferredWhenItExists() {
        assertEquals(
            IconSourceKind.CURATED,
            IconSourcePlan.preferenceOrder(hasCuratedOverride = true).first(),
        )
    }

    @Test
    fun aFailedCuratedOverrideFallsBackToTheInstalledIconNotToAMonogram() {
        // A curated row naming a drawable missing from the APK must cost polish, never coverage.
        val order = IconSourcePlan.preferenceOrder(hasCuratedOverride = true)
        assertEquals(IconSourceKind.INSTALLED, order[1])
    }

    @Test
    fun everyOrderTerminatesInAMonogramSoResolutionCannotRunOut() {
        listOf(true, false).forEach { hasCurated ->
            val order = IconSourcePlan.preferenceOrder(hasCurated)
            assertEquals(IconSourceKind.MONOGRAM, order.last())
            assertTrue(order.size == order.distinct().size)
        }
    }
}
