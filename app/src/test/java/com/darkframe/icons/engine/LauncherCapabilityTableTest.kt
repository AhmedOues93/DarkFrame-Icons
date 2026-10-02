package com.darkframe.icons.engine

import com.darkframe.icons.engine.apply.ApplyCapability
import com.darkframe.icons.engine.apply.LauncherCapabilityTable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherCapabilityTableTest {

    @Test
    fun iconPackLaunchersAreRecognised() {
        listOf("com.teslacoilsw.launcher", "app.lawnchair", "com.microsoft.launcher").forEach {
            assertEquals(
                "$it should be icon-pack capable",
                ApplyCapability.ICON_PACK_NATIVE,
                LauncherCapabilityTable.resolve(it).capability,
            )
            assertTrue(LauncherCapabilityTable.supportsIconPack(it))
        }
    }

    @Test
    fun stockLaunchersAreNotClaimedToSupportIconPacks() {
        // The single most important honesty check in the product. Neither Pixel Launcher nor One UI
        // Home lets a third-party app substitute icons directly, and DarkFrame must never imply so.
        listOf("com.google.android.apps.nexuslauncher", "com.sec.android.app.launcher").forEach {
            assertFalse(
                "$it must not be advertised as icon-pack capable",
                LauncherCapabilityTable.supportsIconPack(it),
            )
        }
        assertEquals(
            ApplyCapability.PINNED_SHORTCUT,
            LauncherCapabilityTable.resolve("com.google.android.apps.nexuslauncher").capability,
        )
    }

    @Test
    fun samsungUsesThemeParkRatherThanDuplicateShortcuts() {
        // Testing on a real Galaxy Z Fold8 showed pinning creates a second icon beside the original
        // instead of replacing it. Samsung's flow must not regress to that.
        val profile = LauncherCapabilityTable.resolve("com.sec.android.app.launcher")
        assertEquals(ApplyCapability.SAMSUNG_THEME_PARK, profile.capability)
        assertTrue(LauncherCapabilityTable.usesSamsungThemePark("com.sec.android.app.launcher"))
        assertFalse(LauncherCapabilityTable.usesSamsungThemePark("com.teslacoilsw.launcher"))
    }

    @Test
    fun oneUiNoteNamesThemeParkAsTheMechanism() {
        val note = LauncherCapabilityTable.resolve("com.sec.android.app.launcher").note
        assertTrue(note.contains("Theme Park"))
    }

    @Test
    fun unknownLaunchersFallBackConservatively() {
        val profile = LauncherCapabilityTable.resolve("com.some.unreleased.launcher", "Mystery")
        assertEquals(ApplyCapability.EXPORT_ONLY, profile.capability)
        assertTrue(profile.requiresManualStep)
        assertEquals("Mystery", profile.displayName)
    }

    @Test
    fun missingLauncherPackageDoesNotCrash() {
        assertEquals(ApplyCapability.EXPORT_ONLY, LauncherCapabilityTable.resolve(null).capability)
        assertEquals(ApplyCapability.EXPORT_ONLY, LauncherCapabilityTable.resolve("").capability)
        assertFalse(LauncherCapabilityTable.supportsIconPack(null))
    }

    @Test
    fun unknownLauncherWithNoDisplayNameFallsBackToItsPackage() {
        assertEquals("com.x.y", LauncherCapabilityTable.resolve("com.x.y", "  ").displayName)
    }

    @Test
    fun everyKnownLauncherIsUsableAndDocumented() {
        val table = LauncherCapabilityTable.knownLaunchers()
        assertEquals(table.size, table.map { it.packageName }.toSet().size)
        table.forEach { profile ->
            assertTrue("${profile.packageName} needs a display name", profile.displayName.isNotBlank())
            assertTrue("${profile.packageName} needs a user-facing note", profile.note.length > 20)
            if (profile.capability == ApplyCapability.ICON_PACK_NATIVE) {
                // Selecting an icon pack always happens inside the launcher's own settings; no
                // launcher exposes an API that lets us do it for the user.
                assertTrue(
                    "${profile.packageName} icon-pack selection must be flagged as manual",
                    profile.requiresManualStep,
                )
            }
        }
    }
}
