package com.darkframe.icons.engine

import com.darkframe.icons.engine.domain.AppIdentity
import com.darkframe.icons.engine.domain.CuratedEntry
import com.darkframe.icons.engine.domain.CuratedIconIndex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CuratedIconIndexTest {

    private fun identity(pkg: String, activity: String = "$pkg.Main") =
        AppIdentity(pkg, activity, pkg, 1L)

    @Test
    fun componentOverrideWins() {
        val index = CuratedIconIndex.from(
            listOf(CuratedEntry("dfg_spotify", component = "ComponentInfo{com.spotify.music/com.spotify.music.MainActivity}")),
        )
        assertEquals(
            "dfg_spotify",
            index.drawableNameFor(identity("com.spotify.music", "com.spotify.music.MainActivity")),
        )
    }

    @Test
    fun packageOverrideCoversEveryLauncherActivityOfThatPackage() {
        val index = CuratedIconIndex.from(listOf(CuratedEntry("dfg_cam", packageName = "com.cam")))
        assertEquals("dfg_cam", index.drawableNameFor(identity("com.cam", "com.cam.Photo")))
        assertEquals("dfg_cam", index.drawableNameFor(identity("com.cam", "com.cam.Video")))
    }

    @Test
    fun componentEntryBeatsPackageEntryForTheSamePackage() {
        val index = CuratedIconIndex.from(
            listOf(
                CuratedEntry("dfg_generic", packageName = "com.cam"),
                CuratedEntry("dfg_video", component = "ComponentInfo{com.cam/com.cam.Video}"),
            ),
        )
        assertEquals("dfg_video", index.drawableNameFor(identity("com.cam", "com.cam.Video")))
        assertEquals("dfg_generic", index.drawableNameFor(identity("com.cam", "com.cam.Photo")))
    }

    @Test
    fun anAppWithNoCuratedArtworkIsSimplyNotOverridden() {
        // The core promise of the engine: absence from the curated set is not absence of support.
        val index = CuratedIconIndex.from(listOf(CuratedEntry("dfg_a", packageName = "com.a")))
        assertNull(index.drawableNameFor(identity("com.brand.new.app")))
        assertFalse(index.hasOverrideFor(identity("com.brand.new.app")))
    }

    @Test
    fun anEmptyIndexIsValid() {
        assertTrue(CuratedIconIndex.EMPTY.isEmpty)
        assertNull(CuratedIconIndex.EMPTY.drawableNameFor(identity("com.a")))
        assertEquals(0, CuratedIconIndex.from(emptyList()).size)
    }

    @Test
    fun malformedRowsAreSkippedWithoutLosingValidOnes() {
        val index = CuratedIconIndex.from(
            listOf(
                CuratedEntry("dfg_broken", component = "not a component"),
                CuratedEntry("dfg_broken2", component = "ComponentInfo{missing_slash}"),
                CuratedEntry("dfg_broken3", component = "ComponentInfo{com.a/}"),
                CuratedEntry("", packageName = "com.blank"),
                CuratedEntry("dfg_ok", packageName = "com.ok"),
            ),
        )
        assertEquals(1, index.size)
        assertEquals("dfg_ok", index.drawableNameFor(identity("com.ok")))
    }

    @Test
    fun relativeActivityNamesAreExpandedAgainstTheirPackage() {
        val index = CuratedIconIndex.from(
            listOf(CuratedEntry("dfg_a", component = "ComponentInfo{com.a/.Main}")),
        )
        assertEquals("dfg_a", index.drawableNameFor(identity("com.a", "com.a.Main")))
    }

    @Test
    fun bareComponentFormIsAlsoAccepted() {
        val index = CuratedIconIndex.from(listOf(CuratedEntry("dfg_a", component = "com.a/com.a.Main")))
        assertEquals("dfg_a", index.drawableNameFor(identity("com.a", "com.a.Main")))
    }

    @Test
    fun drawableNamesAreReportedForTheCuratedFilter() {
        val index = CuratedIconIndex.from(
            listOf(
                CuratedEntry("dfg_a", packageName = "com.a"),
                CuratedEntry("dfg_b", component = "ComponentInfo{com.b/com.b.Main}"),
                CuratedEntry("dfg_a", packageName = "com.c"),
            ),
        )
        assertEquals(setOf("dfg_a", "dfg_b"), index.drawableNames())
    }
}
