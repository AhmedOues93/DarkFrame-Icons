package com.darkframe.icons.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.darkframe.icons.R

/** Day and date, in the user's collection. Rolls over by itself; no alarm, no service. */
class DarkFrameDateWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val style = WidgetTheme.currentStyle(context)
        ids.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.widget_date)
            WidgetTheme.apply(views, style, R.id.widget_root, R.id.widget_primary, R.id.widget_secondary)
            manager.updateAppWidget(id, views)
        }
    }
}
