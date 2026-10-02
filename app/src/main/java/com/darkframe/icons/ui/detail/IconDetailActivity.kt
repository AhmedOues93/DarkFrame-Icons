package com.darkframe.icons.ui.detail

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.darkframe.icons.R
import com.darkframe.icons.billing.Entitlement
import com.darkframe.icons.billing.ProEntitlementStore
import com.darkframe.icons.data.FavoriteKind
import com.darkframe.icons.data.FavoritesStore
import com.darkframe.icons.engine.DarkFrameEngine
import com.darkframe.icons.engine.apply.ApplyCapability
import com.darkframe.icons.engine.apply.ApplyOutcome
import com.darkframe.icons.engine.data.StylePreferenceStore
import com.darkframe.icons.engine.domain.AppIdentity
import com.darkframe.icons.engine.domain.IconStyle
import com.darkframe.icons.engine.domain.IconStyleCatalog
import com.darkframe.icons.model.ContentTier
import com.darkframe.icons.ui.ProActivity
import com.darkframe.icons.ui.common.DarkFrameActivity
import com.darkframe.icons.ui.common.applySystemBarPadding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * One app, up close.
 *
 * This replaces the list dialog the grid used to open, which was the last developer-shaped thing in
 * the browser: a stack of verbs with no picture of what any of them would produce. The screen is
 * built around the preview instead — large, at the collection the user is looking at, with the other
 * five rendered underneath so switching is a comparison rather than a guess.
 *
 * Only actions that are real on this device appear. A themed shortcut is offered on launchers where
 * a pinned shortcut is genuinely the best available mechanism and withheld everywhere else, because
 * on Samsung and on icon-pack launchers it adds a second icon beside one that is about to be themed
 * properly.
 */
class IconDetailActivity : DarkFrameActivity() {

    private val viewModel: IconDetailViewModel by viewModels()

    private lateinit var identity: AppIdentity
    private lateinit var preview: ImageView
    private lateinit var collectionName: TextView
    private lateinit var collectionNote: TextView
    private lateinit var favourite: TextView
    private lateinit var export: TextView
    private lateinit var shortcut: TextView
    private lateinit var swatches: CollectionSwatchAdapter

    private lateinit var style: IconStyle
    private var capability: ApplyCapability? = null
    private var previewJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val parsed = identityFrom(intent)
        if (parsed == null) {
            // Nothing sensible to show, and a blank detail screen is worse than going back.
            finish()
            return
        }
        identity = parsed
        style = IconStyleCatalog.forId(
            intent.getStringExtra(EXTRA_STYLE_ID) ?: StylePreferenceStore(this).selectedStyle().id,
        )

        setContentView(R.layout.activity_icon_detail)
        findViewById<View>(R.id.detail_scroll).applySystemBarPadding()

        preview = findViewById(R.id.detail_preview)
        preview.clipToOutline = true
        collectionName = findViewById(R.id.detail_collection)
        collectionNote = findViewById(R.id.detail_collection_note)
        favourite = findViewById(R.id.detail_favourite)
        export = findViewById(R.id.detail_export)
        shortcut = findViewById(R.id.detail_shortcut)

        findViewById<TextView>(R.id.detail_title).text = identity.displayLabel()
        findViewById<TextView>(R.id.detail_subtitle).text = subtitle()

        setUpSwatches()
        bindStyle()
        bindFavourite()

        export.setOnClickListener { if (requireTier()) exportIcon() }
        favourite.setOnClickListener { toggleFavourite() }
        shortcut.setOnClickListener { if (requireTier()) pinShortcut() }

        // The shortcut row is hidden until the launcher has been identified, rather than shown and
        // then taken away, which would read as the app changing its mind.
        shortcut.visibility = View.GONE
        lifecycleScope.launch {
            capability = viewModel.capability()
            shortcut.visibility =
                if (capability == ApplyCapability.PINNED_SHORTCUT) View.VISIBLE else View.GONE
        }
    }

    private fun subtitle(): String {
        val curated = viewModel.isCurated(identity)
        return if (curated) {
            getString(R.string.detail_subtitle_curated)
        } else {
            getString(R.string.detail_subtitle_generated)
        }
    }

    private fun setUpSwatches() {
        val strip = findViewById<RecyclerView>(R.id.detail_swatches)
        swatches = CollectionSwatchAdapter(
            loader = viewModel,
            scope = lifecycleScope,
            sizePx = resources.getDimensionPixelSize(R.dimen.df_swatch_icon),
            onSelect = ::selectStyle,
        )
        strip.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        strip.adapter = swatches
        swatches.bind(identity, style)
    }

    private fun selectStyle(selected: IconStyle) {
        if (selected.id == style.id) return
        style = selected
        swatches.select(selected)
        bindStyle()
    }

    private fun bindStyle() {
        collectionName.text = style.displayName
        collectionNote.text = style.description

        // Sized from the window rather than from the view: a measured width is 0 on the first pass
        // and real on the second, which would key two renders of the same preview.
        val size = previewSizePx()
        previewJob?.cancel()
        val warm = viewModel.peek(identity, style, size)
        if (warm != null) {
            preview.setImageBitmap(warm)
            return
        }
        preview.setImageDrawable(null)
        previewJob = lifecycleScope.launch {
            preview.setImageBitmap(viewModel.load(identity, style, size))
        }
    }

    /**
     * Edge of the large preview, as a share of the window's shorter side.
     *
     * Derived rather than fixed so the preview grows on a Fold's inner screen and on a tablet
     * instead of leaving a phone-sized picture in the middle of a wide page — and capped, because
     * past a point a bigger preview is just a bigger upscale of the same bucketed render.
     */
    private fun previewSizePx(): Int {
        val metrics = resources.displayMetrics
        val shorter = minOf(metrics.widthPixels, metrics.heightPixels)
        val max = resources.getDimensionPixelSize(R.dimen.df_detail_preview_max)
        return (shorter * PREVIEW_SHARE).toInt().coerceIn(1, max)
    }

    private fun bindFavourite() {
        val saved = FavoritesStore(this).isFavorite(FavoriteKind.APP, identity.componentKey)
        favourite.text = getString(
            if (saved) R.string.detail_unfavourite else R.string.detail_favourite,
        )
        favourite.isSelected = saved
    }

    private fun toggleFavourite() {
        FavoritesStore(this).toggle(FavoriteKind.APP, identity.componentKey)
        bindFavourite()
    }

    /** Browsing a Pro collection is free; taking one of its renders off the device is not. */
    private fun requireTier(): Boolean {
        if (style.tier != ContentTier.PRO) return true
        if (ProEntitlementStore(this).current() == Entitlement.PRO) return true
        AlertDialog.Builder(this)
            .setTitle(R.string.pro_required_title)
            .setMessage(getString(R.string.pro_required_message, style.displayName))
            .setPositiveButton(R.string.pro_required_open) { _, _ ->
                startActivity(Intent(this, ProActivity::class.java))
            }
            .setNegativeButton(R.string.close, null)
            .show()
        return false
    }

    private fun exportIcon() {
        lifecycleScope.launch {
            val engine = DarkFrameEngine.get(applicationContext)
            val icon = viewModel.loadForExport(identity, style)
            val uri = withContext(Dispatchers.IO) { engine.apply.exportIcon(identity, style, icon) }
            if (uri == null) {
                toastDialog(getString(R.string.browser_export_failed))
                return@launch
            }
            startActivity(
                Intent.createChooser(
                    engine.apply.shareIntent(uri),
                    getString(R.string.browser_share_title),
                ),
            )
        }
    }

    private fun pinShortcut() {
        lifecycleScope.launch {
            val engine = DarkFrameEngine.get(applicationContext)
            val icon = viewModel.loadForExport(identity, style)
            val outcome = withContext(Dispatchers.IO) { engine.apply.pinThemedShortcut(identity, icon) }
            toastDialog(
                when (outcome) {
                    ApplyOutcome.Requested -> getString(R.string.browser_pin_requested)
                    is ApplyOutcome.NotSupported -> outcome.reason
                    is ApplyOutcome.Failed -> outcome.reason
                },
            )
        }
    }

    private fun toastDialog(message: String) {
        AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton(R.string.close, null)
            .show()
    }

    companion object {
        private const val EXTRA_PACKAGE = "package"
        private const val EXTRA_ACTIVITY = "activity"
        private const val EXTRA_LABEL = "label"
        private const val EXTRA_STAMP = "stamp"
        private const val EXTRA_SECONDARY = "secondary"
        private const val EXTRA_WORK = "work"
        private const val EXTRA_STYLE_ID = "style"

        /** Share of the window's shorter side the large preview occupies. */
        private const val PREVIEW_SHARE = 0.52f

        /**
         * The identity travels in the intent rather than being looked up again.
         *
         * Re-reading the catalog to find one app would make opening a detail screen cost a full
         * PackageManager scan, which is exactly the kind of incidental work the performance pass
         * removed. The caller already holds a current identity — including the change stamp the
         * cache key is built from — so it is passed straight through.
         */
        fun intent(context: Context, identity: AppIdentity, style: IconStyle): Intent =
            Intent(context, IconDetailActivity::class.java)
                .putExtra(EXTRA_PACKAGE, identity.packageName)
                .putExtra(EXTRA_ACTIVITY, identity.activityName)
                .putExtra(EXTRA_LABEL, identity.label)
                .putExtra(EXTRA_STAMP, identity.versionStamp)
                .putExtra(EXTRA_SECONDARY, identity.isSecondaryEntryPoint)
                .putExtra(EXTRA_WORK, identity.isWorkProfile)
                .putExtra(EXTRA_STYLE_ID, style.id)

        private fun identityFrom(intent: Intent): AppIdentity? {
            val pkg = intent.getStringExtra(EXTRA_PACKAGE)?.takeIf { it.isNotBlank() } ?: return null
            val activity = intent.getStringExtra(EXTRA_ACTIVITY)?.takeIf { it.isNotBlank() }
                ?: return null
            return AppIdentity(
                packageName = pkg,
                activityName = activity,
                label = intent.getStringExtra(EXTRA_LABEL).orEmpty().ifBlank { pkg },
                versionStamp = intent.getLongExtra(EXTRA_STAMP, 0L),
                isSecondaryEntryPoint = intent.getBooleanExtra(EXTRA_SECONDARY, false),
                isWorkProfile = intent.getBooleanExtra(EXTRA_WORK, false),
            )
        }
    }
}
