package com.darkframe.icons.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.*
import android.os.BatteryManager
import android.widget.RemoteViews
import com.darkframe.icons.R

class DarkFrameBatteryWidget:AppWidgetProvider(){
 override fun onUpdate(context:Context,manager:AppWidgetManager,ids:IntArray){
  val b=context.registerReceiver(null,IntentFilter(Intent.ACTION_BATTERY_CHANGED))
  val level=b?.getIntExtra(BatteryManager.EXTRA_LEVEL,0)?:0
  ids.forEach{val v=RemoteViews(context.packageName,R.layout.widget_battery);v.setTextViewText(R.id.battery_value,"$level%");manager.updateAppWidget(it,v)}
 }
}
