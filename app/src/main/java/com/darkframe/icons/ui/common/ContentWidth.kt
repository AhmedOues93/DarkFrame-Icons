package com.darkframe.icons.ui.common

import android.content.res.Resources
import android.view.View
import androidx.core.view.updatePadding
import com.darkframe.icons.R

/**
 * Keeps a single-column page from stretching across a large screen.
 *
 * Grids answer a wide screen by adding columns, and that is handled where each grid is built. A page
 * that is one column of cards cannot do that, and the default behaviour — every card growing to the
 * full width of a Fold's inner screen — is exactly the stretched phone layout a large-screen layout
 * is supposed to avoid: a 700dp-wide button with a word in the middle of it.
 *
 * So the column is capped and centred, with the surplus taken as padding rather than as a margin, so
 * the page background still runs to both edges. Applied on every layout pass, because a fold changes
 * the width without recreating the Activity.
 */
fun View.constrainContentWidth() {
    val maxWidth = resources.getDimensionPixelSize(R.dimen.df_content_max_width)
    val baseStart = paddingStart
    val baseEnd = paddingEnd
    addOnLayoutChangeListener { view, left, _, right, _, _, _, _, _ ->
        val available = right - left
        if (available <= 0) return@addOnLayoutChangeListener
        val surplus = available - maxWidth
        val extra = if (surplus > 0) surplus / 2 else 0
        if (view.paddingStart == baseStart + extra) return@addOnLayoutChangeListener
        view.updatePadding(left = baseStart + extra, right = baseEnd + extra)
    }
}

/**
 * The width a preview inside a capped column will actually be drawn at.
 *
 * Derived from the window rather than from a measured view, because a measured width is 0 on the
 * first layout pass and real on the second — keying a preview off it renders the same image twice,
 * and the preview cache is keyed by size, so the first render is pure waste. Capped by the same value
 * [constrainContentWidth] uses, so a tablet does not allocate a 1200px-wide bitmap to show it at 560.
 */
fun Resources.contentColumnWidthPx(): Int {
    val margin = getDimensionPixelSize(R.dimen.df_screen_margin)
    val maxWidth = getDimensionPixelSize(R.dimen.df_content_max_width)
    return (displayMetrics.widthPixels - margin * 2).coerceAtMost(maxWidth - margin * 2)
        .coerceAtLeast(1)
}
