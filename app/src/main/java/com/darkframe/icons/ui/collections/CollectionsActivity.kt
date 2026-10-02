package com.darkframe.icons.ui.collections

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.darkframe.icons.R
import com.darkframe.icons.engine.domain.CompleteLook
import com.darkframe.icons.engine.domain.LookCatalog
import com.darkframe.icons.model.ContentTier
import com.darkframe.icons.ui.common.DarkFrameActivity
import com.darkframe.icons.ui.common.SpacingDecoration
import com.darkframe.icons.ui.common.applySystemBarPadding
import com.darkframe.icons.ui.common.spanFromWidth
import com.darkframe.icons.ui.home.CollectionCardAdapter
import com.darkframe.icons.ui.home.EngineLookPreviewLoader
import com.darkframe.icons.ui.look.LookDetailActivity

/**
 * All six collections, at a size worth judging them at.
 *
 * The home screen carries a horizontal row of the same cards, which is right for picking up where
 * you left off and wrong for choosing: a row shows two and a half cards and hides the rest behind a
 * swipe. This screen is the one to open when the question is "which of these do I want", so the
 * cards are as large as the screen allows and all six are on it at once wherever they fit.
 *
 * Previews come from the engine's cache, which the home screen has usually already filled — so
 * arriving here normally costs no rendering at all.
 */
class CollectionsActivity : DarkFrameActivity() {

    private lateinit var grid: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_collections)

        findViewById<View>(R.id.collections_header).applySystemBarPadding(top = true, bottom = false)
        findViewById<TextView>(R.id.collections_summary).text = summary()

        grid = findViewById(R.id.collections_grid)
        grid.applySystemBarPadding(top = false, bottom = true)

        val cardWidth = estimatedCardWidthPx()
        val adapter = CollectionCardAdapter(
            loader = EngineLookPreviewLoader(this),
            scope = lifecycleScope,
            previewWidthPx = cardWidth - cardPaddingPx() * 2,
            previewHeightPx = ((cardWidth - cardPaddingPx() * 2) * PREVIEW_ASPECT).toInt(),
            cardWidthPx = null,
            onClick = ::open,
        )
        grid.layoutManager = GridLayoutManager(this, 1)
        grid.adapter = adapter
        grid.addItemDecoration(
            SpacingDecoration(resources.getDimensionPixelSize(R.dimen.df_space_3)),
        )
        grid.spanFromWidth(
            targetCellPx = resources.getDimensionPixelSize(R.dimen.df_collection_grid_target),
            minSpan = 1,
            maxSpan = 4,
        )
        adapter.submitList(LookCatalog.all)
    }

    private fun summary(): String {
        val free = LookCatalog.all.count { it.tier == ContentTier.FREE }
        return getString(R.string.collections_summary, LookCatalog.all.size, free)
    }

    /**
     * The preview size the cards will be asked for, computed once from the window.
     *
     * One size for every card, taken from the narrowest column the grid can produce, rather than a
     * per-card measurement. Measured widths arrive as 0 on the first layout pass and real on the
     * second, so keying previews off them would render each one twice — and the preview cache is
     * keyed by size, so the first render would be pure waste.
     */
    private fun estimatedCardWidthPx(): Int {
        val target = resources.getDimensionPixelSize(R.dimen.df_collection_grid_target)
        val margin = resources.getDimensionPixelSize(R.dimen.df_screen_margin)
        val usable = resources.displayMetrics.widthPixels - margin * 2
        val span = (usable / target).coerceIn(1, 4)
        val gaps = resources.getDimensionPixelSize(R.dimen.df_space_3) * (span - 1)
        return ((usable - gaps) / span).coerceAtLeast(target)
    }

    private fun cardPaddingPx() = resources.getDimensionPixelSize(R.dimen.df_space_2)

    private fun open(look: CompleteLook) {
        startActivity(LookDetailActivity.intent(this, look))
    }

    companion object {
        /** Preview cards are taller than wide, like the phone they are showing. */
        private const val PREVIEW_ASPECT = 1.35f

        fun intent(context: Context) = Intent(context, CollectionsActivity::class.java)
    }
}
