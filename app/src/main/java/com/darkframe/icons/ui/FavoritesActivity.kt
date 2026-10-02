package com.darkframe.icons.ui

import android.content.Intent
import android.os.Bundle
import com.darkframe.icons.R
import com.darkframe.icons.data.FavoriteKind
import com.darkframe.icons.data.FavoritesStore
import com.darkframe.icons.engine.domain.LookCatalog
import com.darkframe.icons.engine.wallpaper.WallpaperCatalog
import com.darkframe.icons.ui.browser.IconBrowserActivity
import com.darkframe.icons.ui.common.DarkFrameActivity
import com.darkframe.icons.ui.common.SectionScreen
import com.darkframe.icons.ui.look.LookDetailActivity
import com.darkframe.icons.wallpaper.WallpaperActivity

/**
 * Everything the user has saved, in one place.
 *
 * Looks, wallpapers and app icons share a store, so this is one useful screen rather than three
 * partial ones. Sections with nothing in them are omitted entirely.
 */
class FavoritesActivity : DarkFrameActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        render()
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        val store = FavoritesStore(this)
        val looks = LookCatalog.all.filter { store.isFavorite(FavoriteKind.LOOK, it.id) }
        val wallpapers = WallpaperCatalog.all.filter { store.isFavorite(FavoriteKind.WALLPAPER, it.id) }
        val apps = store.idsOf(FavoriteKind.APP).sorted()

        val screen = SectionScreen(this).setUp(getString(R.string.favorites_title))

        if (looks.isEmpty() && wallpapers.isEmpty() && apps.isEmpty()) {
            screen.caption(getString(R.string.favorites_empty))
            return
        }

        if (looks.isNotEmpty()) {
            screen.header(getString(R.string.favorites_section_looks))
            looks.forEach { look ->
                screen.row(look.name, look.tagline) {
                    startActivity(LookDetailActivity.intent(this, look))
                }
            }
        }
        if (wallpapers.isNotEmpty()) {
            screen.header(getString(R.string.favorites_section_wallpapers))
            wallpapers.forEach { spec ->
                screen.row(spec.title, spec.category.name.lowercase().replaceFirstChar { it.uppercase() }) {
                    startActivity(Intent(this, WallpaperActivity::class.java))
                }
            }
        }
        if (apps.isNotEmpty()) {
            screen.header(getString(R.string.favorites_section_apps))
            apps.forEach { componentKey ->
                screen.row(componentKey.substringBefore('/'), componentKey.substringAfter('/')) {
                    startActivity(Intent(this, IconBrowserActivity::class.java))
                }
            }
        }
    }
}
