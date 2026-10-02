package com.darkframe.icons.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.darkframe.icons.R
import com.darkframe.icons.engine.data.LookPreferenceStore
import com.darkframe.icons.engine.domain.IconStyle
import com.darkframe.icons.ui.common.DarkFrameActivity
import com.darkframe.icons.ui.common.SectionScreen
import com.darkframe.icons.widget.WidgetTheme

/**
 * The six DarkFrame widgets, shown rather than listed.
 *
 * The previous version was six names and six one-line descriptions, which told the user nothing
 * about what they would get. Each row now inflates the widget's own layout and themes it exactly as
 * the provider would, so the card on this screen is the widget — and switching collections visibly
 * changes all six.
 *
 * Android has no API for placing a widget on a home screen; only the user can, from the launcher's
 * own picker. So the screen says where to find them instead of offering a button that cannot work.
 */
class WidgetsActivity : DarkFrameActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val look = LookPreferenceStore(this).selected()
        val style = look.style

        val screen = SectionScreen(this).setUp(getString(R.string.widgets_title), look.name)
        WIDGETS.forEach { widget ->
            screen.custom(previewCard(widget, style))
        }
        screen.section(getString(R.string.widgets_add_heading), getString(R.string.widgets_body))
        screen.caption(getString(R.string.widgets_resizable))
    }

    /**
     * One widget's real layout, inside a labelled card.
     *
     * Inflated directly rather than through `RemoteViews`: this process can just inflate the layout,
     * and the themed result is identical because [WidgetTheme] decides the colours from the same
     * style either way. The only thing a preview cannot show is the live clock ticking, and
     * `TextClock` handles that for free in this process too.
     */
    private fun previewCard(widget: WidgetPreview, style: IconStyle): View {
        val card = LayoutInflater.from(this).inflate(R.layout.item_widget_preview, null, false)
        card.findViewById<TextView>(R.id.widget_preview_name).setText(widget.nameRes)
        card.findViewById<TextView>(R.id.widget_preview_detail).setText(widget.detailRes)

        val frame = card.findViewById<ViewGroup>(R.id.widget_preview_frame)
        val layout = if (widget.lightSurfaceLayoutRes != 0 && WidgetTheme.hasLightSurface(style)) {
            widget.lightSurfaceLayoutRes
        } else {
            widget.layoutRes
        }
        val preview = LayoutInflater.from(this).inflate(layout, frame, false)
        preview.layoutParams = LinearLayout.LayoutParams(
            resources.getDimensionPixelSize(widget.widthRes),
            resources.getDimensionPixelSize(widget.heightRes),
        )
        preview.contentDescription =
            getString(R.string.widgets_preview_description, getString(widget.nameRes))
        themePreview(preview, style)
        frame.addView(preview)
        return card
    }

    /**
     * The same decisions the providers make, applied to inflated views instead of RemoteViews.
     *
     * Each role is resolved as a plain `View` and narrowed with `as?`, because the analog clock's
     * primary role is an `AnalogClock` rather than a `TextView` — asking `findViewById` for a
     * `TextView` there would be an unchecked cast that fails at runtime. A role that is absent, or
     * is not text, is simply skipped: its ink is baked into its own drawable.
     */
    private fun themePreview(preview: View, style: IconStyle) {
        preview.setBackgroundResource(WidgetTheme.backgroundFor(style))
        textRole(preview, R.id.widget_primary)?.setTextColor(WidgetTheme.inkColor(style))
        textRole(preview, R.id.widget_secondary)?.setTextColor(WidgetTheme.secondaryInkColor(style))
        textRole(preview, R.id.widget_tertiary)?.setTextColor(WidgetTheme.secondaryInkColor(style))
    }

    private fun textRole(preview: View, id: Int): TextView? =
        preview.findViewById<View>(id) as? TextView

    /**
     * What a widget looks like on this screen.
     *
     * The preview sizes are the widget's own `targetCellWidth`/`targetCellHeight` translated into
     * dimensions, so a widget that is four cells wide and one tall looks four-by-one here too.
     */
    private data class WidgetPreview(
        val nameRes: Int,
        val detailRes: Int,
        val layoutRes: Int,
        val widthRes: Int,
        val heightRes: Int,
        /** A second layout for light collections, where the ink is baked in. 0 when not needed. */
        val lightSurfaceLayoutRes: Int = 0,
    )

    private companion object {
        val WIDGETS = listOf(
            WidgetPreview(
                nameRes = R.string.widget_clock,
                detailRes = R.string.widget_clock_sub,
                layoutRes = R.layout.widget_clock,
                widthRes = R.dimen.df_widget_preview_wide,
                heightRes = R.dimen.df_widget_preview_square,
            ),
            WidgetPreview(
                nameRes = R.string.widget_analog,
                detailRes = R.string.widget_analog_sub,
                layoutRes = R.layout.widget_analog,
                lightSurfaceLayoutRes = R.layout.widget_analog_on_light,
                widthRes = R.dimen.df_widget_preview_square,
                heightRes = R.dimen.df_widget_preview_square,
            ),
            WidgetPreview(
                nameRes = R.string.widget_date,
                detailRes = R.string.widget_date_sub,
                layoutRes = R.layout.widget_date,
                widthRes = R.dimen.df_widget_preview_square,
                heightRes = R.dimen.df_widget_preview_square,
            ),
            WidgetPreview(
                nameRes = R.string.widget_calendar,
                detailRes = R.string.widget_calendar_sub,
                layoutRes = R.layout.widget_calendar,
                widthRes = R.dimen.df_widget_preview_square,
                heightRes = R.dimen.df_widget_preview_square,
            ),
            WidgetPreview(
                nameRes = R.string.widget_battery,
                detailRes = R.string.widget_battery_sub,
                layoutRes = R.layout.widget_battery,
                widthRes = R.dimen.df_widget_preview_square,
                heightRes = R.dimen.df_widget_preview_square,
            ),
            WidgetPreview(
                nameRes = R.string.widget_info,
                detailRes = R.string.widget_info_sub,
                layoutRes = R.layout.widget_info,
                widthRes = R.dimen.df_widget_preview_wide,
                heightRes = R.dimen.df_widget_preview_short,
            ),
        )
    }
}
