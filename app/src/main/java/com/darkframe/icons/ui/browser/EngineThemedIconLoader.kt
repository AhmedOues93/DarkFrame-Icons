package com.darkframe.icons.ui.browser

import android.content.Context
import android.graphics.Bitmap
import com.darkframe.icons.engine.DarkFrameEngine
import com.darkframe.icons.engine.data.StylePreferenceStore
import com.darkframe.icons.engine.domain.AppIdentity
import com.darkframe.icons.engine.domain.IconRenderPolicy
import kotlinx.coroutines.withContext

/**
 * Themed icons in the user's currently selected collection.
 *
 * The browser has its own view model for this because it also owns the collection *choice*; a screen
 * that only wants to draw icons in whatever is selected — Favourites, for one — needs none of that
 * machinery, and giving it the view model would have it start a package watcher it has no use for.
 *
 * Sizes go through [IconRenderPolicy] exactly as everywhere else, so these icons share cache entries
 * with the browser's rather than forming a second set at slightly different dimensions.
 */
class EngineThemedIconLoader(context: Context) : ThemedIconLoader {

    private val appContext = context.applicationContext
    private val engine = DarkFrameEngine.get(appContext)

    /** Read per call, not cached, so a look change on another screen is picked up on return. */
    private fun style() = StylePreferenceStore(appContext).selectedStyle()

    override fun peek(identity: AppIdentity, sizePx: Int): Bitmap? =
        engine.resolver.peek(identity, style(), IconRenderPolicy.previewSizePx(sizePx))

    override suspend fun load(identity: AppIdentity, sizePx: Int): Bitmap {
        val style = style()
        val previewPx = IconRenderPolicy.previewSizePx(sizePx)
        return withContext(engine.renderDispatcher) {
            engine.resolver.resolve(identity, style, previewPx)
        }
    }

    override fun isCurated(identity: AppIdentity): Boolean = engine.resolver.isCurated(identity)
}
