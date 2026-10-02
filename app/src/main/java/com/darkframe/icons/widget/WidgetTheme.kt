package com.darkframe.icons.widget

import android.content.Context
import android.widget.RemoteViews
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import com.darkframe.icons.R
import com.darkframe.icons.engine.data.LookPreferenceStore
import com.darkframe.icons.engine.domain.ColorMatrices
import com.darkframe.icons.engine.domain.IconStyle

/**
 * Makes a widget look like it belongs to the collection the user picked.
 *
 * Widgets are drawn by the launcher in another process, so they cannot share the engine's renderer.
 * What they can share is the palette: one background shape per collection, carrying the same
 * container colour, keyline and corner radius as that collection's icons, plus the same ink colour
 * for the text. The result is a home screen where the clock and the icons are obviously the same
 * product rather than two things that happen to both be dark.
 */
object WidgetTheme {

    /** Background shape for a collection. Named per style id so the two cannot drift apart. */
    @DrawableRes
    fun backgroundFor(style: IconStyle): Int = when (style.id) {
        "noir" -> R.drawable.widget_bg_noir
        "color_pop" -> R.drawable.widget_bg_color_pop
        "frost" -> R.drawable.widget_bg_frost
        "titanium" -> R.drawable.widget_bg_titanium
        "glass" -> R.drawable.widget_bg_glass
        "pure_amoled" -> R.drawable.widget_bg_pure_amoled
        else -> R.drawable.widget_bg_noir
    }

    /** Primary text colour: the collection's own ink. */
    fun inkColor(style: IconStyle): Int = style.glyphTint.toInt()

    /**
     * Secondary text colour.
     *
     * Derived from the ink rather than being a fixed grey, so it stays readable on Frost's light
     * surface as well as on the dark collections.
     */
    fun secondaryInkColor(style: IconStyle): Int =
        ((style.glyphTint and 0x00FFFFFFL) or (0xB0L shl 24)).toInt()

    /** The user's current collection, read fresh so a widget follows a look change. */
    fun currentStyle(context: Context): IconStyle = LookPreferenceStore(context).selected().style

    /**
     * Whether this collection's surface is light, and therefore wants dark ink drawn on it.
     *
     * Text colour is set from the provider, so most widgets need no such branch. The analog clock
     * does: `AnalogClock` exposes no RemoteViews-settable dial or hands, so its ink is baked into a
     * layout and the provider has to pick between two.
     */
    fun hasLightSurface(style: IconStyle): Boolean =
        ColorMatrices.relativeLuminance(style.containerColor) > LIGHT_SURFACE_LUMINANCE

    /** Applies the collection to a widget with three text roles. */
    fun apply(
        views: RemoteViews,
        style: IconStyle,
        @IdRes rootId: Int,
        @IdRes primaryId: Int,
        @IdRes secondaryId: Int,
        @IdRes tertiaryId: Int,
    ) {
        apply(views, style, rootId, primaryId, secondaryId)
        views.setTextColor(tertiaryId, secondaryInkColor(style))
    }

    /** Frost is the one light collection; the threshold sits well clear of every dark one. */
    private const val LIGHT_SURFACE_LUMINANCE = 0.4f

    /** Applies the collection to a widget's root and its two text roles. */
    fun apply(
        views: RemoteViews,
        style: IconStyle,
        @IdRes rootId: Int,
        @IdRes primaryId: Int,
        @IdRes secondaryId: Int? = null,
    ) {
        views.setInt(rootId, "setBackgroundResource", backgroundFor(style))
        views.setTextColor(primaryId, inkColor(style))
        secondaryId?.let { views.setTextColor(it, secondaryInkColor(style)) }
    }
}
