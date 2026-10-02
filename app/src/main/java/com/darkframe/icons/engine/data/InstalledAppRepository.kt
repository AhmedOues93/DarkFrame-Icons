package com.darkframe.icons.engine.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import androidx.annotation.WorkerThread
import com.darkframe.icons.engine.domain.AppCatalogBuilder
import com.darkframe.icons.engine.domain.AppIdentity
import com.darkframe.icons.engine.domain.RawLauncherEntry

/**
 * Enumerates the launchable applications on this device.
 *
 * ## Package visibility
 *
 * Since Android 11 (API 30) an app sees only the packages it declares an interest in. DarkFrame
 * declares exactly one `<queries><intent>` in its manifest: `ACTION_MAIN` + `CATEGORY_LAUNCHER`.
 * That is the narrowest declaration that can answer the question the product exists to answer —
 * "which apps appear in your launcher?" — and it is self-limiting: services, content providers and
 * apps with no launcher entry stay invisible to DarkFrame, as they should.
 *
 * `QUERY_ALL_PACKAGES` is deliberately **not** requested. It would be broader than the feature
 * needs, and Google Play restricts it to a short list of use cases that an icon customiser is not
 * on, so requesting it would put the listing at risk for no functional gain.
 *
 * ## What this means for the user
 *
 * The catalog is "every app Android is willing to tell us about", which on a normal device is the
 * user's whole app drawer. It is not guaranteed to be literally every installed package, and the
 * product must never imply otherwise. Known, documented gaps:
 *  - apps with no launcher activity (they have no launcher icon to theme);
 *  - work-profile and Secure Folder apps, which only the active home launcher may enumerate;
 *  - packages hidden by device policy.
 */
class InstalledAppRepository(private val context: Context) {

    private val packageManager: PackageManager get() = context.packageManager

    /**
     * Blocking. On a device with several hundred apps this costs tens of milliseconds plus label
     * loading, so it belongs on a background dispatcher.
     */
    @WorkerThread
    fun loadCatalog(): List<AppIdentity> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = queryLauncherActivities(intent)

        // One package can contribute several launcher activities; its change stamp is looked up
        // once rather than once per activity.
        val stampCache = HashMap<String, Long>()

        val raw = resolved.mapNotNull { info ->
            val activityInfo = info.activityInfo ?: return@mapNotNull null
            val packageName = activityInfo.packageName ?: return@mapNotNull null
            RawLauncherEntry(
                packageName = packageName,
                activityName = activityInfo.name ?: return@mapNotNull null,
                label = runCatching { info.loadLabel(packageManager)?.toString() }
                    .getOrNull()
                    .orEmpty(),
                versionStamp = stampCache.getOrPut(packageName) { changeStampOf(packageName) },
            )
        }

        return AppCatalogBuilder.build(raw, context.packageName)
    }

    private fun queryLauncherActivities(intent: Intent): List<ResolveInfo> = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(intent, 0)
        }
    }.getOrDefault(emptyList())

    /**
     * `lastUpdateTime` is the engine's cache-invalidation signal: it changes on install and on
     * every update, which is exactly when a package's icon may have changed. Version codes are a
     * worse choice here because a sideloaded rebuild can reuse one.
     */
    private fun changeStampOf(packageName: String): Long = runCatching {
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, 0)
        }
        info.lastUpdateTime
    }.getOrDefault(0L)

    /** True when this device still exposes the launcher activity behind [identity]. */
    @WorkerThread
    fun isStillInstalled(identity: AppIdentity): Boolean = runCatching {
        packageManager.getApplicationInfo(identity.packageName, 0).enabled
    }.getOrDefault(false)
}
