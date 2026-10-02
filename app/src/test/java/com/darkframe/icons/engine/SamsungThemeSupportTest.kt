package com.darkframe.icons.engine

import com.darkframe.icons.engine.apply.SamsungApplyStep
import com.darkframe.icons.engine.apply.SamsungThemeSupport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SamsungThemeSupportTest {

    @Test
    fun oneUiHomeIsRecognised() {
        assertTrue(SamsungThemeSupport.isOneUiHome(SamsungThemeSupport.ONE_UI_HOME))
    }

    @Test
    fun aGalaxyDeviceRunningNovaGetsTheIconPackFlowNotTheSamsungOne() {
        // The launcher drawing the home screen is what decides, not the manufacturer.
        assertFalse(SamsungThemeSupport.isOneUiHome("com.teslacoilsw.launcher"))
        assertFalse(SamsungThemeSupport.isOneUiHome(null))
        assertFalse(SamsungThemeSupport.isOneUiHome(""))
    }

    @Test
    fun withThemeParkInstalledWeHandOverDirectly() {
        assertEquals(
            SamsungApplyStep.OPEN_THEME_PARK,
            SamsungThemeSupport.stepFor(themeParkInstalled = true, goodLockInstalled = true),
        )
        assertEquals(
            SamsungApplyStep.OPEN_THEME_PARK,
            SamsungThemeSupport.stepFor(themeParkInstalled = true, goodLockInstalled = false),
        )
    }

    @Test
    fun withOnlyGoodLockWeOpenGoodLockSoThemeParkCanBeAdded() {
        assertEquals(
            SamsungApplyStep.OPEN_GOOD_LOCK,
            SamsungThemeSupport.stepFor(themeParkInstalled = false, goodLockInstalled = true),
        )
    }

    @Test
    fun withNeitherInstalledWeSendTheUserToTheStore() {
        assertEquals(
            SamsungApplyStep.INSTALL_GOOD_LOCK,
            SamsungThemeSupport.stepFor(themeParkInstalled = false, goodLockInstalled = false),
        )
    }

    @Test
    fun everyStepCorrespondsToSomethingWeCanActuallyDo() {
        // There is deliberately no "applied" or "apply all" state: Samsung performs the final
        // system-wide step, so DarkFrame must never model itself as having done it.
        assertEquals(3, SamsungApplyStep.entries.size)
    }

    @Test
    fun storeLinksNameTheRequestedPackage() {
        assertTrue(SamsungThemeSupport.galaxyStoreUri("com.x").endsWith("com.x"))
        assertTrue(SamsungThemeSupport.playStoreUri("com.x").endsWith("com.x"))
    }
}
