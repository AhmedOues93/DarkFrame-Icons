package com.darkframe.icons.engine

import com.darkframe.icons.engine.domain.IconStyleCatalog
import com.darkframe.icons.engine.domain.LookCatalog
import com.darkframe.icons.model.WidgetKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Widgets are drawn by the launcher in another process and so cannot share the engine's renderer;
 * they carry one background drawable per collection instead. That mapping is by hand, so this pins
 * it: adding a collection without adding its widget background would silently fall back to Noir,
 * and the clock would stop matching the icons beside it.
 */
class WidgetCoverageTest {

    @Test
    fun everyCollectionHasAWidgetBackground() {
        // Mirrors the branches in WidgetTheme.backgroundFor and the widget_bg_* drawables.
        val expected = setOf("noir", "color_pop", "frost", "titanium", "glass", "pure_amoled")
        assertEquals(
            "a collection was added or renamed without updating WidgetTheme and widget_bg_*",
            expected,
            IconStyleCatalog.all.map { it.id }.toSet(),
        )
    }

    /**
     * Every widget shape a look can recommend has to be a widget someone can actually add.
     *
     * [WidgetKind] is what [com.darkframe.icons.engine.domain.LookCatalog] recommends from, so a
     * kind with no provider behind it would have a look telling the user to add a widget that does
     * not exist — a fake feature, visible on the one screen whose whole job is being honest about
     * what Android lets DarkFrame do.
     */
    @Test
    fun everyRecommendableWidgetKindHasAProvider() {
        // Mirrors the receivers in AndroidManifest.xml and the rows on WidgetsActivity.
        val shipped = setOf(
            WidgetKind.DIGITAL_CLOCK,
            WidgetKind.ANALOG_CLOCK,
            WidgetKind.DATE,
            WidgetKind.CALENDAR,
            WidgetKind.BATTERY,
            WidgetKind.INFO,
        )
        assertEquals(
            "a WidgetKind was added without a provider, a layout and a row on the Widgets screen",
            shipped,
            WidgetKind.entries.toSet(),
        )
    }

    @Test
    fun everyRecommendedWidgetIsOneThatExists() {
        LookCatalog.all.forEach { look ->
            look.widgets.forEach { kind ->
                assertTrue(
                    "${look.id} recommends $kind, which has no provider",
                    kind in WidgetKind.entries,
                )
            }
        }
    }
}
