package com.darkframe.icons.ui.detail

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import com.darkframe.icons.engine.DarkFrameEngine
import com.darkframe.icons.engine.apply.ApplyCapability
import com.darkframe.icons.engine.domain.AppIdentity
import com.darkframe.icons.engine.domain.IconRenderPolicy
import com.darkframe.icons.engine.domain.IconStyle
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Icons for one app in any collection.
 *
 * The browser's view model is scoped to the single collection the grid is showing, which is right
 * there and wrong here: the whole point of the detail screen is comparing one app across all six.
 * Everything still goes through the engine at the same bucketed preview sizes, so the large preview
 * and the grid cell behind it share cache entries wherever the sizes land in the same bucket.
 */
class IconDetailViewModel @JvmOverloads constructor(
    application: Application,
    // See IconBrowserViewModel: the default factory reflects for a constructor taking exactly
    // (Application), which Kotlin default arguments alone do not emit.
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AndroidViewModel(application), StyledIconLoader {

    private val engine = DarkFrameEngine.get(application)

    override fun peek(identity: AppIdentity, style: IconStyle, sizePx: Int): Bitmap? =
        engine.resolver.peek(identity, style, IconRenderPolicy.previewSizePx(sizePx))

    override suspend fun load(identity: AppIdentity, style: IconStyle, sizePx: Int): Bitmap =
        withContext(engine.renderDispatcher) {
            engine.resolver.resolve(identity, style, IconRenderPolicy.previewSizePx(sizePx))
        }

    override fun isCurated(identity: AppIdentity): Boolean = engine.resolver.isCurated(identity)

    /** Full resolution, only for an export or a launcher hand-off. */
    suspend fun loadForExport(identity: AppIdentity, style: IconStyle): Bitmap =
        withContext(engine.renderDispatcher) {
            engine.resolver.resolve(identity, style, IconRenderPolicy.EXPORT_PX)
        }

    suspend fun capability(): ApplyCapability = withContext(ioDispatcher) {
        engine.apply.effectiveCapability(engine.apply.currentLauncher())
    }
}

/**
 * A finished bitmap for one app in one named collection.
 *
 * Separate from [com.darkframe.icons.ui.browser.ThemedIconLoader], which carries the selected
 * collection implicitly, because a screen that shows several collections at once has to name the
 * one it wants. Both are the same narrow contract: ask for pixels, get pixels, make no drawing
 * decisions.
 */
interface StyledIconLoader {
    fun peek(identity: AppIdentity, style: IconStyle, sizePx: Int): Bitmap?
    suspend fun load(identity: AppIdentity, style: IconStyle, sizePx: Int): Bitmap
    fun isCurated(identity: AppIdentity): Boolean
}
