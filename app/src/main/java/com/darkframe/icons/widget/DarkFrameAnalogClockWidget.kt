package com.darkframe.icons.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.darkframe.icons.R

/**
 * An analog face in the user's collection.
 *
 * `AnalogClock` keeps itself ticking in the launcher's process, so like the digital clock this costs
 * DarkFrame nothing at all: no service, no alarm, no periodic update. What it cannot do is accept a
 * dial or hands through `RemoteViews`, so the ink is baked into the layout and the provider picks
 * between a pair of them. Everything else — background, secondary text colour — is themed at runtime
 * exactly as the other widgets are.
 */
class DarkFrameAnalogClockWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val style = WidgetTheme.currentStyle(context)
        val layout = if (WidgetTheme.hasLightSurface(style)) {
            R.layout.widget_analog_on_light
        } else {
            R.layout.widget_analog
        }
        ids.forEach { id ->
            val views = RemoteViews(context.packageName, layout)
            views.setInt(R.id.widget_root, "setBackgroundResource", WidgetTheme.backgroundFor(style))
            views.setTextColor(R.id.widget_secondary, WidgetTheme.secondaryInkColor(style))
            manager.updateAppWidget(id, views)
        }
    }
}
