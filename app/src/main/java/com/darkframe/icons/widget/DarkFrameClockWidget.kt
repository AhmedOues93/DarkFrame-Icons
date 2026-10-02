package com.darkframe.icons.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.darkframe.icons.R

class DarkFrameClockWidget:AppWidgetProvider(){
    override fun onUpdate(context:Context,manager:AppWidgetManager,ids:IntArray){
        ids.forEach{manager.updateAppWidget(it,RemoteViews(context.packageName,R.layout.widget_clock))}
    }
}
