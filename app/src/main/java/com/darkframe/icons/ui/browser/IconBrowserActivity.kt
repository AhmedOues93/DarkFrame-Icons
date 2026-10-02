package com.darkframe.icons.ui.browser

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.darkframe.icons.R
import com.darkframe.icons.engine.DarkFrameEngine
import com.darkframe.icons.engine.apply.ApplyCapability
import com.darkframe.icons.engine.apply.ApplyOutcome
import com.darkframe.icons.engine.domain.AppIdentity
import com.darkframe.icons.engine.domain.IconStyleCatalog
import com.darkframe.icons.ui.setup.GuidedSetupActivity
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Browser for every app DarkFrame can theme on this device.
 *
 * The activity is wiring only: it inflates views, forwards input to [IconBrowserViewModel] and
 * renders state. No icon composition, no package queries, no cache decisions live here.
 */
class IconBrowserActivity : AppCompatActivity() {

    private val viewModel: IconBrowserViewModel by viewModels()

    private lateinit var grid: RecyclerView
    private lateinit var adapter: IconGridAdapter
    private lateinit var chips: ChipGroup
    private lateinit var coverage: TextView
    private lateinit var message: TextView

    private var lastStyleId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_icon_browser)

        grid = findViewById(R.id.icon_grid)
        chips = findViewById(R.id.collection_chips)
        coverage = findViewById(R.id.browser_coverage)
        message = findViewById(R.id.browser_message)

        val iconSizePx = resources.getDimensionPixelSize(R.dimen.df_icon_size)
        adapter = IconGridAdapter(
            loader = viewModel,
            scope = lifecycleScope,
            iconSizePx = iconSizePx,
            onClick = ::showApplyOptions,
        )
        grid.adapter = adapter
        setUpResponsiveGrid()
        setUpChips()
        setUpSearch()

        findViewById<View>(R.id.browser_setup_link).setOnClickListener {
            startActivity(Intent(this, GuidedSetupActivity::class.java))
        }

        observeState()
    }

    override fun onStop() {
        viewModel.trimCache()
        super.onStop()
    }

    /**
     * Column count is derived from the measured width rather than from a layout qualifier.
     *
     * This is what makes one layout correct on a phone, on a Fold's cover screen, on the same Fold
     * unfolded, and on a tablet — including across a fold/unfold while the screen is open, which a
     * qualifier-selected span count would get wrong until the activity was recreated.
     */
    private fun setUpResponsiveGrid() {
        val layoutManager = GridLayoutManager(this, 4)
        grid.layoutManager = layoutManager
        val target = resources.getDimensionPixelSize(R.dimen.df_grid_cell_target)
        grid.addOnLayoutChangeListener { _, left, _, right, _, _, _, _, _ ->
            val usable = right - left - grid.paddingStart - grid.paddingEnd
            if (usable <= 0) return@addOnLayoutChangeListener
            val span = (usable / target).coerceIn(MIN_SPAN, MAX_SPAN)
            if (span != layoutManager.spanCount) layoutManager.spanCount = span
        }
    }

    private fun setUpChips() {
        IconStyleCatalog.all.forEach { style ->
            val chip = Chip(this).apply {
                id = View.generateViewId()
                text = if (style.tier.name == "PRO") {
                    "${style.displayName} · ${getString(R.string.browser_pro_suffix)}"
                } else {
                    style.displayName
                }
                isCheckable = true
                tag = style.id
            }
            chips.addView(chip)
        }
        chips.setOnCheckedStateChangeListener { group, checkedIds ->
            val checked = checkedIds.firstOrNull() ?: return@setOnCheckedStateChangeListener
            val styleId = group.findViewById<Chip>(checked)?.tag as? String ?: return@setOnCheckedStateChangeListener
            viewModel.selectStyle(IconStyleCatalog.forId(styleId))
        }
    }

    private fun setUpSearch() {
        findViewById<EditText>(R.id.browser_search).addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setQuery(s?.toString().orEmpty())
            }

            override fun afterTextChanged(s: Editable?) = Unit
        })
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    adapter.submitList(state.visibleApps)

                    if (lastStyleId != null && lastStyleId != state.style.id) {
                        adapter.onStyleChanged()
                    }
                    lastStyleId = state.style.id
                    syncChipSelection(state.style.id)

                    coverage.text = getString(
                        R.string.browser_coverage,
                        state.totalApps,
                        state.curatedApps,
                    )

                    message.visibility = when {
                        state.loading -> View.VISIBLE
                        state.visibleApps.isEmpty() -> View.VISIBLE
                        else -> View.GONE
                    }
                    message.text = when {
                        state.loading -> getString(R.string.browser_loading)
                        else -> getString(R.string.browser_empty)
                    }
                }
            }
        }
    }

    private fun syncChipSelection(styleId: String) {
        for (index in 0 until chips.childCount) {
            val chip = chips.getChildAt(index) as? Chip ?: continue
            val shouldCheck = chip.tag == styleId
            if (chip.isChecked != shouldCheck) chip.isChecked = shouldCheck
        }
    }

    /**
     * Offers only what the current launcher actually supports. On a launcher that reads icon packs
     * there is nothing per-app to do here, so the user is sent to the instructions instead of being
     * given a button that would do the wrong thing.
     */
    private fun showApplyOptions(identity: AppIdentity) {
        val capability = viewModel.state.value.capability
        if (capability == ApplyCapability.ICON_PACK_NATIVE) {
            startActivity(Intent(this, GuidedSetupActivity::class.java))
            return
        }

        val actions = buildList {
            if (capability == ApplyCapability.PINNED_SHORTCUT) add(getString(R.string.apply_pin))
            add(getString(R.string.apply_export))
        }

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.apply_title, identity.displayLabel()))
            .setItems(actions.toTypedArray()) { _, which ->
                when (actions[which]) {
                    getString(R.string.apply_pin) -> pinShortcut(identity)
                    getString(R.string.apply_export) -> exportIcon(identity)
                }
            }
            .setNegativeButton(R.string.apply_cancel, null)
            .show()
    }

    private fun pinShortcut(identity: AppIdentity) {
        lifecycleScope.launch {
            val engine = DarkFrameEngine.get(applicationContext)
            val icon = viewModel.load(identity, EXPORT_SIZE_PX)
            val outcome = withContext(Dispatchers.IO) { engine.apply.pinThemedShortcut(identity, icon) }
            val text = when (outcome) {
                ApplyOutcome.Requested -> getString(R.string.apply_requested)
                is ApplyOutcome.NotSupported -> outcome.reason
                is ApplyOutcome.Failed -> outcome.reason
            }
            AlertDialog.Builder(this@IconBrowserActivity)
                .setMessage(text)
                .setPositiveButton(R.string.apply_cancel, null)
                .show()
        }
    }

    private fun exportIcon(identity: AppIdentity) {
        lifecycleScope.launch {
            val engine = DarkFrameEngine.get(applicationContext)
            val style = viewModel.state.value.style
            val icon = viewModel.load(identity, EXPORT_SIZE_PX)
            val uri = withContext(Dispatchers.IO) { engine.apply.exportIcon(identity, style, icon) }
            if (uri == null) {
                AlertDialog.Builder(this@IconBrowserActivity)
                    .setMessage(R.string.apply_export_failed)
                    .setPositiveButton(R.string.apply_cancel, null)
                    .show()
                return@launch
            }
            startActivity(
                Intent.createChooser(
                    engine.apply.shareIntent(uri),
                    getString(R.string.apply_share_title),
                ),
            )
        }
    }

    private companion object {
        const val MIN_SPAN = 3
        const val MAX_SPAN = 10

        /** Exports and shortcut icons are produced at DarkFrame's full design grid. */
        const val EXPORT_SIZE_PX = 512
    }
}
