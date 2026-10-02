package com.darkframe.icons.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.darkframe.icons.R

/**
 * Time, date and charge on one line — the whole status of the phone in the smallest tile.
 *
 * The time and date keep themselves current in the launcher's process; only the charge figure comes
 * from here, on the same four real power events the battery widget uses. Nothing polls.
 */
class DarkFrameInfoWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        render(context, manager, ids)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action in BatteryLevel.POWER_EVENTS) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, DarkFrameInfoWidget::class.java))
            if (ids.isNotEmpty()) render(context, manager, ids)
        }
    }

    private fun render(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val style = WidgetTheme.currentStyle(context)
        val level = BatteryLevel.read(context)
        ids.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.widget_info)
            WidgetTheme.apply(
                views = views,
                style = style,
                rootId = R.id.widget_root,
                primaryId = R.id.widget_primary,
                secondaryId = R.id.widget_secondary,
                tertiaryId = R.id.widget_tertiary,
            )
            views.setTextViewText(
                R.id.widget_secondary,
                if (level != null) {
                    context.getString(R.string.widget_battery_percent, level)
                } else {
                    context.getString(R.string.widget_battery_placeholder)
                },
            )
            manager.updateAppWidget(id, views)
        }
    }
}
