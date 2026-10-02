package com.darkframe.icons.ui

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.lifecycle.lifecycleScope
import com.darkframe.icons.MainActivity
import com.darkframe.icons.R
import com.darkframe.icons.engine.domain.CompleteLook
import com.darkframe.icons.engine.domain.LookCatalog
import com.darkframe.icons.ui.common.DarkFrameActivity
import com.darkframe.icons.ui.common.contentColumnWidthPx
import com.darkframe.icons.ui.common.SectionScreen
import com.darkframe.icons.ui.home.EngineLookPreviewLoader
import kotlinx.coroutines.launch

/**
 * Three screens, each with a real picture.
 *
 * Every page shows a different look, rendered through the engine rather than illustrated. That is
 * doing two jobs at once: it explains what DarkFrame is faster than the sentence next to it, and it
 * puts three of the six looks in front of someone before they have tapped anything — which is the
 * actual argument for the product.
 *
 * Only the first page offers Skip. A skip link on every page is three chances to leave a flow that
 * is three screens long.
 */
class OnboardingActivity : DarkFrameActivity() {

    private val previews by lazy { EngineLookPreviewLoader(this) }
    private var page = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        render()
    }

    private fun render() {
        val content = PAGES[page]
        val screen = SectionScreen(this)
            .setUp(getString(content.title), getString(content.body))

        screen.custom(previewView(content.look))
        screen.custom(dots())

        screen.primaryButton(
            if (page == PAGES.lastIndex) getString(R.string.onboarding_done)
            else getString(R.string.onboarding_next),
        ) {
            if (page == PAGES.lastIndex) {
                finishOnboarding()
            } else {
                page++
                render()
            }
        }
        if (page == 0) {
            screen.secondaryButton(getString(R.string.onboarding_skip)) { finishOnboarding() }
        }
    }

    private fun previewView(look: CompleteLook): ImageView {
        val height = resources.getDimensionPixelSize(R.dimen.df_hero_height)
        val width = resources.contentColumnWidthPx()
        val view = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundResource(R.drawable.df_preview_clip)
            clipToOutline = true
            contentDescription = getString(R.string.look_preview_description, look.name)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, height)
        }
        val warm = previews.peek(look, width, height)
        if (warm != null) {
            view.setImageBitmap(warm)
        } else {
            lifecycleScope.launch { view.setImageBitmap(previews.load(look, width, height)) }
        }
        return view
    }

    /**
     * Three dots, so the flow says how long it is.
     *
     * Built in code rather than as a layout because it is three views and a selected state; a layout
     * plus an adapter for that would be more machinery than the thing it draws.
     */
    private fun dots(): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )
        }
        val size = resources.getDimensionPixelSize(R.dimen.df_space_2)
        val gap = resources.getDimensionPixelSize(R.dimen.df_space_1)
        PAGES.indices.forEach { index ->
            val dot = View(this)
            dot.setBackgroundResource(R.drawable.df_dot)
            dot.isSelected = index == page
            dot.layoutParams = LinearLayout.LayoutParams(size, size).apply {
                marginStart = gap
                marginEnd = gap
            }
            row.addView(dot)
        }
        return row
    }

    private fun finishOnboarding() {
        getSharedPreferences("darkframe", MODE_PRIVATE).edit().putBoolean("onboarded", true).apply()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private data class Page(val title: Int, val body: Int, val look: CompleteLook)

    private companion object {
        /** Three different looks, so the flow shows the range rather than the same tile three times. */
        val PAGES = listOf(
            Page(R.string.onboarding_1_title, R.string.onboarding_1_body, LookCatalog.noir),
            Page(R.string.onboarding_2_title, R.string.onboarding_2_body, LookCatalog.colorPop),
            Page(R.string.onboarding_3_title, R.string.onboarding_3_body, LookCatalog.pureAmoled),
        )
    }
}
