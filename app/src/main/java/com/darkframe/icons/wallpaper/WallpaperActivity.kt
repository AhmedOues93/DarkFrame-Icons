package com.darkframe.icons.wallpaper

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.LruCache
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
import com.darkframe.icons.model.WallpaperCategory
import com.darkframe.icons.ui.ProActivity
import com.darkframe.icons.ui.common.DarkFrameActivity
import com.darkframe.icons.ui.common.applySystemBarPadding
import com.darkframe.icons.ui.common.spanFromWidth
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Browse and apply DarkFrame wallpapers.
 *
 * Every wallpaper here is drawn on the device at the size it is needed, so the grid shows thumbnails
 * and only the one being applied is rendered at the panel's real resolution. On a Fold that matters
 * twice over: the inner and cover screens want very different images, and both are produced from the
 * same few lines of description.
 */
class WallpaperActivity : DarkFrameActivity(), WallpaperPreviewLoader {

    private val engine by lazy { DarkFrameEngine.get(applicationContext) }
    private val favorites by lazy { FavoritesStore(this) }

    private lateinit var grid: RecyclerView
    private lateinit var adapter: WallpaperAdapter
    private var category: WallpaperCategory = WallpaperCatalog.categoriesWithContent().first()

    /**
     * Thumbnail cache.
     *
     * Bounded like every other bitmap store in the app: a category scroll must not accumulate
     * full-size bitmaps, and returning to a category should not redraw what was already drawn.
     */
    private val thumbnails = object : LruCache<String, Bitmap>(THUMBNAIL_BUDGET_BYTES) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallpapers)
        findViewById<View>(R.id.wallpapers_root).applySystemBarPadding()

        grid = findViewById(R.id.wallpaper_grid)
        adapter = WallpaperAdapter(this, lifecycleScope, ::openWallpaper)
        grid.layoutManager = GridLayoutManager(this, 2)
        grid.adapter = adapter
        grid.spanFromWidth(
            targetCellPx = resources.getDimensionPixelSize(R.dimen.df_collection_card_width),
            minSpan = 2,
            maxSpan = 6,
            onSpanChanged = { updateCellSize() },
        )
        grid.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> updateCellSize() }

        setUpCategories()
        showCategory(category)
    }

    private fun setUpCategories() {
        val group = findViewById<ChipGroup>(R.id.wallpaper_categories)
        WallpaperCatalog.categoriesWithContent().forEach { value ->
            val chip = Chip(this).apply {
                id = View.generateViewId()
                text = label(value)
                isCheckable = true
                isChecked = value == category
                tag = value
            }
            group.addView(chip)
        }
        group.setOnCheckedStateChangeListener { chipGroup, checked ->
            val id = checked.firstOrNull() ?: return@setOnCheckedStateChangeListener
            val value = chipGroup.findViewById<Chip>(id)?.tag as? WallpaperCategory
                ?: return@setOnCheckedStateChangeListener
            category = value
            showCategory(value)
        }
    }

    private fun label(category: WallpaperCategory): String = when (category) {
        WallpaperCategory.AMOLED -> "AMOLED"
        WallpaperCategory.CARBON -> "Carbon"
        WallpaperCategory.GRAPHITE -> "Graphite"
        WallpaperCategory.TITANIUM -> "Titanium"
        WallpaperCategory.GLASS -> "Glass"
        WallpaperCategory.MINIMAL -> "Minimal"
        WallpaperCategory.ABSTRACT -> "Abstract"
        WallpaperCategory.FOLD -> "Fold"
        WallpaperCategory.LIGHT -> "Light"
    }

    private fun showCategory(value: WallpaperCategory) {
        adapter.submitList(WallpaperCatalog.inCategory(value))
    }

    /** Cell size follows the measured grid, so a fold changes the thumbnails rather than stretching. */
    private fun updateCellSize() {
        val manager = grid.layoutManager as? GridLayoutManager ?: return
        val usable = grid.width - grid.paddingStart - grid.paddingEnd
        if (usable <= 0 || manager.spanCount <= 0) return
        val cell = usable / manager.spanCount - resources.getDimensionPixelSize(R.dimen.df_space_2)
        if (cell <= 0) return
        adapter.setCellSize(cell, (cell * THUMBNAIL_ASPECT).toInt())
    }

    // ---- applying ----------------------------------------------------------------------------

    private fun openWallpaper(spec: WallpaperSpec) {
        if (spec.tier == ContentTier.PRO && ProEntitlementStore(this).current() != Entitlement.PRO) {
            startActivity(Intent(this, ProActivity::class.java))
            return
        }
        val saved = favorites.isFavorite(FavoriteKind.WALLPAPER, spec.id)
        val actions = buildList {
            add(getString(R.string.wallpaper_apply_home))
            if (engine.wallpapers.supportsSeparateLockScreen()) {
                add(getString(R.string.wallpaper_apply_lock))
                add(getString(R.string.wallpaper_apply_both))
            }
            add(
                if (saved) getString(R.string.wallpaper_unfavorite)
                else getString(R.string.wallpaper_favorite),
            )
        }
        AlertDialog.Builder(this)
            .setTitle(spec.title)
            .setItems(actions.toTypedArray()) { _, which ->
                when (actions[which]) {
                    getString(R.string.wallpaper_apply_home) -> apply(spec, WallpaperTarget.HOME)
                    getString(R.string.wallpaper_apply_lock) -> apply(spec, WallpaperTarget.LOCK)
                    getString(R.string.wallpaper_apply_both) -> apply(spec, WallpaperTarget.BOTH)
                    else -> favorites.toggle(FavoriteKind.WALLPAPER, spec.id)
                }
            }
            .setNegativeButton(R.string.close, null)
            .show()
    }

    private fun apply(spec: WallpaperSpec, target: WallpaperTarget) {
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
                WallpaperOutcome.LockNotSupported -> getString(R.string.wallpaper_lock_unsupported)
            }
            AlertDialog.Builder(this@WallpaperActivity)
                .setMessage(message)
                .setPositiveButton(R.string.close, null)
                .show()
        }
    }

    // ---- previews ----------------------------------------------------------------------------

    private fun key(spec: WallpaperSpec, w: Int, h: Int) = "${spec.id}:${w}x$h"

    override fun peek(spec: WallpaperSpec, widthPx: Int, heightPx: Int): Bitmap? =
        thumbnails.get(key(spec, widthPx, heightPx))

    override suspend fun load(spec: WallpaperSpec, widthPx: Int, heightPx: Int): Bitmap =
        withContext(engine.renderDispatcher) {
            val cacheKey = key(spec, widthPx, heightPx)
            thumbnails.get(cacheKey) ?: engine.wallpapers.preview(spec, widthPx, heightPx)
                .also { thumbnails.put(cacheKey, it) }
        }

    private companion object {
        const val THUMBNAIL_ASPECT = 1.5f
        const val THUMBNAIL_BUDGET_BYTES = 8 * 1024 * 1024
    }
}
