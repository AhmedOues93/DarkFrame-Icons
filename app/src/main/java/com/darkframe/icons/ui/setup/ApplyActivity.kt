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
import com.darkframe.icons.engine.apply.SamsungCapabilityState
import com.darkframe.icons.engine.data.StylePreferenceStore
import com.darkframe.icons.ui.browser.IconBrowserActivity
import com.darkframe.icons.ui.common.DarkFrameActivity
import com.darkframe.icons.ui.common.SectionScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Honest launcher-specific apply flow. No button claims an action Android/Samsung does not expose. */
class ApplyActivity : DarkFrameActivity() {
    private val engine by lazy { DarkFrameEngine.get(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            val model = withContext(Dispatchers.IO) {
                val profile = engine.apply.currentLauncher()
                val capability = engine.apply.effectiveCapability(profile)
                val apps = engine.installedApps.loadCatalog()
                val curated = apps.count(engine.curatedIcons::hasOverrideFor)
                ApplyModel(
                    profile = profile,
                    capability = capability,
                    samsungState = if (capability == ApplyCapability.SAMSUNG_THEME_PARK) engine.apply.samsungState() else null,
                    appCount = apps.size,
                    curatedCount = curated,
                    styleName = StylePreferenceStore(applicationContext).selectedStyle().displayName,
                )
            }
            render(model)
        }
    }

    private fun render(model: ApplyModel) {
        val screen = SectionScreen(this).setUp(getString(R.string.apply_title), model.profile.displayName)
        screen.row(getString(R.string.apply_selected_collection), value = model.styleName)
        screen.row(
            getString(R.string.apply_accessible_apps),
            getString(R.string.apply_coverage_detail, model.curatedCount, model.appCount - model.curatedCount),
            model.appCount.toString(),
        )

        when (model.capability) {
            ApplyCapability.SAMSUNG_THEME_PARK -> renderSamsung(screen, model.samsungState)
            ApplyCapability.ICON_PACK_NATIVE -> renderIconPack(screen, model.profile)
            ApplyCapability.PER_ICON_PICKER -> renderPerIcon(screen, model.profile)
            ApplyCapability.PINNED_SHORTCUT -> renderShortcut(screen, model.profile)
            ApplyCapability.EXPORT_ONLY -> renderExport(screen)
        }

        screen.secondaryButton(getString(R.string.apply_preview_icons)) {
            startActivity(Intent(this, IconBrowserActivity::class.java))
        }
        screen.section(getString(R.string.apply_limits_heading), getString(R.string.apply_limits_body))
    }

    private fun renderSamsung(screen: SectionScreen, state: SamsungCapabilityState?) {
        screen.section(getString(R.string.apply_samsung_heading), getString(R.string.apply_samsung_body))

        val step = state?.let(engine.apply::samsungStep)
        val intent = step?.let(engine.apply::samsungIntentFor)
        val label = when (step) {
            SamsungApplyStep.OPEN_THEME_PARK -> getString(R.string.apply_samsung_step_open)
            SamsungApplyStep.INSTALL_THEME_PARK -> getString(R.string.apply_samsung_step_install_theme_park)
            null -> getString(R.string.apply_samsung_unavailable)
        }
        screen.primaryButton(label, enabled = intent != null) {
            runCatching { startActivity(intent) }.onFailure {
                Toast.makeText(this, R.string.apply_samsung_unavailable, Toast.LENGTH_LONG).show()
            }
        }

        when (state) {
            is SamsungCapabilityState.Ready -> {
                screen.caption(getString(R.string.apply_samsung_how))
                screen.caption(
                    if (state.goodLockInstalled) getString(R.string.apply_goodlock_detected)
                    else getString(R.string.apply_goodlock_not_required)
                )
            }
            is SamsungCapabilityState.NeedsThemePark ->
                screen.caption(getString(R.string.apply_samsung_install_theme_park_hint))
            is SamsungCapabilityState.Unavailable, null ->
                screen.caption(getString(R.string.apply_samsung_unavailable_detail))
        }
    }

    private fun renderIconPack(screen: SectionScreen, profile: LauncherProfile) {
        screen.section(getString(R.string.apply_iconpack_heading, profile.displayName), getString(R.string.apply_iconpack_body))
        val launch = packageManager.getLaunchIntentForPackage(profile.packageName)
        screen.primaryButton(getString(R.string.apply_open_launcher_settings), enabled = launch != null) {
            runCatching { startActivity(launch) }
        }
        screen.caption(profile.note)
    }

    private fun renderPerIcon(screen: SectionScreen, profile: LauncherProfile) {
        screen.section(getString(R.string.apply_iconpack_heading, profile.displayName), profile.note)
        screen.section(getString(R.string.apply_export_heading), getString(R.string.apply_export_body))
    }

    private fun renderShortcut(screen: SectionScreen, profile: LauncherProfile) {
        screen.section(getString(R.string.apply_shortcut_heading, profile.displayName), getString(R.string.apply_shortcut_body))
        screen.section(getString(R.string.apply_export_heading), getString(R.string.apply_export_body))
    }

    private fun renderExport(screen: SectionScreen) {
        screen.section(getString(R.string.apply_export_heading), getString(R.string.apply_export_body))
    }

    private data class ApplyModel(
        val profile: LauncherProfile,
        val capability: ApplyCapability,
        val samsungState: SamsungCapabilityState?,
        val appCount: Int,
        val curatedCount: Int,
        val styleName: String,
    )
}
