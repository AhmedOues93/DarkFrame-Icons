package com.darkframe.icons.ui.setup

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.darkframe.icons.R
import com.darkframe.icons.engine.DarkFrameEngine
import com.darkframe.icons.engine.apply.ApplyCapability
import com.darkframe.icons.engine.apply.LauncherProfile
import com.darkframe.icons.engine.apply.SamsungApplyStep
import com.darkframe.icons.ui.browser.IconBrowserActivity
import com.darkframe.icons.ui.common.DarkFrameActivity
import com.darkframe.icons.ui.common.SectionScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * How the user's icons actually get applied, on the device they are holding.
 *
 * The screen is driven by [ApplyCapability], so it is structurally unable to describe a mechanism
 * that does not exist here. Each branch offers exactly one primary action, and that action always
 * does something real:
 *
 *  - **Samsung One UI** hands over to Theme Park, which is the only thing on a Galaxy device that
 *    applies an icon theme to the home screen and app drawer. DarkFrame does not claim to have
 *    applied anything; Samsung performs that step.
 *  - **Icon-pack launchers** get sent to the launcher itself, where the user picks DarkFrame.
 *  - **Everything else** is told plainly what it can and cannot do.
 *
 * There is no "Apply all" button anywhere in this flow, because there is no API behind one.
 */
class ApplyActivity : DarkFrameActivity() {

    private val engine by lazy { DarkFrameEngine.get(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            val profile = withContext(Dispatchers.IO) { engine.apply.currentLauncher() }
            val capability = withContext(Dispatchers.IO) { engine.apply.effectiveCapability(profile) }
            val samsungStep = if (capability == ApplyCapability.SAMSUNG_THEME_PARK) {
                withContext(Dispatchers.IO) { engine.apply.samsungStep() }
            } else {
                null
            }
            render(profile, capability, samsungStep)
        }
    }

    private fun render(
        profile: LauncherProfile,
        capability: ApplyCapability,
        samsungStep: SamsungApplyStep?,
    ) {
        val screen = SectionScreen(this).setUp(getString(R.string.apply_title), profile.displayName)

        when (capability) {
            ApplyCapability.SAMSUNG_THEME_PARK -> renderSamsung(screen, samsungStep)
            ApplyCapability.ICON_PACK_NATIVE -> renderIconPack(screen, profile)
            ApplyCapability.PER_ICON_PICKER -> renderPerIcon(screen, profile)
            ApplyCapability.PINNED_SHORTCUT -> renderShortcut(screen, profile)
            ApplyCapability.EXPORT_ONLY -> renderExport(screen)
        }

        screen.secondaryButton(getString(R.string.apply_preview_icons)) {
            startActivity(Intent(this, IconBrowserActivity::class.java))
        }
        screen.section(
            getString(R.string.apply_limits_heading),
            getString(R.string.apply_limits_body),
        )
    }

    // ---- Samsung ------------------------------------------------------------------------------

    private fun renderSamsung(screen: SectionScreen, step: SamsungApplyStep?) {
        screen.section(
            getString(R.string.apply_samsung_heading),
            getString(R.string.apply_samsung_body),
        )

        val resolved = step ?: SamsungApplyStep.INSTALL_GOOD_LOCK
        val intent = engine.apply.samsungIntentFor(resolved)
        val label = when (resolved) {
            SamsungApplyStep.OPEN_THEME_PARK -> getString(R.string.apply_samsung_step_open)
            SamsungApplyStep.OPEN_GOOD_LOCK -> getString(R.string.apply_samsung_step_goodlock)
            SamsungApplyStep.INSTALL_GOOD_LOCK -> getString(R.string.apply_samsung_step_install)
        }

        // Disabled rather than firing an intent that would land on an error screen. On a device with
        // no Galaxy Store and no Play Store there is genuinely nothing to open, and saying so is
        // better than a button that does nothing.
        screen.primaryButton(label, enabled = intent != null) {
            runCatching { startActivity(intent) }.onFailure {
                Toast.makeText(this, R.string.apply_samsung_unavailable, Toast.LENGTH_LONG).show()
            }
        }

        when (resolved) {
            SamsungApplyStep.OPEN_THEME_PARK ->
                screen.caption(getString(R.string.apply_samsung_how))
            SamsungApplyStep.OPEN_GOOD_LOCK ->
                screen.caption(getString(R.string.apply_samsung_goodlock_hint))
            SamsungApplyStep.INSTALL_GOOD_LOCK ->
                screen.caption(getString(R.string.apply_samsung_install_hint))
        }
        if (intent == null) {
            screen.caption(getString(R.string.apply_samsung_unavailable))
        }
    }

    // ---- other launchers ----------------------------------------------------------------------

    private fun renderIconPack(screen: SectionScreen, profile: LauncherProfile) {
        screen.section(
            getString(R.string.apply_iconpack_heading, profile.displayName),
            getString(R.string.apply_iconpack_body),
        )
        val launch = packageManager.getLaunchIntentForPackage(profile.packageName)
        screen.primaryButton(getString(R.string.apply_open_launcher_settings), enabled = launch != null) {
            runCatching { startActivity(launch) }
        }
        screen.caption(profile.note)
    }

    private fun renderPerIcon(screen: SectionScreen, profile: LauncherProfile) {
        screen.section(
            getString(R.string.apply_iconpack_heading, profile.displayName),
            profile.note,
        )
        screen.section(getString(R.string.apply_export_heading), getString(R.string.apply_export_body))
    }

    private fun renderShortcut(screen: SectionScreen, profile: LauncherProfile) {
        screen.section(
            getString(R.string.apply_shortcut_heading, profile.displayName),
            getString(R.string.apply_shortcut_body),
        )
        screen.section(getString(R.string.apply_export_heading), getString(R.string.apply_export_body))
    }

    private fun renderExport(screen: SectionScreen) {
        screen.section(getString(R.string.apply_export_heading), getString(R.string.apply_export_body))
    }
}
