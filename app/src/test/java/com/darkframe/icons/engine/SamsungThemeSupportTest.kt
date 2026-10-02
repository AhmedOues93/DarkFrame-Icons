package com.darkframe.icons.engine

import com.darkframe.icons.engine.apply.SamsungApplyStep
import com.darkframe.icons.engine.apply.SamsungCapabilityState
import com.darkframe.icons.engine.apply.SamsungThemeSupport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SamsungThemeSupportTest {
    @Test fun oneUiHomeIsRecognised() {
        assertTrue(SamsungThemeSupport.isOneUiHome(SamsungThemeSupport.ONE_UI_HOME))
        assertFalse(SamsungThemeSupport.isOneUiHome("com.teslacoilsw.launcher"))
    }

    @Test fun installedResolvableThemeParkIsReady() {
        assertEquals(
            SamsungCapabilityState.Ready(goodLockInstalled = true),
            SamsungThemeSupport.stateFor(true, true, true, true),
        )
    }

    @Test fun themeParkInstalledButNotLaunchableIsNeverOffered() {
        assertEquals(
            SamsungCapabilityState.Unavailable(goodLockInstalled = true),
            SamsungThemeSupport.stateFor(true, true, false, true),
        )
    }

    @Test fun missingThemeParkUsesDirectStoreRouteWhetherGoodLockExistsOrNot() {
        assertEquals(
            SamsungCapabilityState.NeedsThemePark(goodLockInstalled = true),
            SamsungThemeSupport.stateFor(false, true, false, true),
        )
        assertEquals(
            SamsungCapabilityState.NeedsThemePark(goodLockInstalled = false),
            SamsungThemeSupport.stateFor(false, false, false, true),
        )
    }

    @Test fun noResolvableSamsungRouteIsUnavailable() {
        assertTrue(SamsungThemeSupport.stateFor(false, false, false, false) is SamsungCapabilityState.Unavailable)
    }

    @Test fun samsungStepsContainNoFakeApplyAllAction() {
        assertEquals(setOf(SamsungApplyStep.OPEN_THEME_PARK, SamsungApplyStep.INSTALL_THEME_PARK), SamsungApplyStep.entries.toSet())
    }

    @Test fun storeLinksNameRequestedPackage() {
        assertTrue(SamsungThemeSupport.galaxyStoreUri("com.x").endsWith("com.x"))
        assertTrue(SamsungThemeSupport.playStoreUri("com.x").endsWith("com.x"))
    }
}
