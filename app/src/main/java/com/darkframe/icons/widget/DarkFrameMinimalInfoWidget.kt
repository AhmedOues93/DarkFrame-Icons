package com.darkframe.icons.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.darkframe.icons.R

/** Time + day at a glance, intentionally host-driven so it has zero periodic DarkFrame work. */
class DarkFrameMinimalInfoWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val style = WidgetTheme.currentStyle(context)
        ids.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.widget_minimal_info)
            WidgetTheme.apply(views, style, R.id.widget_root, R.id.widget_primary, R.id.widget_secondary)
            manager.updateAppWidget(id, views)
        }
    }
}
