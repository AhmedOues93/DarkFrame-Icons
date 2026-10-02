package com.darkframe.icons.widget

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

/**
 * Current charge, read once.
 *
 * Shared by the battery and info widgets so there is one answer to "how do we get the level without
 * polling". `ACTION_BATTERY_CHANGED` is a sticky broadcast, so registering a null receiver for it
 * returns the last value immediately and unregisters nothing — there is no receiver left behind and
 * no listener running between updates.
 */
object BatteryLevel {

    /** Percentage in 0..100, or null when the platform did not answer. */
    fun read(context: Context): Int? {
        val status = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?: return null
        val level = status.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = status.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level < 0 || scale <= 0) return null
        return (level * 100) / scale
    }

    /**
     * The power events the system delivers to a manifest receiver.
     *
     * Battery level has no widget-friendly push, and polling it is exactly the continuous background
     * work DarkFrame does not do. These four plus the host's own update requests are what a charge
     * figure is allowed to cost: between them the number can lag, which is a fair trade for a widget
     * that costs nothing when nothing is happening.
     */
    val POWER_EVENTS = setOf(
        Intent.ACTION_POWER_CONNECTED,
        Intent.ACTION_POWER_DISCONNECTED,
        Intent.ACTION_BATTERY_LOW,
        Intent.ACTION_BATTERY_OKAY,
    )
}
