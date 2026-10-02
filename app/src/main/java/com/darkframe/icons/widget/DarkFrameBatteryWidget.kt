package com.darkframe.icons.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.widget.RemoteViews
import com.darkframe.icons.R

/**
 * Charge level, in the user's collection.
 *
 * Battery level has no widget-friendly push, and polling it is exactly the kind of continuous
 * background work DarkFrame does not do. So this redraws on the four power events the system does
 * deliver to a manifest receiver — plugged in, unplugged, low, recovered — plus whenever the widget
 * host asks. Between those the number can lag, which is a fair trade for a widget that costs nothing
 * when nothing is happening.
 */
class DarkFrameBatteryWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        render(context, manager, ids)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action in POWER_EVENTS) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, DarkFrameBatteryWidget::class.java))
            if (ids.isNotEmpty()) render(context, manager, ids)
        }
    }

    private fun render(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val style = WidgetTheme.currentStyle(context)
        val level = currentLevel(context)
        ids.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.widget_battery)
            WidgetTheme.apply(views, style, R.id.widget_root, R.id.widget_primary, R.id.widget_secondary)
            views.setTextViewText(
                R.id.widget_primary,
                if (level != null) {
                    context.getString(R.string.widget_battery_percent, level)
                } else {
                    context.getString(R.string.widget_battery_placeholder)
                },
            )
            manager.updateAppWidget(id, views)
        }
    }

    /** A sticky broadcast read once, not a registered receiver: this returns immediately. */
    private fun currentLevel(context: Context): Int? {
        val status = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?: return null
        val level = status.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = status.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level < 0 || scale <= 0) return null
        return (level * 100) / scale
    }

    private companion object {
        val POWER_EVENTS = setOf(
            Intent.ACTION_POWER_CONNECTED,
            Intent.ACTION_POWER_DISCONNECTED,
            Intent.ACTION_BATTERY_LOW,
            Intent.ACTION_BATTERY_OKAY,
        )
    }
}
