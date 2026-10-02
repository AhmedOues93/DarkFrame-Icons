package com.darkframe.icons.ui.browser

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.darkframe.icons.engine.DarkFrameEngine
import com.darkframe.icons.engine.apply.ApplyCapability
import com.darkframe.icons.engine.apply.LauncherProfile
import com.darkframe.icons.engine.data.AppCatalogWatcher
import com.darkframe.icons.engine.data.PackageChange
import com.darkframe.icons.engine.data.StylePreferenceStore
import com.darkframe.icons.engine.domain.AppCatalogBuilder
import com.darkframe.icons.engine.domain.AppIdentity
import com.darkframe.icons.engine.domain.IconStyle
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * State and work coordination for the icon browser.
 *
 * Holds no drawing code: every pixel comes from [com.darkframe.icons.engine.IconResolver]. What
 * lives here is the part that genuinely belongs to the screen — which collection is selected, the
 * current search, and keeping the list in step with apps appearing and disappearing while the user
 * is looking at it.
 */
class IconBrowserViewModel @JvmOverloads constructor(
    application: Application,
    // @JvmOverloads is load-bearing, not decoration: the default ViewModel factory finds a
    // constructor by reflection, and for an AndroidViewModel it looks for one taking exactly
    // (Application). Kotlin default arguments alone do not emit that overload, so without this the
    // screen fails at runtime with "Cannot create an instance of IconBrowserViewModel".
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val renderDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : AndroidViewModel(application), ThemedIconLoader {

    data class UiState(
        val loading: Boolean = true,
        val style: IconStyle,
        val query: String = "",
        val visibleApps: List<AppIdentity> = emptyList(),
        val totalApps: Int = 0,
        val curatedApps: Int = 0,
        val launcher: LauncherProfile? = null,
        val capability: ApplyCapability? = null,
    )

    private val engine = DarkFrameEngine.get(application)
    private val stylePreferences = StylePreferenceStore(application)

    private var catalog: List<AppIdentity> = emptyList()

    private val _state = MutableStateFlow(UiState(style = stylePreferences.selectedStyle()))
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val watcher = AppCatalogWatcher(application) { change -> onPackageChange(change) }

    init {
        watcher.start()
        refresh()
        viewModelScope.launch {
            val profile = withContext(ioDispatcher) { engine.apply.currentLauncher() }
            val capability = withContext(ioDispatcher) { engine.apply.effectiveCapability(profile) }
            _state.update { it.copy(launcher = profile, capability = capability) }
        }
    }

    override fun onCleared() {
        watcher.stop()
        super.onCleared()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            val loaded = withContext(ioDispatcher) { engine.installedApps.loadCatalog() }
            val curated = withContext(ioDispatcher) {
                loaded.count { engine.resolver.isCurated(it) }
            }
            catalog = loaded
            _state.update { current ->
                current.copy(
                    loading = false,
                    visibleApps = AppCatalogBuilder.filter(loaded, current.query),
                    totalApps = loaded.size,
                    curatedApps = curated,
                )
            }
        }
    }

    fun setQuery(query: String) {
        _state.update { it.copy(query = query, visibleApps = AppCatalogBuilder.filter(catalog, query)) }
    }

    fun selectStyle(style: IconStyle) {
        if (_state.value.style.id == style.id) return
        stylePreferences.setSelectedStyle(style)
        // The catalog itself is unchanged, so only the style swaps; the grid re-requests its icons
        // and the cache answers for any combination already rendered.
        _state.update { it.copy(style = style) }
    }

    override fun peek(identity: AppIdentity, sizePx: Int): Bitmap? =
        engine.resolver.peek(identity, _state.value.style, sizePx)

    override suspend fun load(identity: AppIdentity, sizePx: Int): Bitmap {
        val style = _state.value.style
        return withContext(renderDispatcher) { engine.resolver.resolve(identity, style, sizePx) }
    }

    override fun isCurated(identity: AppIdentity): Boolean = engine.resolver.isCurated(identity)

    /**
     * Keeps the visible list truthful while the user is on the screen.
     *
     * Every change reloads the catalog, including an update that leaves the set of apps identical.
     * That is not redundant: the cache key is built from the [AppIdentity] the catalog is holding,
     * so an in-memory identity carrying the *old* change stamp would keep producing the old key and
     * the old icon. Re-reading the catalog is what picks up the new stamp, and the cache miss
     * follows from that.
     */
    private fun onPackageChange(change: PackageChange) {
        viewModelScope.launch {
            withContext(ioDispatcher) { engine.resolver.invalidate(change.packageName) }
            refresh()
        }
    }

    /** Reclaims cache disk once the user leaves a burst of rendering behind. */
    fun trimCache() {
        viewModelScope.launch { withContext(ioDispatcher) { engine.resolver.trimCache() } }
    }

    fun clearCache(onDone: () -> Unit) {
        viewModelScope.launch {
            withContext(ioDispatcher) { engine.resolver.invalidateAll() }
            onDone()
        }
    }
}
