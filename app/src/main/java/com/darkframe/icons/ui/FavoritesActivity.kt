package com.darkframe.icons.ui

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.darkframe.icons.R
import com.darkframe.icons.data.FavoriteKind
import com.darkframe.icons.data.FavoritesStore
import com.darkframe.icons.engine.DarkFrameEngine
import com.darkframe.icons.engine.data.StylePreferenceStore
import com.darkframe.icons.engine.domain.AppIdentity
import com.darkframe.icons.engine.domain.CompleteLook
import com.darkframe.icons.engine.domain.LookCatalog
import com.darkframe.icons.engine.wallpaper.WallpaperCatalog
import com.darkframe.icons.engine.wallpaper.WallpaperSpec
import com.darkframe.icons.ui.browser.EngineThemedIconLoader
import com.darkframe.icons.ui.browser.IconGridAdapter
import com.darkframe.icons.ui.common.DarkFrameActivity
import com.darkframe.icons.ui.common.SpacingDecoration
import com.darkframe.icons.ui.common.applySystemBarPadding
import com.darkframe.icons.ui.common.spanFromWidth
import com.darkframe.icons.ui.detail.IconDetailActivity
import com.darkframe.icons.ui.home.CollectionCardAdapter
import com.darkframe.icons.ui.home.EngineLookPreviewLoader
import com.darkframe.icons.ui.look.LookDetailActivity
import com.darkframe.icons.wallpaper.EngineWallpaperPreviewLoader
import com.darkframe.icons.wallpaper.WallpaperAdapter
import com.darkframe.icons.wallpaper.WallpaperPreviewActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Everything the user has saved — as pictures, and leading where it should.
 *
 * The previous version was three lists of text, and the app section was the worst of it: it showed
 * raw `package/activity` strings and every row opened the whole browser. That is a bookmark list that
 * cannot be used as one. Each kind now shows its own artwork, through the same loaders the screens it
 * came from use, and each row opens the thing itself.
 *
 * Saved apps are resolved against the installed catalog, which is one package read on a screen the
 * user explicitly opened — and it is also what makes an app that has since been uninstalled disappear
 * from the list instead of sitting there as an unopenable row.
 */
class FavoritesActivity : DarkFrameActivity() {

    private val store by lazy { FavoritesStore(this) }
    private val engine by lazy { DarkFrameEngine.get(applicationContext) }

    private lateinit var empty: TextView
    private lateinit var looksSection: View
    private lateinit var wallpapersSection: View
    private lateinit var appsSection: View

    private var lookAdapter: CollectionCardAdapter? = null
    private var wallpaperAdapter: WallpaperAdapter? = null
    private var appAdapter: IconGridAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)
        findViewById<View>(R.id.favorites_scroll).applySystemBarPadding()

        empty = findViewById(R.id.favorites_empty)
        looksSection = findViewById(R.id.favorites_looks_section)
        wallpapersSection = findViewById(R.id.favorites_wallpapers_section)
        appsSection = findViewById(R.id.favorites_apps_section)

        setUpLooks()
        setUpWallpapers()
        setUpApps()
    }

    override fun onResume() {
        super.onResume()
        // Favourites change on other screens, so the lists are rebuilt on every return rather than
        // once at creation. The underlying previews are cached, so this costs no rendering.
        bindLooks()
        bindWallpapers()
        bindApps()
    }

    // ---- looks ----------------------------------------------------------------------------------

    private fun setUpLooks() {
        val row = findViewById<RecyclerView>(R.id.favorites_looks)
        val width = resources.getDimensionPixelSize(R.dimen.df_collection_card_width) -
            resources.getDimensionPixelSize(R.dimen.df_space_2) * 2
        lookAdapter = CollectionCardAdapter(
            loader = EngineLookPreviewLoader(this),
            scope = lifecycleScope,
            previewWidthPx = width,
            previewHeightPx = (width * PREVIEW_ASPECT).toInt(),
            onClick = ::openLook,
        )
        row.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        row.adapter = lookAdapter
    }

    private fun bindLooks() {
        val saved = LookCatalog.all.filter { store.isFavorite(FavoriteKind.LOOK, it.id) }
        lookAdapter?.submitList(saved)
        looksSection.visibility = if (saved.isEmpty()) View.GONE else View.VISIBLE
        updateEmptyState()
    }

    private fun openLook(look: CompleteLook) {
        startActivity(LookDetailActivity.intent(this, look))
    }

    // ---- wallpapers -----------------------------------------------------------------------------

    private fun setUpWallpapers() {
        val row = findViewById<RecyclerView>(R.id.favorites_wallpapers)
        wallpaperAdapter = WallpaperAdapter(
            loader = EngineWallpaperPreviewLoader(this),
            scope = lifecycleScope,
            onClick = ::openWallpaper,
        )
        row.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        row.adapter = wallpaperAdapter
        val cell = resources.getDimensionPixelSize(R.dimen.df_favorite_thumb)
        wallpaperAdapter?.setCellSize(cell, (cell * THUMBNAIL_ASPECT).toInt())
    }

    private fun bindWallpapers() {
        val saved = WallpaperCatalog.all.filter { store.isFavorite(FavoriteKind.WALLPAPER, it.id) }
        wallpaperAdapter?.submitList(saved)
        wallpapersSection.visibility = if (saved.isEmpty()) View.GONE else View.VISIBLE
        updateEmptyState()
    }

    private fun openWallpaper(spec: WallpaperSpec) {
        startActivity(WallpaperPreviewActivity.intent(this, spec))
    }

    // ---- apps -----------------------------------------------------------------------------------

    private fun setUpApps() {
        val grid = findViewById<RecyclerView>(R.id.favorites_apps)
        appAdapter = IconGridAdapter(
            loader = EngineThemedIconLoader(this),
            scope = lifecycleScope,
            iconSizePx = resources.getDimensionPixelSize(R.dimen.df_icon_size),
            onClick = ::openApp,
        )
        grid.layoutManager = GridLayoutManager(this, MIN_APP_SPAN)
        grid.adapter = appAdapter
        grid.addItemDecoration(SpacingDecoration(resources.getDimensionPixelSize(R.dimen.df_space_1)))
        grid.spanFromWidth(
            targetCellPx = resources.getDimensionPixelSize(R.dimen.df_grid_cell_target),
            minSpan = MIN_APP_SPAN,
            maxSpan = MAX_APP_SPAN,
        )
        // Nested in a scrolling page, so the grid has to lay out at its full height rather than
        // scroll inside itself — two scrollable regions in one column is unusable.
        grid.isNestedScrollingEnabled = false
    }

    private fun bindApps() {
        lifecycleScope.launch {
            val savedKeys = store.idsOf(FavoriteKind.APP)
            if (savedKeys.isEmpty()) {
                appAdapter?.submitList(emptyList())
                appsSection.visibility = View.GONE
                updateEmptyState()
                return@launch
            }
            // Resolved against the installed catalog, which also quietly drops anything the user has
            // since uninstalled — a saved row that cannot be opened is worse than no row.
            val installed = withContext(Dispatchers.IO) { engine.installedApps.loadCatalog() }
            val saved = installed.filter { it.componentKey in savedKeys }
            appAdapter?.submitList(saved)
            appsSection.visibility = if (saved.isEmpty()) View.GONE else View.VISIBLE
            updateEmptyState()
        }
    }

    private fun openApp(identity: AppIdentity) {
        startActivity(
            IconDetailActivity.intent(this, identity, StylePreferenceStore(this).selectedStyle()),
        )
    }

    private fun updateEmptyState() {
        val anyVisible = listOf(looksSection, wallpapersSection, appsSection)
            .any { it.visibility == View.VISIBLE }
        empty.visibility = if (anyVisible) View.GONE else View.VISIBLE
    }

    private companion object {
        const val PREVIEW_ASPECT = 1.35f
        const val THUMBNAIL_ASPECT = 1.5f
        const val MIN_APP_SPAN = 3
        const val MAX_APP_SPAN = 10
    }
}
