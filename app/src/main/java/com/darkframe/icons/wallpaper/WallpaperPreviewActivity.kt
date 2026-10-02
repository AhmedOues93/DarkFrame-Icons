package com.darkframe.icons.wallpaper

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.darkframe.icons.R
import com.darkframe.icons.billing.Entitlement
import com.darkframe.icons.billing.ProEntitlementStore
import com.darkframe.icons.data.FavoriteKind
import com.darkframe.icons.data.FavoritesStore
import com.darkframe.icons.engine.DarkFrameEngine
import com.darkframe.icons.engine.wallpaper.WallpaperCatalog
import com.darkframe.icons.engine.wallpaper.WallpaperOutcome
import com.darkframe.icons.engine.wallpaper.WallpaperSpec
import com.darkframe.icons.engine.wallpaper.WallpaperTarget
import com.darkframe.icons.model.ContentTier
import com.darkframe.icons.ui.ProActivity
import com.darkframe.icons.ui.common.DarkFrameActivity
import com.darkframe.icons.ui.common.applySystemBarPadding
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * One wallpaper, full screen, before it is applied.
 *
 * This replaces a dialog listing four verbs. A wallpaper is a picture: the only way to decide about
 * one is to see it at the size it will actually be, which on a Fold's inner screen is a very
 * different image from the thumbnail in the grid. So the preview is rendered at the window's own
 * resolution — the same render the apply would produce — and the controls sit over it.
 *
 * Tapping the image hides the controls, because the last thing in the way of judging a wallpaper is
 * a bar of buttons across the bottom of it.
 */
class WallpaperPreviewActivity : DarkFrameActivity() {

    private val engine by lazy { DarkFrameEngine.get(applicationContext) }
    private val favorites by lazy { FavoritesStore(this) }

    private lateinit var spec: WallpaperSpec
    private lateinit var image: ImageView
    private lateinit var chrome: View
    private lateinit var favourite: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val resolved = WallpaperCatalog.byId(intent.getStringExtra(EXTRA_ID))
        if (resolved == null) {
            finish()
            return
        }
        spec = resolved

        setContentView(R.layout.activity_wallpaper_preview)
        image = findViewById(R.id.preview_image)
        chrome = findViewById(R.id.preview_chrome)
        favourite = findViewById(R.id.preview_favourite)
        chrome.applySystemBarPadding(top = false, bottom = true)

        findViewById<TextView>(R.id.preview_title).text = spec.title
        findViewById<TextView>(R.id.preview_category).text = categoryLabel()

        image.contentDescription = getString(R.string.wallpaper_fullscreen_description, spec.title)
        image.setOnClickListener { toggleChrome() }

        bindFavourite()
        favourite.setOnClickListener {
            favorites.toggle(FavoriteKind.WALLPAPER, spec.id)
            bindFavourite()
        }

        bindApplyButtons()
        render()
    }

    /**
     * Rendered at the window's own pixel size, which is also what the apply will use.
     *
     * So the preview is not an approximation of the result: it is the result, drawn once and
     * answered from the wallpaper applier's own path.
     */
    private fun render() {
        val metrics = resources.displayMetrics
        lifecycleScope.launch {
            val bitmap = withContext(engine.renderDispatcher) {
                engine.wallpapers.preview(spec, metrics.widthPixels, metrics.heightPixels)
            }
            image.setImageBitmap(bitmap)
        }
    }

    private fun bindFavourite() {
        val saved = favorites.isFavorite(FavoriteKind.WALLPAPER, spec.id)
        favourite.text = getString(
            if (saved) R.string.wallpaper_unfavorite else R.string.wallpaper_favorite,
        )
        favourite.isSelected = saved
    }

    /**
     * Home, Lock and Both — and Lock and Both only where the platform really has a separate lock
     * wallpaper, rather than three buttons two of which quietly do the same thing.
     */
    private fun bindApplyButtons() {
        findViewById<TextView>(R.id.preview_apply_home).setOnClickListener {
            apply(WallpaperTarget.HOME)
        }
        val lock = findViewById<TextView>(R.id.preview_apply_lock)
        val both = findViewById<TextView>(R.id.preview_apply_both)
        if (engine.wallpapers.supportsSeparateLockScreen()) {
            lock.setOnClickListener { apply(WallpaperTarget.LOCK) }
            both.setOnClickListener { apply(WallpaperTarget.BOTH) }
        } else {
            lock.visibility = View.GONE
            both.visibility = View.GONE
        }
    }

    private fun apply(target: WallpaperTarget) {
        if (spec.tier == ContentTier.PRO && ProEntitlementStore(this).current() != Entitlement.PRO) {
            startActivity(Intent(this, ProActivity::class.java))
            return
        }
        lifecycleScope.launch {
            val outcome = withContext(engine.renderDispatcher) {
                engine.wallpapers.apply(spec, target)
            }
            val message = when (outcome) {
                is WallpaperOutcome.Applied -> when (outcome.target) {
                    WallpaperTarget.HOME -> getString(R.string.wallpaper_applied_home)
                    WallpaperTarget.LOCK -> getString(R.string.wallpaper_applied_lock)
                    WallpaperTarget.BOTH -> getString(R.string.wallpaper_applied_both)
                }

                is WallpaperOutcome.Failed -> getString(R.string.wallpaper_failed, outcome.reason)
            }
            AlertDialog.Builder(this@WallpaperPreviewActivity)
                .setMessage(message)
                .setPositiveButton(R.string.close, null)
                .show()
        }
    }

    private fun toggleChrome() {
        chrome.visibility = if (chrome.visibility == View.VISIBLE) View.GONE else View.VISIBLE
    }

    private fun categoryLabel(): String = getString(
        R.string.wallpaper_preview_subtitle,
        spec.category.name.lowercase().replaceFirstChar { it.uppercase() },
    )

    companion object {
        private const val EXTRA_ID = "wallpaper_id"

        fun intent(context: Context, spec: WallpaperSpec): Intent =
            Intent(context, WallpaperPreviewActivity::class.java).putExtra(EXTRA_ID, spec.id)
    }
}
