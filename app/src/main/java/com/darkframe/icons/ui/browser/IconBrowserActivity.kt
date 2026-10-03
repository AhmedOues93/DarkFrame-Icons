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
import com.darkframe.icons.data.FavoriteKind
import com.darkframe.icons.data.FavoritesStore
import com.darkframe.icons.engine.domain.IconStyleCatalog
import com.darkframe.icons.ui.common.DarkFrameActivity
import com.darkframe.icons.ui.common.applySystemBarPadding
import com.darkframe.icons.ui.common.spanFromWidth
import com.darkframe.icons.ui.setup.ApplyActivity
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
class IconBrowserActivity : DarkFrameActivity() {

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

        findViewById<View>(R.id.browser_root).applySystemBarPadding()
        grid = findViewById(R.id.icon_grid)
        chips = findViewById(R.id.collection_chips)
        coverage = findViewById(R.id.browser_coverage)
        message = findViewById(R.id.browser_message)

        val iconSizePx = resources.getDimensionPixelSize(R.dimen.df_icon_size)
        adapter = IconGridAdapter(
            loader = viewModel,
            scope = lifecycleScope,
            iconSizePx = iconSizePx,
            onClick = ::showAppActions,
        )
        grid.adapter = adapter
        setUpResponsiveGrid()
        setUpChips()
        setUpSearch()

        findViewById<View>(R.id.browser_setup_link).setOnClickListener {
            startActivity(Intent(this, ApplyActivity::class.java))
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
        grid.layoutManager = GridLayoutManager(this, MIN_SPAN)
        grid.spanFromWidth(
            targetCellPx = resources.getDimensionPixelSize(R.dimen.df_grid_cell_target),
            minSpan = MIN_SPAN,
            maxSpan = MAX_SPAN,
        )
    }

    private fun setUpChips() {
        IconStyleCatalog.all.forEach { style ->
            val chip = Chip(this).apply {
                id = View.generateViewId()
                text = style.displayName
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

                    coverage.text = resources.getQuantityString(
                        R.plurals.browser_coverage,
                        state.totalApps,
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
     * What a user can do with one app's icon.
     *
     * The browser is an inspection surface, not an apply surface — applying is a whole-device
     * action and lives on its own screen. So this offers only things that are true per app, and the
     * themed-shortcut option is withheld on Samsung and on icon-pack launchers, where it would add
     * a duplicate icon beside one that is about to be themed properly.
     */
    private fun showAppActions(identity: AppIdentity) {
        val capability = viewModel.state.value.capability
        val offerShortcut = capability == ApplyCapability.PINNED_SHORTCUT
        val favorites = FavoritesStore(this)
        val saved = favorites.isFavorite(FavoriteKind.APP, identity.componentKey)

        val actions = buildList {
            add(getString(R.string.browser_app_action_export))
            if (offerShortcut) add(getString(R.string.browser_app_action_pin))
            add(
                if (saved) getString(R.string.browser_app_action_unfavorite)
                else getString(R.string.browser_app_action_favorite),
            )
        }

        AlertDialog.Builder(this)
            .setTitle(identity.displayLabel())
            .setItems(actions.toTypedArray()) { _, which ->
                when (actions[which]) {
                    getString(R.string.browser_app_action_export) ->
                        exportIcon(identity)
                    getString(R.string.browser_app_action_pin) ->
                        pinShortcut(identity)
                    else -> favorites.toggle(FavoriteKind.APP, identity.componentKey)
                }
            }
            .setNegativeButton(R.string.close, null)
            .show()
    }


    /** Only reachable on a launcher where a pinned shortcut is the best available mechanism. */
    private fun pinShortcut(identity: AppIdentity) {
        lifecycleScope.launch {
            val engine = DarkFrameEngine.get(applicationContext)
            val icon = viewModel.loadForExport(identity)
            val outcome = withContext(Dispatchers.IO) { engine.apply.pinThemedShortcut(identity, icon) }
            val text = when (outcome) {
                ApplyOutcome.Requested -> getString(R.string.browser_pin_requested)
                is ApplyOutcome.NotSupported -> outcome.reason
                is ApplyOutcome.Failed -> outcome.reason
            }
            AlertDialog.Builder(this@IconBrowserActivity)
                .setMessage(text)
                .setPositiveButton(R.string.close, null)
                .show()
        }
    }

    private fun exportIcon(identity: AppIdentity) {
        lifecycleScope.launch {
            val engine = DarkFrameEngine.get(applicationContext)
            val style = viewModel.state.value.style
            // Export is the one place a full-resolution render is legitimate.
            val icon = viewModel.loadForExport(identity)
            val uri = withContext(Dispatchers.IO) { engine.apply.exportIcon(identity, style, icon) }
            if (uri == null) {
                AlertDialog.Builder(this@IconBrowserActivity)
                    .setMessage(R.string.browser_export_failed)
                    .setPositiveButton(R.string.close, null)
                    .show()
                return@launch
            }
            startActivity(
                Intent.createChooser(
                    engine.apply.shareIntent(uri),
                    getString(R.string.browser_share_title),
                ),
            )
        }
    }

    private companion object {
        const val MIN_SPAN = 3
        const val MAX_SPAN = 10
    }
}
