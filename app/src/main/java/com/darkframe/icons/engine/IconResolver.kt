package com.darkframe.icons.engine

import android.graphics.Bitmap
import androidx.annotation.WorkerThread
import com.darkframe.icons.engine.data.CuratedIconRepository
import com.darkframe.icons.engine.data.IconCache
import com.darkframe.icons.engine.domain.AppIdentity
import com.darkframe.icons.engine.domain.IconCacheKey
import com.darkframe.icons.engine.domain.IconStyle
import com.darkframe.icons.engine.domain.Monogram
import com.darkframe.icons.engine.render.IconRenderer
import com.darkframe.icons.engine.render.IconSourceLoader

/**
 * The dynamic icon engine's entry point.
 *
 * Implements the one flow the whole product rests on:
 *
 * ```
 * installed app
 *   -> read package/component + original icon
 *   -> is there a curated DarkFrame override?
 *        yes -> use the curated glyph
 *        no  -> use the app's own installed icon
 *   -> normalise size / padding / shape
 *   -> apply the selected DarkFrame style
 *   -> cache
 *   -> themed icon
 * ```
 *
 * The important property is the `no` branch: it is the *normal* path, not a fallback. DarkFrame
 * themes an app it has never seen — one installed tomorrow, one that exists only on this user's
 * device — with no curated artwork and no code change, because the app's own icon is a perfectly
 * good source once it has been normalised. Curated artwork raises quality for the apps we have
 * drawn; it never gates coverage.
 *
 * Every method here is blocking and must be called off the main thread. Rendering is CPU-bound and
 * the engine deliberately does not hide that behind an internal dispatcher — the caller owns
 * concurrency, so a screen can cancel a burst of work it no longer needs.
 */
class IconResolver(
    private val curatedIcons: CuratedIconRepository,
    private val sourceLoader: IconSourceLoader,
    private val cache: IconCache,
    private val rendererProvider: () -> IconRenderer = { IconRenderer() },
) {

    // IconRenderer is not thread-safe (it reuses Paint and geometry objects), so each thread that
    // renders gets its own. One instance behind a lock would serialise the whole grid.
    private val renderer = ThreadLocal.withInitial(rendererProvider)

    /** Non-blocking peek, safe on the main thread. Used to avoid a flicker on already-warm icons. */
    fun peek(identity: AppIdentity, style: IconStyle, sizePx: Int): Bitmap? {
        val curated = curatedIcons.hasOverrideFor(identity)
        return cache.fromMemory(IconCacheKey.of(identity, style, sizePx, curated))
    }

    /**
     * Resolves the themed icon for [identity] in [style] at [sizePx], rendering it if necessary.
     *
     * Never returns null and never throws for an app whose artwork is unusable: that case degrades
     * to a styled monogram, because one broken install must not leave a hole in the grid.
     */
    @WorkerThread
    fun resolve(identity: AppIdentity, style: IconStyle, sizePx: Int): Bitmap {
        val curatedId = curatedIcons.curatedDrawableId(identity)
        val key = IconCacheKey.of(identity, style, sizePx, curated = curatedId != null)

        cache.get(key, identity.packageName)?.let { return it }

        val source = sourceLoader.load(identity, style, curatedId)
        val rendered = renderer.get()!!.render(
            style = style,
            source = source,
            sizePx = sizePx,
            monogramText = Monogram.initials(identity.label),
        )
        cache.put(key, identity.packageName, rendered)

        // The source buffer is large (288px ARGB_8888) and is of no further use once composited.
        source?.bitmap?.let { if (!it.isRecycled) it.recycle() }
        return rendered
    }

    /** True when this app is drawn from DarkFrame's own artwork rather than its installed icon. */
    fun isCurated(identity: AppIdentity): Boolean = curatedIcons.hasOverrideFor(identity)

    /** Drops every cached render for a package. Call on uninstall, update or disable. */
    @WorkerThread
    fun invalidate(packageName: String) = cache.invalidatePackage(packageName)

    /** Drops every cached render. Exposed to the user as "rebuild icon cache". */
    @WorkerThread
    fun invalidateAll() = cache.clear()

    /** Reclaims disk once a burst of rendering has settled. */
    @WorkerThread
    fun trimCache() = cache.trimToBudget()
}
