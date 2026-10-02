package com.darkframe.icons.engine

import com.darkframe.icons.engine.domain.IconStyleCatalog
import org.junit.Assert.assertEquals
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
}
