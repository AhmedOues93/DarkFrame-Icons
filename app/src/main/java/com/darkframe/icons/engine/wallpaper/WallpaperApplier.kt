package com.darkframe.icons.engine.wallpaper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Point
import androidx.annotation.WorkerThread
import kotlin.math.max

/** Which surface a wallpaper goes on. */
enum class WallpaperTarget { HOME, LOCK, BOTH }

/** What happened when a wallpaper was applied. */
sealed interface WallpaperOutcome {
    data class Applied(val target: WallpaperTarget) : WallpaperOutcome
    data class Failed(val reason: String) : WallpaperOutcome
}

/**
 * Sets DarkFrame wallpapers.
 *
 * This is the one part of a Complete Look that DarkFrame really can put in place itself —
 * [WallpaperManager] is a genuine public API and `SET_WALLPAPER` is a normal permission. Icons and
 * widgets are not like that, which is why the look model keeps them separate.
 */
class WallpaperApplier(private val context: Context) {

    private val renderer = WallpaperRenderer()

    /**
     * The size a wallpaper should be drawn at.
     *
     * Asks [WallpaperManager] for its desired dimensions first, because on One UI that already
     * accounts for the panel the user is on. Falls back to the current display, never to a
     * hard-coded size — a Fold's inner screen and its cover screen are nothing alike.
     */
    fun desiredSize(): Point {
        val manager = WallpaperManager.getInstance(context)
        val desired = Point(manager.desiredMinimumWidth, manager.desiredMinimumHeight)
        if (desired.x > 0 && desired.y > 0) return desired

        val metrics = context.resources.displayMetrics
        return Point(max(1, metrics.widthPixels), max(1, metrics.heightPixels))
    }

    @WorkerThread
    fun apply(spec: WallpaperSpec, target: WallpaperTarget): WallpaperOutcome {
        val size = desiredSize()
        return runCatching {
            val bitmap = renderer.render(spec, size.x, size.y)
            val flags = when (target) {
                WallpaperTarget.HOME -> WallpaperManager.FLAG_SYSTEM
                WallpaperTarget.LOCK -> WallpaperManager.FLAG_LOCK
                WallpaperTarget.BOTH -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
            }
            WallpaperManager.getInstance(context).setBitmap(bitmap, null, true, flags)
            bitmap.recycle()
            WallpaperOutcome.Applied(target) as WallpaperOutcome
        }.getOrElse { error ->
            WallpaperOutcome.Failed(error.message ?: "The system refused the wallpaper.")
        }
    }

    /** Preview bitmap at thumbnail size. Cheap enough to render per visible cell. */
    @WorkerThread
    fun preview(spec: WallpaperSpec, widthPx: Int, heightPx: Int) =
        renderer.render(spec, widthPx, heightPx)

    /**
     * True when the lock screen can be set independently.
     *
     * Always true at DarkFrame's minSdk of 26 — separate lock wallpapers arrived in API 24 — so this
     * is kept as a named concept for the UI rather than as a version check.
     */
    fun supportsSeparateLockScreen(): Boolean = true
}
