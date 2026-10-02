package com.darkframe.icons.ui.common

import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * Column count from the measured width, recomputed on every layout.
 *
 * This is the whole responsive strategy for DarkFrame's grids, and it is deliberately not
 * `smallestScreenWidthDp`: a resource qualifier is resolved when the activity is created, so a
 * Fold that opens while a screen is already visible keeps the folded column count until something
 * recreates it. Measuring instead means the inner screen gets its columns the moment it appears.
 *
 * The change is applied in a post, because `setSpanCount` requests a layout and doing that from
 * inside a layout pass is deferred by the framework anyway, with a warning.
 */
fun RecyclerView.spanFromWidth(
    targetCellPx: Int,
    minSpan: Int = 1,
    maxSpan: Int = 12,
    onSpanChanged: ((Int) -> Unit)? = null,
) {
    val manager = layoutManager as? GridLayoutManager ?: return
    addOnLayoutChangeListener { _, left, _, right, _, _, _, _, _ ->
        val usable = right - left - paddingStart - paddingEnd
        if (usable <= 0 || targetCellPx <= 0) return@addOnLayoutChangeListener
        val span = (usable / targetCellPx).coerceIn(minSpan, maxSpan)
        if (span == manager.spanCount) return@addOnLayoutChangeListener
        post {
            manager.spanCount = span
            onSpanChanged?.invoke(span)
        }
    }
}
