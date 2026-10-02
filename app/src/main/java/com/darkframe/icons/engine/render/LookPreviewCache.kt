package com.darkframe.icons.engine.render

import android.content.Context
import android.graphics.Bitmap
import android.util.LruCache
import androidx.annotation.WorkerThread
import com.darkframe.icons.engine.domain.CompleteLook

/**
 * Memory cache for look previews.
 *
 * The home screen shows one large preview and six small ones, and they must survive a scroll, a
 * rotation and a fold without being redrawn — a preview is a wallpaper plus eight themed icons, so
 * re-rendering them on every layout pass would reintroduce exactly the load the engine was fixed to
 * avoid. Bounded by bytes like the icon cache, and small: there are only six looks.
 */
class LookPreviewCache(context: Context) {

    private val renderer = LookPreviewRenderer(context)

    /**
     * Preview rendering is serialised.
     *
     * [LookPreviewRenderer] reuses its Paint and geometry objects and is therefore not thread-safe,
     * and the engine's render pool has two threads. Serialising also means the home screen's six
     * cards cannot start the same preview twice: the second caller waits briefly and then finds the
     * finished bitmap in the cache. Previews are few and large, so this costs nothing.
     */
    private val renderLock = Any()

    private val cache = object : LruCache<String, Bitmap>(BUDGET_BYTES) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    private fun key(look: CompleteLook, widthPx: Int, heightPx: Int) =
        "${look.id}:${widthPx}x$heightPx"

    /** Main-thread-safe lookup, so an already-rendered preview appears without a blank frame. */
    fun peek(look: CompleteLook, widthPx: Int, heightPx: Int): Bitmap? =
        cache.get(key(look, widthPx, heightPx))

    @WorkerThread
    fun get(look: CompleteLook, widthPx: Int, heightPx: Int): Bitmap {
        val key = key(look, widthPx, heightPx)
        cache.get(key)?.let { return it }
        synchronized(renderLock) {
            // Re-checked inside the lock: whoever we waited for has very likely just produced it.
            cache.get(key)?.let { return it }
            val rendered = renderer.render(look, widthPx, heightPx)
            cache.put(key, rendered)
            return rendered
        }
    }

    fun clear() = cache.evictAll()

    private companion object {
        /** Six looks at two sizes, with headroom. Previews are large but few. */
        const val BUDGET_BYTES = 12 * 1024 * 1024
    }
}
