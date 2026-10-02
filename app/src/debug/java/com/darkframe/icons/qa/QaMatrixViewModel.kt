package com.darkframe.icons.qa

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.darkframe.icons.engine.DarkFrameEngine
import com.darkframe.icons.engine.domain.AppCatalogBuilder
import com.darkframe.icons.engine.domain.AppIdentity
import com.darkframe.icons.engine.domain.IconRenderPolicy
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
 * Icons for the QA matrix, by app *and* collection.
 *
 * Deliberately not reusing the browser's view model: that one holds a single selected collection,
 * which is correct for the browser and exactly wrong here. Everything else goes through the same
 * engine calls at the same bucketed preview sizes, so what the matrix shows is what the real app
 * shows — a QA screen that rendered differently would be worse than no QA screen.
 */
class QaMatrixViewModel @JvmOverloads constructor(
    application: Application,
    // Same reason as IconBrowserViewModel: the default factory looks for a constructor taking
    // exactly (Application), and Kotlin default arguments alone do not emit that overload.
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AndroidViewModel(application), QaIconLoader {

    data class UiState(
        val loading: Boolean = true,
        val query: String = "",
        val visibleApps: List<AppIdentity> = emptyList(),
        val totalApps: Int = 0,
        val curatedApps: Int = 0,
    )

    private val engine = DarkFrameEngine.get(application)
    private var catalog: List<AppIdentity> = emptyList()

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
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

    override fun peek(identity: AppIdentity, style: IconStyle, sizePx: Int): Bitmap? =
        engine.resolver.peek(identity, style, IconRenderPolicy.previewSizePx(sizePx))

    override suspend fun load(identity: AppIdentity, style: IconStyle, sizePx: Int): Bitmap {
        val previewPx = IconRenderPolicy.previewSizePx(sizePx)
        return withContext(engine.renderDispatcher) {
            engine.resolver.resolve(identity, style, previewPx)
        }
    }

    override fun isCurated(identity: AppIdentity): Boolean = engine.resolver.isCurated(identity)

    fun trimCache() {
        viewModelScope.launch { withContext(ioDispatcher) { engine.resolver.trimCache() } }
    }
}

/** What the matrix adapter is allowed to ask for: a finished bitmap for one app in one collection. */
interface QaIconLoader {
    fun peek(identity: AppIdentity, style: IconStyle, sizePx: Int): Bitmap?
    suspend fun load(identity: AppIdentity, style: IconStyle, sizePx: Int): Bitmap
    fun isCurated(identity: AppIdentity): Boolean
}
