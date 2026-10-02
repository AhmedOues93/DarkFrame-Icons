package com.darkframe.icons.wallpaper

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.darkframe.icons.R
import com.darkframe.icons.engine.wallpaper.WallpaperCatalog
import com.darkframe.icons.engine.wallpaper.WallpaperSpec
import com.darkframe.icons.model.WallpaperCategory
import com.darkframe.icons.ui.common.DarkFrameActivity
import com.darkframe.icons.ui.common.applySystemBarPadding
import com.darkframe.icons.ui.common.spanFromWidth
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

/**
 * Browse DarkFrame's wallpapers by category.
 *
 * Every wallpaper here is drawn on the device at the size it is needed, so the grid shows thumbnails
 * at cell size and the full panel resolution is only ever produced by the preview screen and the
 * apply. On a Fold that matters twice over: the inner and cover screens want very different images,
 * and both come from the same few lines of description.
 *
 * Choosing happens on [WallpaperPreviewActivity], not here — a thumbnail and a word is not enough to
 * decide about a picture.
 */
class WallpaperActivity : DarkFrameActivity() {

    private lateinit var grid: RecyclerView
    private lateinit var adapter: WallpaperAdapter
    private var category: WallpaperCategory = WallpaperCatalog.categoriesWithContent().first()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallpapers)
        findViewById<View>(R.id.wallpapers_root).applySystemBarPadding()

        grid = findViewById(R.id.wallpaper_grid)
        adapter = WallpaperAdapter(
            loader = EngineWallpaperPreviewLoader(this),
            scope = lifecycleScope,
            onClick = ::openWallpaper,
        )
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
        WallpaperCategory.FROST -> "Frost"
        WallpaperCategory.MINIMAL -> "Minimal"
        WallpaperCategory.ABSTRACT -> "Abstract"
        WallpaperCategory.FOLD -> "Fold"
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

    /**
     * The Pro gate lives on the preview screen, not here.
     *
     * Seeing a Pro wallpaper at full size is how someone decides to buy it; what the gate withholds
     * is setting it as the wallpaper.
     */
    private fun openWallpaper(spec: WallpaperSpec) {
        startActivity(WallpaperPreviewActivity.intent(this, spec))
    }

    private companion object {
        /** Thumbnails are taller than wide, like the screen they are a picture of. */
        const val THUMBNAIL_ASPECT = 1.5f
    }
}
