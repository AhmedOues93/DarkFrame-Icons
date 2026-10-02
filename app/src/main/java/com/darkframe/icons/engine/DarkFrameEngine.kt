package com.darkframe.icons.engine

import android.content.Context
import android.os.Process
import com.darkframe.icons.engine.apply.IconApplyService
import com.darkframe.icons.engine.data.CuratedIconRepository
import com.darkframe.icons.engine.data.IconCache
import com.darkframe.icons.engine.data.InstalledAppRepository
import com.darkframe.icons.engine.domain.IconRenderPolicy
import com.darkframe.icons.engine.render.IconSourceLoader
import com.darkframe.icons.engine.render.LookPreviewCache
import com.darkframe.icons.engine.wallpaper.WallpaperApplier
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * Process-wide wiring for the icon engine.
 *
 * A hand-written service locator rather than a DI framework: the graph is six objects deep and
 * entirely unconditional, which is well under the point where a framework starts paying for its
 * build-time and indirection cost. The shape is kept constructor-injected throughout so introducing
 * one later is a wiring change and nothing else.
 *
 * Holds an application context only, never an Activity.
 */
class DarkFrameEngine private constructor(context: Context) {

    val installedApps = InstalledAppRepository(context)
    val curatedIcons = CuratedIconRepository(context)
    val cache = IconCache(context)
    val resolver = IconResolver(
        curatedIcons = curatedIcons,
        sourceLoader = IconSourceLoader(context),
        cache = cache,
    )
    val apply = IconApplyService(context)
    val wallpapers = WallpaperApplier(context)
    val lookPreviews = LookPreviewCache(context)

    /**
     * The only dispatcher icon rendering may run on.
     *
     * Deliberately not `Dispatchers.Default`: that sizes itself to the core count, so a grid filling
     * up would put bitmap work on all eight cores of a Fold8 at once. That is what made the device
     * noticeably warm, and it buys nothing — a screen holds about a dozen cells and each render
     * takes tens of milliseconds, so two workers fill it faster than anyone can scroll.
     *
     * The threads also run at background priority, which asks the scheduler for the leftover CPU
     * rather than competing with the UI thread for it.
     */
    val renderDispatcher: CoroutineDispatcher = run {
        val threads = IconRenderPolicy.maxParallelRenders(Runtime.getRuntime().availableProcessors())
        val counter = AtomicInteger(0)
        Executors.newFixedThreadPool(threads) { runnable ->
            Thread({
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
                runnable.run()
            }, "darkframe-render-${counter.incrementAndGet()}").apply { isDaemon = true }
        }.asCoroutineDispatcher()
    }

    companion object {
        @Volatile
        private var instance: DarkFrameEngine? = null

        fun get(context: Context): DarkFrameEngine {
            instance?.let { return it }
            return synchronized(this) {
                instance ?: DarkFrameEngine(context.applicationContext).also { instance = it }
            }
        }
    }
}
