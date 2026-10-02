package com.darkframe.icons.widget
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.darkframe.icons.R
class DarkFrameDateWidget:AppWidgetProvider(){override fun onUpdate(c:Context,m:AppWidgetManager,ids:IntArray){ids.forEach{m.updateAppWidget(it,RemoteViews(c.packageName,R.layout.widget_date))}}}