package com.darkframe.icons.ui.common

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

/**
 * Edge-to-edge helpers.
 *
 * DarkFrame draws its own ground behind the status and navigation bars, which is what makes a dark
 * app look like one surface rather than a page between two grey strips. The cost is that every
 * scrolling container has to inset its own content, or the first row sits under the clock.
 *
 * Applied to padding rather than margins so the scrim and background still run to the screen edge.
 */
fun View.applySystemBarPadding(top: Boolean = true, bottom: Boolean = true) {
    val initialTop = paddingTop
    val initialBottom = paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val bars = insets.getInsets(
            WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout(),
        )
        view.updatePadding(
            top = if (top) initialTop + bars.top else initialTop,
            bottom = if (bottom) initialBottom + bars.bottom else initialBottom,
        )
        insets
    }
    ViewCompat.requestApplyInsets(this)
}
