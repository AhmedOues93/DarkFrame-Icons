package com.darkframe.icons.ui.common

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
