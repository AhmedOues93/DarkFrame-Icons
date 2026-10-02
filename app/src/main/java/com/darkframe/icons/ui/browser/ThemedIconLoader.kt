package com.darkframe.icons.ui.browser

import android.graphics.Bitmap
import com.darkframe.icons.engine.domain.AppIdentity

/**
 * The only icon-related capability the view layer is given.
 *
 * Narrow on purpose: an adapter can ask for a finished bitmap and nothing else, so there is no way
 * for drawing decisions to leak back into views. It also makes the adapter testable against a fake.
 */
interface ThemedIconLoader {
    /** Synchronous, main-thread-safe hit against the in-memory cache. Null means "not yet". */
    fun peek(identity: AppIdentity, sizePx: Int): Bitmap?

    /** Suspends while the icon is rendered on a background dispatcher. */
    suspend fun load(identity: AppIdentity, sizePx: Int): Bitmap

    fun isCurated(identity: AppIdentity): Boolean
}
