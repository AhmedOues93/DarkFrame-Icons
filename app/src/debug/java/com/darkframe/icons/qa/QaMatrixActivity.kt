package com.darkframe.icons.qa

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.darkframe.icons.R
import com.darkframe.icons.ui.common.DarkFrameActivity
import com.darkframe.icons.ui.common.applySystemBarPadding
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch

/**
 * Debug-only visual QA: every installed app, rendered in all six collections, side by side.
 *
 * The renderer is covered by unit tests at the level of its maths, and unit tests cannot see that a
 * glyph is clipped or that an icon has two backgrounds. This screen is how those get found, and the
 * reason it is a matrix rather than a gallery is that the defects worth catching are *comparative*:
 * a Frost cell only looks wrong next to the Noir cell for the same app, and inconsistent optical
 * sizing is invisible until the same logo appears six times in a row.
 *
 * What each control is for:
 *
 *  - **Cell size** — sizing and centring errors show up at a glance when cells are small and a
 *    hundred of them are on screen; clipping, mask edges and soft upscaling need a large cell.
 *  - **Checkerboard** — a flat magenta ground behind each cell. A transparent gap inside an icon,
 *    a container that did not draw, or artwork seated past its tile all become obvious; against the
 *    app's own dark background they are not.
 *  - **Filter** — to go straight back to the app that looked wrong.
 *
 * Lives in `src/debug`, so it is absent from a release APK rather than merely unreachable. It draws
 * nothing itself: every bitmap comes from the engine, through the same path the browser uses, at
 * the same bucketed preview sizes — so opening it cannot produce renders the real app would not.
 */
class QaMatrixActivity : DarkFrameActivity() {

    // Through the ViewModelStore, not constructed directly: the catalog read is the expensive part
    // of this screen and a rotation or a fold should not repeat it.
    private val viewModel: QaMatrixViewModel by viewModels()

    private lateinit var rows: RecyclerView
    private lateinit var summary: TextView
    private lateinit var adapter: QaMatrixAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_DarkFrame)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qa_matrix)

        findViewById<View>(R.id.qa_header).applySystemBarPadding(top = true, bottom = false)
        summary = findViewById(R.id.qa_summary)
        rows = findViewById(R.id.qa_rows)
        rows.applySystemBarPadding(top = false, bottom = true)

        adapter = QaMatrixAdapter(
            loader = viewModel,
            scope = lifecycleScope,
            cellSizePx = cellPxFor(DEFAULT_CELL_DP),
        )
        rows.layoutManager = LinearLayoutManager(this)
        rows.adapter = adapter
        // Each row holds six cells that are rendered on bind; recycling six views per row is the
        // reason the pool default of five is not enough here.
        rows.setItemViewCacheSize(4)

        setUpFilter()
        setUpSizes()
        setUpChecker()
        observe()
    }

    override fun onStop() {
        // The matrix renders the same app six times over, so it fills the cache faster than any
        // other screen. Trimming on the way out keeps a QA session from leaving a large cache
        // behind on a device that is also being used to test the real app's behaviour.
        viewModel.trimCache()
        super.onStop()
    }

    private fun setUpFilter() {
        findViewById<EditText>(R.id.qa_filter).addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setQuery(s?.toString().orEmpty())
            }

            override fun afterTextChanged(s: Editable?) = Unit
        })
    }

    private fun setUpSizes() {
        val group = findViewById<ChipGroup>(R.id.qa_sizes)
        CELL_SIZES_DP.forEach { dp ->
            group.addView(
                Chip(this).apply {
                    id = View.generateViewId()
                    text = "$dp"
                    isCheckable = true
                    isChecked = dp == DEFAULT_CELL_DP
                    tag = dp
                },
            )
        }
        group.setOnCheckedStateChangeListener { chips, checkedIds ->
            val checked = checkedIds.firstOrNull() ?: return@setOnCheckedStateChangeListener
            val dp = chips.findViewById<Chip>(checked)?.tag as? Int ?: return@setOnCheckedStateChangeListener
            adapter.setCellSize(cellPxFor(dp))
        }
    }

    private fun setUpChecker() {
        findViewById<CheckBox>(R.id.qa_checker).setOnCheckedChangeListener { _, checked ->
            adapter.setChecker(checked)
        }
    }

    private fun observe() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    adapter.submitList(state.visibleApps)
                    summary.text = if (state.loading) {
                        getString(R.string.qa_loading)
                    } else {
                        getString(R.string.qa_count, state.totalApps, state.curatedApps)
                    }
                }
            }
        }
    }

    private fun cellPxFor(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    private companion object {
        val CELL_SIZES_DP = listOf(48, 72, 112, 160)
        const val DEFAULT_CELL_DP = 72
    }
}
