package com.darkframe.icons.engine

import com.darkframe.icons.engine.domain.AppCatalogBuilder
import com.darkframe.icons.engine.domain.AppIdentity
import com.darkframe.icons.engine.domain.RawLauncherEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppCatalogBuilderTest {

    private fun entry(
        pkg: String,
        activity: String = "$pkg.Main",
        label: String = pkg,
        stamp: Long = 1L,
        work: Boolean = false,
    ) = RawLauncherEntry(pkg, activity, label, stamp, work)

    @Test
    fun darkFrameDoesNotThemeItself() {
        val result = AppCatalogBuilder.build(
            listOf(entry("com.darkframe.icons"), entry("com.other.app")),
            selfPackage = "com.darkframe.icons",
        )
        assertEquals(listOf("com.other.app"), result.map { it.packageName })
    }

    @Test
    fun duplicateComponentsAreCollapsed() {
        val result = AppCatalogBuilder.build(
            listOf(entry("com.a"), entry("com.a"), entry("com.a")),
            selfPackage = "self",
        )
        assertEquals(1, result.size)
    }

    @Test
    fun theSameComponentInAWorkProfileIsKeptSeparately() {
        val result = AppCatalogBuilder.build(
            listOf(entry("com.a"), entry("com.a", work = true)),
            selfPackage = "self",
        )
        assertEquals(2, result.size)
        assertEquals(1, result.count { it.isWorkProfile })
    }

    @Test
    fun multipleLauncherActivitiesAreAllKeptAndDisambiguated() {
        val result = AppCatalogBuilder.build(
            listOf(
                entry("com.cam", "com.cam.Photo", "Camera"),
                entry("com.cam", "com.cam.Video", "Camera"),
            ),
            selfPackage = "self",
        )
        assertEquals(2, result.size)
        assertEquals(1, result.count { it.isSecondaryEntryPoint })
        val labels = result.map { it.displayLabel() }
        assertEquals(labels.size, labels.toSet().size)
        assertTrue(labels.contains("Camera"))
        assertTrue(labels.any { it.startsWith("Camera · ") })
    }

    @Test
    fun aSingleLauncherActivityIsNeverMarkedSecondary() {
        val result = AppCatalogBuilder.build(listOf(entry("com.a")), selfPackage = "self")
        assertFalse(result.single().isSecondaryEntryPoint)
        assertEquals("com.a", result.single().displayLabel())
    }

    @Test
    fun whichEntryIsPrimaryIsDeterministicAcrossPlatformOrdering() {
        val forwards = listOf(
            entry("com.cam", "com.cam.APhoto", "Camera"),
            entry("com.cam", "com.cam.ZVideo", "Camera"),
        )
        val primaryOf = { entries: List<RawLauncherEntry> ->
            AppCatalogBuilder.build(entries, "self").first { !it.isSecondaryEntryPoint }.activityName
        }
        assertEquals(primaryOf(forwards), primaryOf(forwards.reversed()))
    }

    @Test
    fun blankLabelFallsBackToPackageNameSoTheRowStaysSearchable() {
        val result = AppCatalogBuilder.build(
            listOf(entry("com.mystery", label = "   ")),
            selfPackage = "self",
        )
        assertEquals("com.mystery", result.single().label)
    }

    @Test
    fun malformedEntriesAreDropped() {
        val result = AppCatalogBuilder.build(
            listOf(entry("", "x"), entry("com.a", ""), entry("com.good")),
            selfPackage = "self",
        )
        assertEquals(listOf("com.good"), result.map { it.packageName })
    }

    @Test
    fun catalogIsSortedCaseInsensitivelyByLabel() {
        val result = AppCatalogBuilder.build(
            listOf(
                entry("com.c", label = "zebra"),
                entry("com.a", label = "Apple"),
                entry("com.b", label = "banana"),
            ),
            selfPackage = "self",
        )
        assertEquals(listOf("Apple", "banana", "zebra"), result.map { it.label })
    }

    @Test
    fun searchMatchesLabelAndPackageName() {
        val catalog = AppCatalogBuilder.build(
            listOf(
                entry("com.spotify.music", label = "Spotify"),
                entry("com.zhiliaoapp.musically", label = "TikTok"),
            ),
            selfPackage = "self",
        )
        assertEquals(1, AppCatalogBuilder.filter(catalog, "spot").size)
        // Found by package name even though the label says nothing about it.
        assertEquals(1, AppCatalogBuilder.filter(catalog, "zhiliao").size)
        assertEquals(2, AppCatalogBuilder.filter(catalog, "   ").size)
        assertEquals(0, AppCatalogBuilder.filter(catalog, "nothing").size)
    }

    @Test
    fun componentKeysRoundTripThroughTheFlattenedForm() {
        val identity = AppIdentity("com.a", "com.a.Main", "A", 1L)
        assertEquals(
            "com.a" to "com.a.Main",
            AppIdentity.parseFlattened(identity.flattenedComponent),
        )
    }
}
