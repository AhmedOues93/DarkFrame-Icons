package com.darkframe.icons.ui.look

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
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
import com.darkframe.icons.model.WidgetKind
import com.darkframe.icons.ui.ProActivity
import com.darkframe.icons.ui.common.DarkFrameActivity
import com.darkframe.icons.ui.common.contentColumnWidthPx
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
        screen.custom(paletteView())

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
        // A recommendation, and labelled as one. Android gives no app a way to set another
        // launcher's grid or turn its labels off, so this is the layout the look was composed at and
        // a line of why — never a button that claims to arrange someone's home screen.
        screen.row(
            title = getString(R.string.look_part_layout),
            subtitle = getString(
                if (look.layout.labels) R.string.look_layout_with_labels
                else R.string.look_layout_without_labels,
                look.layout.grid,
            ),
            value = getString(R.string.look_layout_suggested),
        )
        screen.caption(look.layout.note)

        val saved = favorites.isFavorite(FavoriteKind.LOOK, look.id)
        screen.secondaryButton(
            if (saved) getString(R.string.wallpaper_unfavorite) else getString(R.string.wallpaper_favorite),
        ) {
            favorites.toggle(FavoriteKind.LOOK, look.id)
            render()
        }
    }

    /**
     * One label per widget, not one per family.
     *
     * This used to collapse the six kinds onto three names, from when only three widgets existed — so
     * a look recommending the analog clock said "Clock" and a user looking for it in the launcher's
     * picker had no way to know which of the two to add.
     */
    private fun widgetLabel(kind: WidgetKind): String = when (kind) {
        WidgetKind.DIGITAL_CLOCK -> getString(R.string.widget_clock)
        WidgetKind.ANALOG_CLOCK -> getString(R.string.widget_analog)
        WidgetKind.DATE -> getString(R.string.widget_date)
        WidgetKind.CALENDAR -> getString(R.string.widget_calendar)
        WidgetKind.BATTERY -> getString(R.string.widget_battery)
        WidgetKind.INFO -> getString(R.string.widget_info)
    }

    /**
     * The look's three colours, as three bars.
     *
     * Deliberately not labelled "surface / ink / accent": those are the words for building a look,
     * not for choosing one. The bars are in the proportion they appear on a home screen — most of it
     * is surface — so the row reads as the look's weight rather than as a legend.
     */
    private fun paletteView(): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                resources.getDimensionPixelSize(R.dimen.df_palette_height),
            )
            contentDescription = getString(R.string.look_palette_description, look.name)
        }
        PALETTE_WEIGHTS.forEachIndexed { index, weight ->
            val swatch = View(this)
            swatch.setBackgroundColor(look.palette.swatches[index].toInt())
            swatch.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, weight)
            row.addView(swatch)
        }
        // The background is what gives the row an outline for clipToOutline to clip children to;
        // without one the swatches would square off the corners.
        row.setBackgroundResource(R.drawable.df_preview_clip)
        row.clipToOutline = true
        return row
    }

    private fun previewView(): ImageView {
        val height = resources.getDimensionPixelSize(R.dimen.df_hero_height)
        val view = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundResource(R.drawable.df_preview_clip)
            clipToOutline = true
            contentDescription = getString(R.string.look_preview_description, look.name)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                height,
            )
        }
        val width = resources.contentColumnWidthPx()
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
            }
            Toast.makeText(this@LookDetailActivity, message, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val EXTRA_LOOK_ID = "look_id"

        /** Surface dominates a home screen; the accent is a sliver. The bars say the same thing. */
        private val PALETTE_WEIGHTS = floatArrayOf(6f, 2f, 1f)

        fun intent(context: Context, look: CompleteLook): Intent =
            Intent(context, LookDetailActivity::class.java).putExtra(EXTRA_LOOK_ID, look.id)
    }
}
