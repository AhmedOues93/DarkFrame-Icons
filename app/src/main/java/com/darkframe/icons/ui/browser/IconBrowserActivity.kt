package com.darkframe.icons.ui.browser

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.darkframe.icons.R
import com.darkframe.icons.engine.domain.AppIdentity
import com.darkframe.icons.engine.domain.IconStyleCatalog
import com.darkframe.icons.model.ContentTier
import com.darkframe.icons.ui.common.DarkFrameActivity
import com.darkframe.icons.ui.common.applySystemBarPadding
import com.darkframe.icons.ui.common.spanFromWidth
import com.darkframe.icons.ui.detail.IconDetailActivity
import com.darkframe.icons.ui.setup.ApplyActivity
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch

/**
 * Browser for every app DarkFrame can theme on this device.
 *
 * The activity is wiring only: it inflates views, forwards input to [IconBrowserViewModel] and
 * renders state. No icon composition, no package queries, no cache decisions live here.
 *
 * Tapping a cell opens [IconDetailActivity] rather than a list of verbs in a dialog. The browser's
 * job is to let someone look at two hundred apps at once; deciding what to do with one of them is a
 * different job and gets its own screen, where the preview can be large enough to judge.
 */
class IconBrowserActivity : DarkFrameActivity() {

    private val viewModel: IconBrowserViewModel by viewModels()

    private lateinit var grid: RecyclerView
    private lateinit var adapter: IconGridAdapter
    private lateinit var chips: ChipGroup
    private lateinit var coverage: TextView
    private lateinit var message: TextView
    private lateinit var search: EditText
    private lateinit var searchClear: View

    private var lastStyleId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_icon_browser)

        // Header and grid inset separately: the header takes the status bar, the grid takes the
        // navigation bar, and the background still runs to both edges.
        findViewById<View>(R.id.browser_header).applySystemBarPadding(top = true, bottom = false)
        grid = findViewById(R.id.icon_grid)
        grid.applySystemBarPadding(top = false, bottom = true)

        chips = findViewById(R.id.collection_chips)
        coverage = findViewById(R.id.browser_coverage)
        message = findViewById(R.id.browser_message)
        search = findViewById(R.id.browser_search)
        searchClear = findViewById(R.id.browser_search_clear)

        adapter = IconGridAdapter(
            loader = viewModel,
            scope = lifecycleScope,
            iconSizePx = resources.getDimensionPixelSize(R.dimen.df_icon_size),
            onClick = ::openDetail,
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

    private fun openDetail(identity: AppIdentity) {
        startActivity(IconDetailActivity.intent(this, identity, viewModel.state.value.style))
    }

    /**
     * Column count *and* icon size from the measured width, recomputed on every layout.
     *
     * Deliberately not a layout qualifier: a qualifier is resolved when the activity is created, so
     * a Fold that opens while this screen is already visible would keep its folded column count.
     *
     * The icon size matters as much as the count and used to be a fixed 56dp. On a Fold's inner
     * screen that produced the same phone-sized icons with much wider gaps between them — a
     * stretched phone layout, which is the thing to avoid on a large screen. Scaling the icon to the
     * column it sits in is what makes the extra width useful.
     */
    private fun setUpResponsiveGrid() {
        grid.layoutManager = GridLayoutManager(this, MIN_SPAN)
        grid.spanFromWidth(
            targetCellPx = resources.getDimensionPixelSize(R.dimen.df_grid_cell_target),
            minSpan = MIN_SPAN,
            maxSpan = MAX_SPAN,
        ) { span ->
            val usable = grid.width - grid.paddingStart - grid.paddingEnd
            if (usable <= 0 || span <= 0) return@spanFromWidth
            val cell = usable / span
            val minIcon = resources.getDimensionPixelSize(R.dimen.df_icon_size)
            val maxIcon = resources.getDimensionPixelSize(R.dimen.df_icon_size_max)
            adapter.setIconSize((cell * ICON_SHARE_OF_CELL).toInt().coerceIn(minIcon, maxIcon))
        }
    }

    private fun setUpChips() {
        IconStyleCatalog.all.forEach { style ->
            val chip = Chip(this).apply {
                id = View.generateViewId()
                text = if (style.tier == ContentTier.PRO) {
                    getString(R.string.browser_chip_pro, style.displayName, getString(R.string.tier_pro))
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
            val styleId = group.findViewById<Chip>(checked)?.tag as? String
                ?: return@setOnCheckedStateChangeListener
            viewModel.selectStyle(IconStyleCatalog.forId(styleId))
        }
    }

    private fun setUpSearch() {
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setQuery(s?.toString().orEmpty())
                searchClear.visibility = if (s.isNullOrEmpty()) View.GONE else View.VISIBLE
            }

            override fun afterTextChanged(s: Editable?) = Unit
        })
        searchClear.setOnClickListener { search.text = null }
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

                    // Counts the user can act on: how many apps are in front of them right now, and
                    // how many of the whole set DarkFrame draws by hand. The second is the one that
                    // answers "is my app supported", which is the question this screen exists for.
                    coverage.text = when {
                        state.loading -> getString(R.string.browser_loading)
                        state.query.isNotEmpty() -> resources.getQuantityString(
                            R.plurals.browser_matches,
                            state.visibleApps.size,
                            state.visibleApps.size,
                            state.totalApps,
                        )
                        else -> resources.getQuantityString(
                            R.plurals.browser_coverage,
                            state.totalApps,
                            state.totalApps,
                            state.curatedApps,
                        )
                    }

                    val empty = !state.loading && state.visibleApps.isEmpty()
                    message.visibility = if (empty) View.VISIBLE else View.GONE
                    if (empty) {
                        message.text = if (state.query.isNotEmpty()) {
                            getString(R.string.browser_no_matches, state.query)
                        } else {
                            getString(R.string.browser_empty)
                        }
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

    private companion object {
        const val MIN_SPAN = 3
        const val MAX_SPAN = 10

        /**
         * How much of a grid cell the icon fills, leaving room for the label beneath it.
         *
         * Under the preview cap at every column width the grid can produce, so growing the icon
         * with the screen never pushes a render towards export resolution.
         */
        const val ICON_SHARE_OF_CELL = 0.62f
    }
}
