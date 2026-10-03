package com.darkframe.icons.engine

import com.darkframe.icons.engine.domain.CompleteLook
import com.darkframe.icons.engine.domain.IconStyleCatalog
import com.darkframe.icons.engine.domain.LookApplyPart
import com.darkframe.icons.engine.domain.LookApplyPlan
import com.darkframe.icons.engine.domain.LookCatalog
import com.darkframe.icons.engine.wallpaper.WallpaperCatalog
import com.darkframe.icons.model.ContentTier
import com.darkframe.icons.model.IconCollection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class LookAndWallpaperCatalogTest {

    @Test
    fun everyCollectionHasExactlyOneLook() {
        assertEquals(IconCollection.entries.size, LookCatalog.all.size)
        IconCollection.entries.forEach { assertNotNull(LookCatalog.forCollection(it)) }
    }

    @Test
    fun everyLookPointsAtAWallpaperThatExists() {
        // A look whose wallpaper id is a typo would show an empty preview and apply nothing.
        LookCatalog.all.forEach { look ->
            assertNotNull(
                "${look.id} names a wallpaper that is not in the catalog: ${look.wallpaperId}",
                WallpaperCatalog.byId(look.wallpaperId),
            )
        }
    }

    @Test
    fun everyLookBringsWidgets() {
        LookCatalog.all.forEach { look ->
            assertTrue("${look.id} has no widgets", look.widgets.isNotEmpty())
            assertEquals(look.widgets.size, look.widgets.distinct().size)
        }
    }

    @Test
    fun everyIconLookIsFreeDuringIconFirstRelease() {
        LookCatalog.all.forEach { look ->
            assertEquals("${look.id} look must be free", ContentTier.FREE, look.tier)
            assertEquals("${look.id} icon style must be free", ContentTier.FREE, look.style.tier)
        }
    }

    @Test
    fun aLookUsesItsOwnCollectionsStyle() {
        LookCatalog.all.forEach { look ->
            assertSame(IconStyleCatalog.forCollection(look.collection), look.style)
        }
    }

    @Test
    fun allLooksAreImmediatelyUsableForIconTesting() {
        assertTrue(LookCatalog.all.all { it.tier == ContentTier.FREE })
    }

    @Test
    fun anUnknownLookIdFallsBackInsteadOfCrashing() {
        assertSame(LookCatalog.default, LookCatalog.forId("deleted"))
        assertSame(LookCatalog.default, LookCatalog.forId(null))
        assertSame(LookCatalog.frost, LookCatalog.forId("frost"))
    }

    @Test
    fun theApplyPlanAccountsForEveryPartExactlyOnce() {
        // The product rule: nothing in a look is silently skipped, and nothing is claimed to be
        // automatic that is not.
        assertEquals(LookApplyPart.entries.toSet(), LookApplyPlan.allParts())
        val overlap = LookApplyPlan.automaticParts() intersect LookApplyPlan.guidedParts()
        assertTrue("a part cannot be both automatic and guided", overlap.isEmpty())
    }

    @Test
    fun iconsAndWidgetsAreNeverClaimedAsAutomatic() {
        // Android has no API to place a widget, and icon application belongs to the launcher.
        assertFalse(LookApplyPlan.automaticParts().contains(LookApplyPart.ICONS))
        assertFalse(LookApplyPlan.automaticParts().contains(LookApplyPart.WIDGETS))
        assertTrue(LookApplyPlan.automaticParts().contains(LookApplyPart.WALLPAPER))
    }

    @Test
    fun everyWallpaperCategoryShownActuallyHasWallpapers() {
        // An empty category on a browse screen is a promise the product does not keep.
        val categories = WallpaperCatalog.categoriesWithContent()
        assertTrue(categories.isNotEmpty())
        categories.forEach {
            assertTrue("$it is listed but empty", WallpaperCatalog.inCategory(it).isNotEmpty())
        }
    }

    @Test
    fun wallpaperIdsAreUniqueAndResolvable() {
        val ids = WallpaperCatalog.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        ids.forEach { assertNotNull(WallpaperCatalog.byId(it)) }
        assertSame(null, WallpaperCatalog.byId("nope"))
    }

    @Test
    fun everyCategoryOffersSomethingFree() {
        // A category where everything is locked reads as a paywall rather than a collection.
        WallpaperCatalog.categoriesWithContent().forEach { category ->
            val specs = WallpaperCatalog.inCategory(category)
            assertTrue(
                "$category is entirely Pro",
                specs.any { it.tier == ContentTier.FREE },
            )
        }
    }

    @Test
    fun aLooksTaglineDescribesTheLookNotTheMechanism() {
        val mechanismWords = listOf("theme park", "launcher", "icon pack", "shortcut", "apply")
        LookCatalog.all.forEach { look: CompleteLook ->
            assertTrue("${look.id} has no tagline", look.tagline.isNotBlank())
            mechanismWords.forEach { word ->
                assertFalse(
                    "${look.id} tagline talks about mechanism: ${look.tagline}",
                    look.tagline.lowercase().contains(word),
                )
            }
        }
    }

    @Test
    fun `all six icon collections stay free`() {
        assertEquals(6, IconStyleCatalog.all.size)
        IconStyleCatalog.all.forEach { style ->
            assertEquals(style.id, ContentTier.FREE, style.tier)
        }
    }

    /**
     * A look is sold as its collection: a free look whose icons are Pro (or a Pro look over free
     * icons) would make one of the two gates lie about the other.
     */
    @Test
    fun `every look carries the tier of its collection`() {
        LookCatalog.all.forEach { look ->
            assertEquals(look.id, look.tier, look.style.tier)
        }
    }
}
