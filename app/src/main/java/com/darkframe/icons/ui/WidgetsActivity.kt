package com.darkframe.icons.ui

import android.os.Bundle
import com.darkframe.icons.R
import com.darkframe.icons.engine.data.LookPreferenceStore
import com.darkframe.icons.ui.common.DarkFrameActivity
import com.darkframe.icons.ui.common.SectionScreen

/**
 * What DarkFrame's widgets are, and how to add them.
 *
 * Android has no API for placing a widget on a home screen — only the user can do that, from the
 * launcher's own picker. So this screen tells them where to find it rather than offering a button
 * that cannot work.
 */
class WidgetsActivity : DarkFrameActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val look = LookPreferenceStore(this).selected()

        SectionScreen(this)
            .setUp(getString(R.string.widgets_title), look.name)
            .section(getString(R.string.widgets_title), getString(R.string.widgets_body))
            .row(getString(R.string.widget_clock), getString(R.string.widget_clock_sub))
            .row(getString(R.string.widget_analog), getString(R.string.widget_analog_sub))
            .row(getString(R.string.widget_date), getString(R.string.widget_date_sub))
            .row(getString(R.string.widget_calendar), getString(R.string.widget_calendar_sub))
            .row(getString(R.string.widget_battery), getString(R.string.widget_battery_sub))
            .row(getString(R.string.widget_minimal), getString(R.string.widget_minimal_sub))
            .caption(getString(R.string.widgets_resizable))
    }
}
