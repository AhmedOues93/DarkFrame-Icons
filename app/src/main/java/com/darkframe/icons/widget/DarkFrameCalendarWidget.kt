package com.darkframe.icons.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.darkframe.icons.R

/** Calendar glance widget. TextClock rolls the date over in the host process; no alarms or workers. */
class DarkFrameCalendarWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val style = WidgetTheme.currentStyle(context)
        ids.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.widget_calendar)
            WidgetTheme.apply(views, style, R.id.widget_root, R.id.widget_primary, R.id.widget_secondary)
            manager.updateAppWidget(id, views)
        }
    }
}
