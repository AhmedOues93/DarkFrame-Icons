package com.darkframe.icons.wallpaper

import android.content.Context
import android.graphics.Bitmap
import android.util.LruCache
import com.darkframe.icons.engine.DarkFrameEngine
import com.darkframe.icons.engine.wallpaper.WallpaperSpec
import kotlinx.coroutines.withContext

/**
 * Wallpaper thumbnails from the engine, with a bounded cache.
 *
 * Extracted from the wallpaper grid once Favourites wanted the same thumbnails. Each instance keeps
 * its own cache, which is the right shape here: the cache's whole job is to stop one screen redrawing
 * what it has already drawn, and it should go away with the screen rather than hold several megabytes
 * of bitmaps for a grid nobody is looking at.
 */
class EngineWallpaperPreviewLoader(context: Context) : WallpaperPreviewLoader {

    private val engine = DarkFrameEngine.get(context.applicationContext)

    private val thumbnails = object : LruCache<String, Bitmap>(BUDGET_BYTES) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    override fun peek(spec: WallpaperSpec, widthPx: Int, heightPx: Int): Bitmap? =
        thumbnails.get(key(spec, widthPx, heightPx))

    override suspend fun load(spec: WallpaperSpec, widthPx: Int, heightPx: Int): Bitmap =
        withContext(engine.renderDispatcher) {
            val cacheKey = key(spec, widthPx, heightPx)
            thumbnails.get(cacheKey) ?: engine.wallpapers.preview(spec, widthPx, heightPx)
                .also { thumbnails.put(cacheKey, it) }
        }

    private fun key(spec: WallpaperSpec, w: Int, h: Int) = "${spec.id}:${w}x$h"

    private companion object {
        const val BUDGET_BYTES = 8 * 1024 * 1024
    }
}
