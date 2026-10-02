package com.darkframe.icons.engine.data

import android.content.Context
import com.darkframe.icons.engine.domain.CompleteLook
import com.darkframe.icons.engine.domain.LookCatalog

/**
 * The look the user has chosen.
 *
 * One stored id, because a look is the product's single unit of choice: picking Noir selects the
 * Noir collection, its wallpaper and its widgets together. The icon style follows from it, so
 * nothing else needs persisting.
 */
class LookPreferenceStore(context: Context) {

    private val prefs = context.getSharedPreferences("darkframe_look", Context.MODE_PRIVATE)

    /** Resolved through the catalog, which falls back rather than throwing on a stale id. */
    fun selected(): CompleteLook = LookCatalog.forId(prefs.getString(KEY_LOOK_ID, null))

    fun select(look: CompleteLook) {
        prefs.edit().putString(KEY_LOOK_ID, look.id).apply()
    }

    private companion object {
        const val KEY_LOOK_ID = "selected_look_id"
    }
}
