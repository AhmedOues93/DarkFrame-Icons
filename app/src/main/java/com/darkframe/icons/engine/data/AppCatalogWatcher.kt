package com.darkframe.icons.engine.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat

/** What happened to a package on the device. */
sealed interface PackageChange {
    val packageName: String

    data class Installed(override val packageName: String) : PackageChange
    data class Updated(override val packageName: String) : PackageChange
    data class Removed(override val packageName: String) : PackageChange

    /** Enabled/disabled, or a component's enabled state changed. */
    data class Changed(override val packageName: String) : PackageChange
}

/**
 * Keeps the catalog and the cache honest as apps come and go.
 *
 * Registered dynamically rather than in the manifest: since Android 8 a manifest receiver is the
 * wrong tool for this. These broadcasts are only interesting while the user is actually looking at
 * DarkFrame's app list, and waking the process in the background to re-render icons nobody is
 * about to see would be a battery cost with no user-visible benefit.
 *
 * Note the division of labour with [IconCache]: an install or update needs no cache invalidation
 * for *correctness*, because the package's change stamp is part of every cache key, so a new build
 * of an app can never be served an old bitmap. Invalidating anyway is simply housekeeping — it
 * reclaims the space the superseded entries occupy.
 */
class AppCatalogWatcher(
    private val context: Context,
    private val onChange: (PackageChange) -> Unit,
) {

    private var receiver: BroadcastReceiver? = null

    fun start() {
        if (receiver != null) return
        val listener = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val change = classify(intent) ?: return
                onChange(change)
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            // Required: without a data scheme these actions match nothing.
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(
            context,
            listener,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        receiver = listener
    }

    fun stop() {
        receiver?.let { runCatching { context.unregisterReceiver(it) } }
        receiver = null
    }

    private fun classify(intent: Intent?): PackageChange? {
        val action = intent?.action ?: return null
        val packageName = intent.data?.schemeSpecificPart?.takeIf { it.isNotBlank() } ?: return null
        // During an update the platform sends REMOVED then ADDED, both with EXTRA_REPLACING set.
        // Treating those as an uninstall would wipe cache entries that are about to be rebuilt,
        // and would briefly show the app as gone.
        val replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
        return when (action) {
            Intent.ACTION_PACKAGE_ADDED ->
                if (replacing) PackageChange.Updated(packageName) else PackageChange.Installed(packageName)
            Intent.ACTION_PACKAGE_REPLACED -> PackageChange.Updated(packageName)
            Intent.ACTION_PACKAGE_REMOVED ->
                if (replacing) null else PackageChange.Removed(packageName)
            Intent.ACTION_PACKAGE_CHANGED -> PackageChange.Changed(packageName)
            else -> null
        }
    }
}
