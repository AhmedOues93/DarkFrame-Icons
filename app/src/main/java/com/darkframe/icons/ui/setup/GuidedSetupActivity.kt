package com.darkframe.icons.ui.setup

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.darkframe.icons.R
import com.darkframe.icons.engine.DarkFrameEngine
import com.darkframe.icons.engine.apply.ApplyCapability
import com.darkframe.icons.engine.apply.LauncherProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Explains, for this user's actual launcher, how DarkFrame icons get applied — and what Android does
 * not allow.
 *
 * The honesty requirement is a product requirement, not a disclaimer bolted on at the end: the
 * screen's content is driven by [ApplyCapability], so it is structurally incapable of describing a
 * mechanism that is not available on the device it is running on.
 */
class GuidedSetupActivity : AppCompatActivity() {

    private lateinit var container: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_guided_setup)
        container = findViewById(R.id.setup_sections)

        lifecycleScope.launch {
            val engine = DarkFrameEngine.get(applicationContext)
            val profile = withContext(Dispatchers.IO) { engine.apply.currentLauncher() }
            val capability = withContext(Dispatchers.IO) { engine.apply.effectiveCapability(profile) }
            render(profile, capability)
        }
    }

    private fun render(profile: LauncherProfile, capability: ApplyCapability) {
        addSection(getString(R.string.setup_honesty_heading), getString(R.string.setup_honesty_body))
        addSection(getString(R.string.setup_launcher_heading), "${profile.displayName}\n\n${profile.note}")

        when (capability) {
            ApplyCapability.ICON_PACK_NATIVE ->
                addSection(getString(R.string.setup_iconpack_heading), getString(R.string.setup_iconpack_body))
            ApplyCapability.PER_ICON_PICKER ->
                addSection(getString(R.string.setup_picker_heading), getString(R.string.setup_picker_body))
            ApplyCapability.PINNED_SHORTCUT ->
                addSection(getString(R.string.setup_shortcut_heading), getString(R.string.setup_shortcut_body))
            ApplyCapability.EXPORT_ONLY ->
                addSection(getString(R.string.setup_export_heading), getString(R.string.setup_export_body))
        }

        // Export is always available, whatever the launcher does, so it is offered alongside the
        // primary mechanism rather than only as a last resort.
        if (capability != ApplyCapability.EXPORT_ONLY && capability != ApplyCapability.PER_ICON_PICKER) {
            addSection(
                getString(R.string.setup_export_extra_heading),
                getString(R.string.setup_export_extra_body),
            )
        }

        addSection(
            getString(R.string.setup_visibility_heading),
            getString(R.string.setup_visibility_body),
        )
    }

    private fun addSection(heading: String, body: String) {
        val view = LayoutInflater.from(this)
            .inflate(R.layout.item_setup_section, container, false)
        view.findViewById<TextView>(R.id.section_heading).text = heading
        view.findViewById<TextView>(R.id.section_body).text = body
        container.addView(view)
    }
}
