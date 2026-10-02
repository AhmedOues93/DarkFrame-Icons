package com.darkframe.icons.engine

import android.content.Context
import com.darkframe.icons.engine.apply.IconApplyService
import com.darkframe.icons.engine.data.CuratedIconRepository
import com.darkframe.icons.engine.data.IconCache
import com.darkframe.icons.engine.data.InstalledAppRepository
import com.darkframe.icons.engine.render.IconSourceLoader

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
