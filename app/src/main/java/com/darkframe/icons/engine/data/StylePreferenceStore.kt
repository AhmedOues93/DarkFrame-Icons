package com.darkframe.icons.engine.data

import android.content.Context
import com.darkframe.icons.engine.domain.IconStyle
import com.darkframe.icons.engine.domain.IconStyleCatalog

/** Remembers which collection the user is working in. */
class StylePreferenceStore(context: Context) {

    private val prefs = context.getSharedPreferences("darkframe_style", Context.MODE_PRIVATE)

    /**
     * Resolved through [IconStyleCatalog.forId], which falls back rather than throwing — a style
     * id persisted by an older build and since renamed must not crash the app on launch.
     */
    fun selectedStyle(): IconStyle = IconStyleCatalog.forId(prefs.getString(KEY_STYLE_ID, null))

    fun setSelectedStyle(style: IconStyle) {
        prefs.edit().putString(KEY_STYLE_ID, style.id).apply()
    }

    private companion object {
        const val KEY_STYLE_ID = "selected_style_id"
    }
}
