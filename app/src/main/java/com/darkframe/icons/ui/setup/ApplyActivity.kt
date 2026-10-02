package com.darkframe.icons.ui.setup

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.darkframe.icons.R
import com.darkframe.icons.engine.DarkFrameEngine
import com.darkframe.icons.engine.apply.ApplyCapability
import com.darkframe.icons.engine.apply.LauncherProfile
import com.darkframe.icons.engine.apply.PreparedIconSet
import com.darkframe.icons.engine.apply.SamsungApplyStep
import com.darkframe.icons.engine.data.StylePreferenceStore
import com.darkframe.icons.engine.domain.IconStyle
import com.darkframe.icons.ui.browser.IconBrowserActivity
import com.darkframe.icons.ui.common.DarkFrameActivity
import com.darkframe.icons.ui.common.SectionScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * How the user's icons actually get applied, on the device they are holding.
 *
 * The screen is driven by [ApplyCapability], so it is structurally unable to describe a mechanism
 * that does not exist here. Each branch offers exactly one thing to do next, and that thing always
 * does something real. There is no "Apply all" button anywhere in this flow, because there is no API
 * behind one.
 *
 * ## Samsung is two steps, and says so
 *
 * The previous version had a single button that opened Theme Park, which quietly implied DarkFrame
 * had already made something. It had not. The flow is now numbered:
 *
 *  1. **Prepare** — DarkFrame renders the chosen collection for every app on the device and writes
 *     the finished icons out. This is work DarkFrame genuinely does, with a count the user can see.
 *  2. **Apply** — hand over to Theme Park, which is the only component on a Galaxy device that can
 *     put an icon theme on the home screen and app drawer. Samsung performs that step; DarkFrame
 *     says so rather than taking credit for it.
 *
 * When Theme Park is not installed, step 2 becomes the one action that actually leads there — Good
 * Lock, or the store listing — instead of a disabled button with no explanation.
 */
class ApplyActivity : DarkFrameActivity() {

    private val engine by lazy { DarkFrameEngine.get(applicationContext) }

    private var prepareJob: Job? = null
    private var prepared: PreparedIconSet? = null

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

    override fun onStop() {
        // A prepare run is a few hundred full-resolution renders. Leaving the screen stops it at the
        // next icon rather than letting it carry on behind the user's back.
        prepareJob?.cancel()
        prepareJob = null
        super.onStop()
    }

    private fun style(): IconStyle = StylePreferenceStore(this).selectedStyle()

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
        val style = style()
        screen.section(
            getString(R.string.apply_samsung_heading),
            getString(R.string.apply_samsung_body),
        )

        val prepareStep = screen.step(
            number = 1,
            title = getString(R.string.apply_prepare_title),
            body = getString(R.string.apply_prepare_body, style.displayName),
            actionLabel = getString(R.string.apply_prepare_action),
        ) { /* replaced below, once the handle exists */ }
        prepareStep.setOnAction { startPrepare(prepareStep, style) }

        // A set prepared earlier survives on disk, so arriving here a second time shows the count
        // instead of asking for the same work again.
        lifecycleScope.launch {
            val existing = withContext(Dispatchers.IO) { engine.iconSets.existing(style) }
            prepared = existing
            if (existing != null) {
                prepareStep.setStatus(getString(R.string.apply_prepare_done, existing.count))
                prepareStep.setActionLabel(getString(R.string.apply_prepare_again))
            }
        }

        renderSamsungHandoff(screen, step)
        renderSaveSet(screen)
    }

    /**
     * Step 1: a real, cancellable, progress-reporting run.
     *
     * The button is disabled while it runs, so the one thing the user cannot do is start it twice.
     */
    private fun startPrepare(handle: SectionScreen.StepHandle, style: IconStyle) {
        if (prepareJob?.isActive == true) return
        handle.setActionEnabled(false)
        handle.setStatus(getString(R.string.apply_prepare_starting))
        prepareJob = lifecycleScope.launch {
            val apps = withContext(Dispatchers.IO) { engine.installedApps.loadCatalog() }
            val set = withContext(engine.renderDispatcher) {
                engine.iconSets.prepare(apps, style) { progress ->
                    // Hopping to the main thread per icon is cheap next to a 512px render, and it is
                    // what makes the count move rather than jump at the end.
                    lifecycleScope.launch {
                        handle.setStatus(
                            getString(R.string.apply_prepare_progress, progress.done, progress.total),
                        )
                    }
                }
            }
            prepared = set
            handle.setStatus(getString(R.string.apply_prepare_done, set.count))
            handle.setActionLabel(getString(R.string.apply_prepare_again))
            handle.setActionEnabled(true)
        }
    }

    private fun renderSamsungHandoff(screen: SectionScreen, step: SamsungApplyStep?) {
        val resolved = step ?: SamsungApplyStep.INSTALL_GOOD_LOCK
        val intent = engine.apply.samsungIntentFor(resolved)
        val label = when (resolved) {
            SamsungApplyStep.OPEN_THEME_PARK -> getString(R.string.apply_samsung_step_open)
            SamsungApplyStep.OPEN_GOOD_LOCK -> getString(R.string.apply_samsung_step_goodlock)
            SamsungApplyStep.INSTALL_GOOD_LOCK -> getString(R.string.apply_samsung_step_install)
        }
        val body = when (resolved) {
            SamsungApplyStep.OPEN_THEME_PARK -> getString(R.string.apply_samsung_how)
            SamsungApplyStep.OPEN_GOOD_LOCK -> getString(R.string.apply_samsung_goodlock_hint)
            SamsungApplyStep.INSTALL_GOOD_LOCK -> getString(R.string.apply_samsung_install_hint)
        }

        // Disabled rather than firing an intent that would land on an error screen — and when it is
        // disabled the caption says why, which is the difference between a dead button and an
        // explanation. On a device with neither store there is genuinely nothing to open.
        val handoff = screen.step(
            number = 2,
            title = getString(R.string.apply_handoff_title),
            body = body,
            actionLabel = label,
            enabled = intent != null,
        ) {
            runCatching { startActivity(intent) }.onFailure {
                Toast.makeText(this, R.string.apply_samsung_unavailable, Toast.LENGTH_LONG).show()
            }
        }
        if (intent == null) handoff.setStatus(getString(R.string.apply_samsung_unavailable))
    }

    /**
     * Sharing the prepared set out of the app.
     *
     * Theme Park can take an image per app from the device, and a user who wants that needs the files
     * somewhere they can pick them. `ACTION_SEND_MULTIPLE` through DarkFrame's existing FileProvider
     * does it with no storage permission — which DarkFrame would otherwise have no use for at all.
     */
    private fun renderSaveSet(screen: SectionScreen) {
        screen.secondaryButton(getString(R.string.apply_share_set)) {
            val set = prepared
            if (set == null || set.count == 0) {
                Toast.makeText(this, R.string.apply_share_set_empty, Toast.LENGTH_LONG).show()
                return@secondaryButton
            }
            val intent = engine.iconSets.shareIntent(set)
            if (intent == null) {
                Toast.makeText(this, R.string.apply_share_set_empty, Toast.LENGTH_LONG).show()
                return@secondaryButton
            }
            startActivity(Intent.createChooser(intent, getString(R.string.apply_share_set)))
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
