package com.darkframe.icons.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.darkframe.icons.BuildConfig
import com.darkframe.icons.R
import com.darkframe.icons.billing.Entitlement
import com.darkframe.icons.billing.ProEntitlementStore
import com.darkframe.icons.engine.DarkFrameEngine
import com.darkframe.icons.ui.common.DarkFrameActivity
import com.darkframe.icons.ui.common.SectionScreen
import com.darkframe.icons.ui.setup.ApplyActivity
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The small maintenance and information area.
 *
 * Everything here is something a user needs occasionally and should not meet on the home screen:
 * the purchase, the apply help, the cache rebuild, and what DarkFrame reads about their device.
 */
class SettingsActivity : DarkFrameActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        render()
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        val isPro = ProEntitlementStore(this).current() == Entitlement.PRO
        SectionScreen(this)
            .setUp(getString(R.string.settings_title))
            .row(
                title = getString(R.string.settings_pro),
                subtitle = if (isPro) getString(R.string.settings_pro_sub_active)
                else getString(R.string.settings_pro_sub_free),
            ) { startActivity(Intent(this, ProActivity::class.java)) }
            .row(
                title = getString(R.string.settings_apply_help),
                subtitle = getString(R.string.settings_apply_help_sub),
            ) { startActivity(Intent(this, ApplyActivity::class.java)) }
            .row(
                title = getString(R.string.settings_rebuild),
                subtitle = getString(R.string.settings_rebuild_sub),
            ) { rebuildCache() }
            .row(
                title = getString(R.string.settings_privacy),
                subtitle = getString(R.string.settings_privacy_sub),
            ) { showPrivacy() }
            .row(
                title = getString(R.string.settings_licenses),
                subtitle = getString(R.string.settings_licenses_sub),
            ) { showLicenses() }
            .row(
                title = getString(R.string.settings_about),
                subtitle = getString(R.string.settings_version, BuildConfig.VERSION_NAME),
            )
            .also { screen -> addVisualQaRow(screen) }
    }

    /**
     * The debug-only visual QA matrix, if this build has one.
     *
     * Found by resolving an action rather than by naming the Activity, because the Activity only
     * exists in the debug source set: a direct class reference would not compile for release. This
     * way the row simply never appears in a release build, and there is no `BuildConfig.DEBUG`
     * branch whose dead side still has to be shrunk away.
     */
    private fun addVisualQaRow(screen: SectionScreen) {
        val intent = Intent(VISUAL_QA_ACTION).setPackage(packageName)
        if (packageManager.resolveActivity(intent, 0) == null) return
        screen.row(
            title = getString(R.string.settings_qa),
            subtitle = getString(R.string.settings_qa_sub),
        ) { startActivity(intent) }
    }

    private fun rebuildCache() {
        val engine = DarkFrameEngine.get(applicationContext)
        lifecycleScope.launch {
            withContext(engine.renderDispatcher) {
                engine.resolver.invalidateAll()
                engine.lookPreviews.clear()
                // The prepared sets and the single-icon exports are renders too, so leaving them
                // would have "redraw your icons" quietly keep the old ones around — and the prepared
                // set is the one a user hands to Theme Park.
                engine.iconSets.clearAll()
                engine.apply.clearExports()
            }
            Toast.makeText(this@SettingsActivity, R.string.settings_rebuild_done, Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun showPrivacy() {
        AlertDialog.Builder(this)
            .setTitle(R.string.settings_privacy)
            .setMessage(R.string.settings_privacy_body)
            .setPositiveButton(R.string.close, null)
            .show()
    }

    private fun showLicenses() {
        AlertDialog.Builder(this)
            .setTitle(R.string.settings_licenses)
            .setMessage(LICENSES)
            .setPositiveButton(R.string.close, null)
            .show()
    }

    private companion object {
        const val VISUAL_QA_ACTION = "com.darkframe.icons.action.VISUAL_QA"

        /**
         * Kept in the app rather than linked out, so the notice is available offline and cannot
         * rot. Every entry is a direct dependency declared in app/build.gradle.kts.
         */
        val LICENSES = """
            AndroidX (core-ktx, appcompat, activity-ktx, recyclerview, lifecycle)
            Apache License 2.0 — The Android Open Source Project

            Material Components for Android
            Apache License 2.0 — Google LLC

            Google Play Billing Library
            Android Software Development Kit License — Google LLC

            Kotlin and kotlinx.coroutines
            Apache License 2.0 — JetBrains s.r.o. and contributors
        """.trimIndent()
    }
}
