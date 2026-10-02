package com.darkframe.icons.ui.common

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat

/**
 * Base for every DarkFrame screen.
 *
 * Does one thing: turns off the system's own window insets fitting so the app can draw edge to edge,
 * which is a per-window setting and therefore easy to forget on a new screen. Screens then inset
 * their own content with [applySystemBarPadding].
 */
abstract class DarkFrameActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
    }
}
