package com.darkframe.icons.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.darkframe.icons.R

/**
 * Month, date and weekday, and a tap that opens the user's calendar.
 *
 * Deliberately not a month grid. Drawing one in `RemoteViews` needs a `GridView` backed by a
 * `RemoteViewsService`, and a grid of thirty numbers at widget size is unreadable anyway — so the
 * widget shows the thing someone actually glances at and hands off to the real calendar for the rest.
 * That hand-off is honest about where the capability lives, which is the same principle the icon
 * apply flow follows.
 *
 * Three `TextClock` views, so it rolls over at midnight inside the launcher's process. No alarm, no
 * periodic update, no work when nothing is happening.
 */
class DarkFrameCalendarWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val style = WidgetTheme.currentStyle(context)
        val launch = calendarIntent(context)
        ids.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.widget_calendar)
            WidgetTheme.apply(
                views = views,
                style = style,
                rootId = R.id.widget_root,
                primaryId = R.id.widget_primary,
                secondaryId = R.id.widget_secondary,
                tertiaryId = R.id.widget_tertiary,
            )
            // Null when the device has no calendar app the launcher can resolve, which is rare but
            // real on a stripped ROM. A tap then does nothing rather than crashing the host.
            launch?.let { views.setOnClickPendingIntent(R.id.widget_root, it) }
            manager.updateAppWidget(id, views)
        }
    }

    /**
     * The device's calendar, by category rather than by package name.
     *
     * `CATEGORY_APP_CALENDAR` is the platform's own way of asking for "whatever calendar this device
     * uses", so it works on a Samsung device with Samsung Calendar and on a Pixel with Google
     * Calendar, with no package list to keep current and no reason to widen package visibility.
     */
    private fun calendarIntent(context: Context): PendingIntent? {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALENDAR)
        if (context.packageManager.resolveActivity(intent, 0) == null) return null
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
