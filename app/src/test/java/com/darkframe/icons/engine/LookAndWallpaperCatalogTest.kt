package com.darkframe.icons.engine

import com.darkframe.icons.engine.domain.ColorMatrices
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
    fun aLookIsNeverHalfLocked() {
        // A Pro look must not pair a Pro collection with a free wallpaper or the reverse: the user
        // would buy half a look, or see a locked part inside something they already own.
        LookCatalog.all.forEach { look ->
            val wallpaper = WallpaperCatalog.byId(look.wallpaperId)!!
            assertEquals(
                "${look.id}: collection tier and look tier disagree",
                look.style.tier,
                look.tier,
            )
            if (look.tier == ContentTier.FREE) {
                assertEquals(
                    "${look.id}: a free look must not contain a Pro wallpaper",
                    ContentTier.FREE,
                    wallpaper.tier,
                )
            }
        }
    }

    @Test
    fun aLookUsesItsOwnCollectionsStyle() {
        LookCatalog.all.forEach { look ->
            assertSame(IconStyleCatalog.forCollection(look.collection), look.style)
        }
    }

    @Test
    fun freeLooksComeFirstSoANewUserSeesSomethingUsable() {
        val firstPro = LookCatalog.all.indexOfFirst { it.tier == ContentTier.PRO }
        val lastFree = LookCatalog.all.indexOfLast { it.tier == ContentTier.FREE }
        assertTrue("free and pro looks are interleaved", lastFree < firstPro)
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

    /**
     * The tier of a collection is the product's pricing promise, and two separate screens gate on
     * it (the look detail and the icon browser's export). Pinned here so a rename or a reshuffle of
     * the catalog cannot silently move a Pro collection into the free set or the reverse.
     */
    @Test
    fun `collection tiers match the published free and pro split`() {
        val tiers = IconStyleCatalog.all.associate { it.id to it.tier }
        assertEquals(ContentTier.FREE, tiers["noir"])
        assertEquals(ContentTier.FREE, tiers["color_pop"])
        assertEquals(ContentTier.PRO, tiers["frost"])
        assertEquals(ContentTier.PRO, tiers["titanium"])
        assertEquals(ContentTier.PRO, tiers["glass"])
        assertEquals(ContentTier.FREE, tiers["pure_amoled"])
        assertEquals(6, tiers.size)
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

    // ---- palette and layout ---------------------------------------------------------------------

    /**
     * A look's ink has to be readable on its own surface.
     *
     * The palette is shown to the user as the look's three colours and is the basis for the widget
     * and label treatment, so a look whose ink disappears into its surface would be a look that
     * cannot be used — and because the surface comes from the wallpaper rather than from the icon
     * style, the icon catalog's own contrast tests do not cover it.
     */
    @Test
    fun `every look's ink reads against its own surface`() {
        LookCatalog.all.forEach { look ->
            val ratio = ColorMatrices.contrastRatio(look.palette.surface, look.palette.ink)
            assertTrue("${look.id} ink contrast is only $ratio", ratio >= 7f)
        }
    }

    /**
     * DarkFrame is not a bright pack, and the accent is where that would slip.
     *
     * Capped on saturation rather than on hue, so a look may lean cool or warm but none of them can
     * introduce a colour that competes with the user's own app icons.
     */
    @Test
    fun `no look's accent is a bright colour`() {
        LookCatalog.all.forEach { look ->
            val accent = look.palette.accent
            val r = ((accent shr 16) and 0xFF).toInt()
            val g = ((accent shr 8) and 0xFF).toInt()
            val b = (accent and 0xFF).toInt()
            val max = maxOf(r, g, b)
            val saturation = if (max == 0) 0f else (max - minOf(r, g, b)).toFloat() / max
            assertTrue("${look.id} accent is too saturated at $saturation", saturation <= 0.35f)
        }
    }

    @Test
    fun `every look recommends a layout that One UI can actually be set to`() {
        LookCatalog.all.forEach { look ->
            // One UI's home-screen grid options span 4x5 through 5x6; the model's own range is wider
            // to leave room for other launchers, so this pins what the shipped looks ask for.
            assertTrue("${look.id} columns ${look.layout.columns}", look.layout.columns in 4..5)
            assertTrue("${look.id} rows ${look.layout.rows}", look.layout.rows in 5..6)
            assertFalse("${look.id} layout note is empty", look.layout.note.isBlank())
        }
    }

    @Test
    fun `the palette surface is drawn from the look's own wallpaper`() {
        // Not decoration: the palette is what the user judges a look by, so if it disagreed with the
        // wallpaper the preview renders, the swatches would be describing a different look.
        LookCatalog.all.forEach { look ->
            val wallpaper = WallpaperCatalog.byId(look.wallpaperId)
            assertNotNull("${look.id} has no wallpaper", wallpaper)
            val surfaceLuma = ColorMatrices.relativeLuminance(look.palette.surface)
            val wallpaperLuma = ColorMatrices.relativeLuminance(wallpaper!!.base)
            assertTrue(
                "${look.id} palette surface and wallpaper base disagree " +
                    "($surfaceLuma vs $wallpaperLuma)",
                kotlin.math.abs(surfaceLuma - wallpaperLuma) < 0.05f,
            )
        }
    }
}
