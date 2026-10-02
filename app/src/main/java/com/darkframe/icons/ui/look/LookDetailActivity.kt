package com.darkframe.icons.ui.look

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.darkframe.icons.R
import com.darkframe.icons.billing.Entitlement
import com.darkframe.icons.billing.ProEntitlementStore
import com.darkframe.icons.data.FavoriteKind
import com.darkframe.icons.data.FavoritesStore
import com.darkframe.icons.engine.DarkFrameEngine
import com.darkframe.icons.engine.data.LookPreferenceStore
import com.darkframe.icons.engine.data.StylePreferenceStore
import com.darkframe.icons.engine.domain.CompleteLook
import com.darkframe.icons.engine.domain.LookCatalog
import com.darkframe.icons.engine.wallpaper.WallpaperCatalog
import com.darkframe.icons.engine.wallpaper.WallpaperOutcome
import com.darkframe.icons.engine.wallpaper.WallpaperTarget
import com.darkframe.icons.model.ContentTier
import com.darkframe.icons.ui.ProActivity
import com.darkframe.icons.ui.common.DarkFrameActivity
import com.darkframe.icons.ui.common.SectionScreen
import com.darkframe.icons.ui.setup.ApplyActivity
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * One Complete Look, shown before anything is applied.
 *
 * The preview at the top is a real render of the look's wallpaper with its collection's icons on it,
 * which is what lets a user decide. Below it, the look's three parts are listed with what actually
 * happens to each: the wallpaper DarkFrame sets itself, the icons go through the launcher, and the
 * widgets the user places — because Android has no API to place a widget.
 */
class LookDetailActivity : DarkFrameActivity() {

    private val engine by lazy { DarkFrameEngine.get(applicationContext) }
    private val lookStore by lazy { LookPreferenceStore(this) }
    private val styleStore by lazy { StylePreferenceStore(this) }
    private val favorites by lazy { FavoritesStore(this) }

    private lateinit var look: CompleteLook

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        look = LookCatalog.forId(intent.getStringExtra(EXTRA_LOOK_ID))
        render()
    }

    private fun render() {
        val locked = look.tier == ContentTier.PRO &&
            ProEntitlementStore(this).current() != Entitlement.PRO
        val wallpaper = WallpaperCatalog.byId(look.wallpaperId)
        val screen = SectionScreen(this).setUp(look.name, look.tagline)

        screen.custom(previewView())

        if (locked) {
            screen.primaryButton(getString(R.string.look_locked)) {
                startActivity(Intent(this, ProActivity::class.java))
            }
        } else {
            val selected = lookStore.selected().id == look.id
            screen.primaryButton(
                if (selected) getString(R.string.look_selected) else getString(R.string.look_use_look),
                enabled = !selected,
            ) {
                lookStore.select(look)
                styleStore.setSelectedStyle(look.style)
                startActivity(Intent(this, ApplyActivity::class.java))
                finish()
            }
            if (wallpaper != null) {
                screen.secondaryButton(getString(R.string.look_set_wallpaper)) {
                    applyWallpaper()
                }
            }
        }

        screen.header(getString(R.string.look_includes))
        screen.row(
            title = getString(R.string.look_part_icons),
            subtitle = getString(R.string.look_part_icons_sub, look.name),
        )
        screen.row(
            title = getString(R.string.look_part_wallpaper),
            subtitle = wallpaper?.title,
        )
        screen.row(
            title = getString(R.string.look_part_widgets),
            subtitle = look.widgets.joinToString(" · ") { widgetLabel(it) },
        )

        val saved = favorites.isFavorite(FavoriteKind.LOOK, look.id)
        screen.secondaryButton(
            if (saved) getString(R.string.wallpaper_unfavorite) else getString(R.string.wallpaper_favorite),
        ) {
            favorites.toggle(FavoriteKind.LOOK, look.id)
            render()
        }
    }

    private fun widgetLabel(kind: com.darkframe.icons.model.WidgetKind): String = when (kind) {
        com.darkframe.icons.model.WidgetKind.DIGITAL_CLOCK,
        com.darkframe.icons.model.WidgetKind.ANALOG_CLOCK,
        -> getString(R.string.widget_clock)

        com.darkframe.icons.model.WidgetKind.DATE,
        com.darkframe.icons.model.WidgetKind.CALENDAR,
        -> getString(R.string.widget_date)

        com.darkframe.icons.model.WidgetKind.BATTERY,
        com.darkframe.icons.model.WidgetKind.INFO,
        -> getString(R.string.widget_battery)
    }

    private fun previewView(): ImageView {
        val height = resources.getDimensionPixelSize(R.dimen.df_hero_height)
        val view = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundResource(R.drawable.df_preview_clip)
            clipToOutline = true
            contentDescription = getString(R.string.look_preview_description, look.name)
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                height,
            )
        }
        val width = resources.displayMetrics.widthPixels -
            resources.getDimensionPixelSize(R.dimen.df_screen_margin) * 2
        engine.lookPreviews.peek(look, width, height)?.let { view.setImageBitmap(it) }
            ?: lifecycleScope.launch {
                val bitmap = withContext(engine.renderDispatcher) {
                    engine.lookPreviews.get(look, width, height)
                }
                view.setImageBitmap(bitmap)
            }
        return view
    }

    private fun applyWallpaper() {
        val spec = WallpaperCatalog.byId(look.wallpaperId) ?: return
        lifecycleScope.launch {
            val outcome = withContext(engine.renderDispatcher) {
                engine.wallpapers.apply(spec, WallpaperTarget.BOTH)
            }
            val message = when (outcome) {
                is WallpaperOutcome.Applied -> getString(R.string.look_applied_wallpaper)
                is WallpaperOutcome.Failed -> getString(R.string.wallpaper_failed, outcome.reason)
                WallpaperOutcome.LockNotSupported -> getString(R.string.wallpaper_lock_unsupported)
            }
            Toast.makeText(this@LookDetailActivity, message, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val EXTRA_LOOK_ID = "look_id"

        fun intent(context: Context, look: CompleteLook): Intent =
            Intent(context, LookDetailActivity::class.java).putExtra(EXTRA_LOOK_ID, look.id)
    }
}
