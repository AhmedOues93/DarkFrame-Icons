package com.darkframe.icons.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.darkframe.icons.R

/** Lightweight analog clock. AnalogClock is advanced by the widget host; DarkFrame does no polling. */
class DarkFrameAnalogClockWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val style = WidgetTheme.currentStyle(context)
        ids.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.widget_analog_clock)
            WidgetTheme.apply(views, style, R.id.widget_root, R.id.widget_label)
            manager.updateAppWidget(id, views)
        }
    }
}
