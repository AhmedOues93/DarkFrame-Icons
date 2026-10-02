package com.darkframe.icons

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.darkframe.icons.billing.Entitlement
import com.darkframe.icons.billing.ProEntitlementStore
import com.darkframe.icons.data.FavoritesStore
import com.darkframe.icons.engine.DarkFrameEngine
import com.darkframe.icons.engine.apply.ApplyCapability
import com.darkframe.icons.engine.data.LookPreferenceStore
import com.darkframe.icons.engine.domain.CompleteLook
import com.darkframe.icons.engine.domain.LookCatalog
import com.darkframe.icons.engine.wallpaper.WallpaperCatalog
import com.darkframe.icons.ui.FavoritesActivity
import com.darkframe.icons.ui.OnboardingActivity
import com.darkframe.icons.ui.ProActivity
import com.darkframe.icons.ui.SettingsActivity
import com.darkframe.icons.ui.WidgetsActivity
import com.darkframe.icons.ui.browser.IconBrowserActivity
import com.darkframe.icons.ui.common.DarkFrameActivity
import com.darkframe.icons.ui.common.contentColumnWidthPx
import com.darkframe.icons.ui.collections.CollectionsActivity
import com.darkframe.icons.ui.common.SpacingDecoration
import com.darkframe.icons.ui.common.applySystemBarPadding
import com.darkframe.icons.ui.common.constrainContentWidth
import com.darkframe.icons.ui.common.spanFromWidth
import com.darkframe.icons.ui.home.CollectionCardAdapter
import com.darkframe.icons.ui.home.HomeTile
import com.darkframe.icons.ui.home.HomeTileAdapter
import com.darkframe.icons.ui.home.EngineLookPreviewLoader
import com.darkframe.icons.ui.look.LookDetailActivity
import com.darkframe.icons.ui.setup.ApplyActivity
import com.darkframe.icons.wallpaper.WallpaperActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The home screen, and the whole product in one view: what your phone will look like, and the one
 * button that starts applying it.
 *
 * Everything else is secondary and sits below. There is deliberately no maintenance action here —
 * rebuilding the icon cache is a Settings concern, not something a user should meet on first launch.
 */
class MainActivity : DarkFrameActivity() {

    private val engine by lazy { DarkFrameEngine.get(applicationContext) }
    private val previews by lazy { EngineLookPreviewLoader(this) }
    private val lookStore by lazy { LookPreferenceStore(this) }
    private val favorites by lazy { FavoritesStore(this) }

    private lateinit var heroImage: ImageView
    private lateinit var heroName: TextView
    private lateinit var heroTagline: TextView
    private lateinit var applyButton: TextView
    private lateinit var applyHint: TextView
    private lateinit var collections: RecyclerView
    private lateinit var tiles: RecyclerView

    private var collectionAdapter: CollectionCardAdapter? = null
    private var tileAdapter: HomeTileAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // The splash theme only covers the cold-start window; from here the normal theme applies,
        // otherwise the launcher icon stays painted behind every screen.
        setTheme(R.style.Theme_DarkFrame)
        super.onCreate(savedInstanceState)
        if (!hasOnboarded()) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }
        setContentView(R.layout.activity_home)

        findViewById<NestedScrollView>(R.id.home_scroll).apply {
            applySystemBarPadding()
            constrainContentWidth()
        }
        heroImage = findViewById(R.id.home_hero_image)
        heroName = findViewById(R.id.home_hero_name)
        heroTagline = findViewById(R.id.home_hero_tagline)
        applyButton = findViewById(R.id.home_apply)
        applyHint = findViewById(R.id.home_apply_hint)
        collections = findViewById(R.id.home_collections)
        tiles = findViewById(R.id.home_tiles)

        findViewById<View>(R.id.home_collections_all).setOnClickListener {
            startActivity(CollectionsActivity.intent(this))
        }
        findViewById<View>(R.id.home_settings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        findViewById<View>(R.id.home_hero).setOnClickListener {
            openLook(lookStore.selected())
        }
        applyButton.setOnClickListener {
            startActivity(Intent(this, ApplyActivity::class.java))
        }

        setUpCollections()
        setUpTiles()
    }

    /**
     * A fold does not recreate this Activity — `configChanges` covers it — so nothing would otherwise
     * ask the hero for a preview at the new width, and `centerCrop` would stretch the old one.
     *
     * Re-binding costs one render per width, after which the preview cache answers both, so folding
     * back and forth is free. That is the whole reason the cache is keyed by size.
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (!hasOnboarded()) return
        bindHero(lookStore.selected())
    }

    override fun onResume() {
        super.onResume()
        if (!hasOnboarded()) return
        bindHero(lookStore.selected())
        bindApplyAction()
        tileAdapter?.submit(buildTiles())
        collectionAdapter?.notifyItemRangeChanged(0, LookCatalog.all.size)
    }

    private fun hasOnboarded() =
        getSharedPreferences("darkframe", MODE_PRIVATE).getBoolean("onboarded", false)

    // ---- hero ---------------------------------------------------------------------------------

    private fun bindHero(look: CompleteLook) {
        heroName.text = look.name
        heroTagline.text = look.tagline
        // Derived from the window rather than from the view: a measured width is 0 on the first
        // pass and real on the second, which would key two separate renders of the same preview.
        val width = resources.contentColumnWidthPx()
        val height = resources.getDimensionPixelSize(R.dimen.df_hero_height)
        val warm = previews.peek(look, width, height)
        if (warm != null) {
            heroImage.setImageBitmap(warm)
            return
        }
        lifecycleScope.launch {
            val bitmap = previews.load(look, width, height)
            heroImage.setImageBitmap(bitmap)
        }
    }

    // ---- the one action -----------------------------------------------------------------------

    /**
     * The primary button names the mechanism that exists on *this* device.
     *
     * It never says "Apply all": on every launcher the final step belongs to someone else, so a
     * button claiming to have done it would be the one thing this product must not do.
     */
    private fun bindApplyAction() {
        lifecycleScope.launch {
            val profile = withContext(Dispatchers.IO) { engine.apply.currentLauncher() }
            val capability = withContext(Dispatchers.IO) { engine.apply.effectiveCapability(profile) }
            applyButton.text = when (capability) {
                ApplyCapability.SAMSUNG_THEME_PARK -> getString(R.string.home_apply_samsung)
                ApplyCapability.ICON_PACK_NATIVE -> getString(R.string.home_apply_icon_pack)
                else -> getString(R.string.home_apply_generic)
            }
            applyHint.text = when (capability) {
                ApplyCapability.SAMSUNG_THEME_PARK -> getString(R.string.home_apply_hint_samsung)
                ApplyCapability.ICON_PACK_NATIVE ->
                    getString(R.string.home_apply_hint_icon_pack, profile.displayName)
                else -> getString(R.string.home_apply_hint_generic, profile.displayName)
            }
        }
    }

    // ---- collections --------------------------------------------------------------------------

    private fun setUpCollections() {
        val width = resources.getDimensionPixelSize(R.dimen.df_collection_card_width) -
            resources.getDimensionPixelSize(R.dimen.df_space_2) * 2
        val height = (width * PREVIEW_ASPECT).toInt()
        val adapter = CollectionCardAdapter(
            loader = previews,
            scope = lifecycleScope,
            previewWidthPx = width,
            previewHeightPx = height,
            onClick = ::openLook,
        )
        collections.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        collections.adapter = adapter
        adapter.submitList(LookCatalog.all)
        collectionAdapter = adapter
    }

    private fun openLook(look: CompleteLook) {
        startActivity(LookDetailActivity.intent(this, look))
    }

    // ---- tiles --------------------------------------------------------------------------------

    private fun setUpTiles() {
        val adapter = HomeTileAdapter(buildTiles())
        tiles.layoutManager = GridLayoutManager(this, 2)
        tiles.adapter = adapter
        tiles.spanFromWidth(
            targetCellPx = resources.getDimensionPixelSize(R.dimen.df_collection_card_width),
            minSpan = 1,
            maxSpan = 4,
        )
        tiles.addItemDecoration(SpacingDecoration(resources.getDimensionPixelSize(R.dimen.df_space_2)))
        tileAdapter = adapter
    }

    private fun buildTiles(): List<HomeTile> = listOf(
        HomeTile(
            id = "wallpapers",
            title = getString(R.string.home_tile_wallpapers),
            subtitle = resources.getQuantityString(
                R.plurals.home_tile_wallpapers_sub,
                WallpaperCatalog.all.size,
                WallpaperCatalog.all.size,
                WallpaperCatalog.categoriesWithContent().size,
            ),
            onClick = { startActivity(Intent(this, WallpaperActivity::class.java)) },
        ),
        HomeTile(
            id = "widgets",
            title = getString(R.string.home_tile_widgets),
            subtitle = getString(R.string.home_tile_widgets_sub),
            onClick = { startActivity(Intent(this, WidgetsActivity::class.java)) },
        ),
        HomeTile(
            id = "apps",
            title = getString(R.string.home_tile_apps),
            subtitle = getString(R.string.home_tile_apps_sub),
            onClick = { startActivity(Intent(this, IconBrowserActivity::class.java)) },
        ),
        HomeTile(
            id = "favorites",
            title = getString(R.string.home_tile_favorites),
            subtitle = resources.getQuantityString(
                R.plurals.home_tile_favorites_sub,
                favorites.totalCount(),
                favorites.totalCount(),
            ),
            onClick = { startActivity(Intent(this, FavoritesActivity::class.java)) },
        ),
        proTile(),
    )

    /**
     * Pro, as a tile rather than a button of its own.
     *
     * It used to be a full-width secondary button under the grid, which gave a shop link its own
     * dedicated control on the home screen. As a tile it sits with the other four places to go, and
     * the only thing that marks it out is the one gold accent in the app.
     */
    private fun proTile(): HomeTile {
        val isPro = ProEntitlementStore(this).current() == Entitlement.PRO
        return HomeTile(
            id = "pro",
            title = getString(R.string.pro_title),
            subtitle = if (isPro) getString(R.string.settings_pro_sub_active)
            else getString(R.string.home_unlock_pro),
            accent = !isPro,
            onClick = { startActivity(Intent(this, ProActivity::class.java)) },
        )
    }

    private companion object {
        /** Preview cards are taller than wide, like the phone they are showing. */
        const val PREVIEW_ASPECT = 1.35f
    }
}
