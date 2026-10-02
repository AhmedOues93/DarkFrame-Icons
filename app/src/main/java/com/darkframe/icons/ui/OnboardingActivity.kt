package com.darkframe.icons.ui

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.lifecycle.lifecycleScope
import com.darkframe.icons.MainActivity
import com.darkframe.icons.R
import com.darkframe.icons.engine.DarkFrameEngine
import com.darkframe.icons.engine.domain.LookCatalog
import com.darkframe.icons.ui.common.DarkFrameActivity
import com.darkframe.icons.ui.common.SectionScreen
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Three screens, each with a picture.
 *
 * The first one shows a real look preview rather than an illustration, because the fastest way to
 * explain what DarkFrame does is to show it doing it.
 */
class OnboardingActivity : DarkFrameActivity() {

    private var page = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        render()
    }

    private fun render() {
        val content = PAGES[page]
        val screen = SectionScreen(this)
            .setUp(getString(content.title), getString(content.body))

        if (page == 0) screen.custom(previewView())

        screen.primaryButton(
            if (page == PAGES.lastIndex) getString(R.string.onboarding_done)
            else getString(R.string.onboarding_next),
        ) {
            if (page == PAGES.lastIndex) finishOnboarding() else {
                page++
                render()
            }
        }
        if (page < PAGES.lastIndex) {
            screen.secondaryButton(getString(R.string.onboarding_skip)) { finishOnboarding() }
        }
    }

    private fun previewView(): ImageView {
        val look = LookCatalog.default
        val engine = DarkFrameEngine.get(applicationContext)
        val height = resources.getDimensionPixelSize(R.dimen.df_hero_height)
        val width = resources.displayMetrics.widthPixels -
            resources.getDimensionPixelSize(R.dimen.df_screen_margin) * 2
        val view = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundResource(R.drawable.df_preview_clip)
            clipToOutline = true
            contentDescription = getString(R.string.look_preview_description, look.name)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, height)
        }
        val warm: Bitmap? = engine.lookPreviews.peek(look, width, height)
        if (warm != null) {
            view.setImageBitmap(warm)
        } else {
            lifecycleScope.launch {
                val bitmap = withContext(engine.renderDispatcher) {
                    engine.lookPreviews.get(look, width, height)
                }
                view.setImageBitmap(bitmap)
            }
        }
        return view
    }

    private fun finishOnboarding() {
        getSharedPreferences("darkframe", MODE_PRIVATE).edit().putBoolean("onboarded", true).apply()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private data class Page(val title: Int, val body: Int)

    private companion object {
        val PAGES = listOf(
            Page(R.string.onboarding_1_title, R.string.onboarding_1_body),
            Page(R.string.onboarding_2_title, R.string.onboarding_2_body),
            Page(R.string.onboarding_3_title, R.string.onboarding_3_body),
        )
    }
}
