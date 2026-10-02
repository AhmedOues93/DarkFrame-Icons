package com.darkframe.icons.ui.common

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * Even gaps between grid items.
 *
 * Half the gap on every side of every item, which adds up to one full gap between neighbours and
 * half a gap at the edges — where the parent's own padding takes over. The obvious alternative, a
 * margin on each item, doubles between neighbours and leaves the first and last columns inset
 * differently from the rest.
 */
class SpacingDecoration(private val gap: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State,
    ) {
        outRect.set(gap / 2, gap / 2, gap / 2, gap / 2)
    }
}
