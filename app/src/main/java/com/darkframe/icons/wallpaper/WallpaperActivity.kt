package com.darkframe.icons.wallpaper

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.LruCache
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.darkframe.icons.R
import com.darkframe.icons.engine.DarkFrameEngine
import com.darkframe.icons.engine.wallpaper.WallpaperCatalog
import com.darkframe.icons.engine.wallpaper.WallpaperSpec
import com.darkframe.icons.model.WallpaperCategory
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
        WallpaperCategory.FROST -> "Frost"
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

    /**
     * Opens the full-screen preview.
     *
     * The grid used to raise a dialog of four verbs here. Deciding about a wallpaper from a thumbnail
     * and a word is not deciding, so the picture comes first and the actions live under it — and the
     * Pro gate moved with them, because browsing a Pro wallpaper at full size is how someone decides
     * to buy it.
     */
    private fun openWallpaper(spec: WallpaperSpec) {
        startActivity(WallpaperPreviewActivity.intent(this, spec))
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
