package com.darkframe.icons.ui.home

import android.content.Context
import android.graphics.Bitmap
import com.darkframe.icons.engine.DarkFrameEngine
import com.darkframe.icons.engine.domain.CompleteLook
import kotlinx.coroutines.withContext

/**
 * The engine's look-preview cache, as a [LookPreviewLoader].
 *
 * Extracted from the home screen once a second screen wanted the same four lines: having each
 * Activity implement the interface itself is how two screens end up rendering previews on different
 * dispatchers, which on this project is the difference between a cool device and a warm one.
 *
 * Holds an application context, so it is safe to keep for the life of a screen.
 */
class EngineLookPreviewLoader(context: Context) : LookPreviewLoader {

    private val engine = DarkFrameEngine.get(context.applicationContext)

    override fun peek(look: CompleteLook, widthPx: Int, heightPx: Int): Bitmap? =
        engine.lookPreviews.peek(look, widthPx, heightPx)

    override suspend fun load(look: CompleteLook, widthPx: Int, heightPx: Int): Bitmap =
        withContext(engine.renderDispatcher) {
            engine.lookPreviews.get(look, widthPx, heightPx)
        }
}
