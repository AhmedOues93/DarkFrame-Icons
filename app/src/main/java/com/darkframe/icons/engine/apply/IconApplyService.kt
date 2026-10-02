package com.darkframe.icons.engine.apply

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.annotation.WorkerThread
import androidx.core.content.FileProvider
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.darkframe.icons.engine.domain.AppIdentity
import com.darkframe.icons.engine.domain.IconStyle
import java.io.File

/** Outcome of trying to apply a themed icon. Every branch is something we can honestly report. */
sealed interface ApplyOutcome {
    /** The system accepted the request. On most launchers the user still confirms a dialog. */
    data object Requested : ApplyOutcome

    /** This launcher cannot do it; [reason] is shown to the user verbatim. */
    data class NotSupported(val reason: String) : ApplyOutcome

    data class Failed(val reason: String) : ApplyOutcome
}

/**
 * Everything DarkFrame can legitimately do to get a themed icon onto a home screen.
 *
 * ## The limitation, stated plainly
 *
 * **Generating a themed icon and applying it to a launcher are two different things.** Android has
 * no API — public, permission-gated or otherwise — that lets an app replace another app's icon
 * across the system. The launcher owns what it draws. Any product claiming to silently restyle your
 * home screen on stock Android is either describing a launcher it also wrote, or describing
 * root/Xposed, or lying.
 *
 * So DarkFrame offers exactly three real mechanisms, chosen per launcher by
 * [LauncherCapabilityTable]:
 *
 * 1. **Icon pack** — launchers such as Nova, Lawnchair and Smart Launcher read the long-standing
 *    `appfilter.xml` icon-pack format. DarkFrame ships one; the user selects DarkFrame in that
 *    launcher's settings and it substitutes icons itself. We cannot select it for them: there is no
 *    API for that either.
 * 2. **Samsung Theme Park** — on One UI, DarkFrame prepares the icon pack in the standard format
 *    and hands over to Samsung's Theme Park, the only component on a Galaxy device that applies an
 *    icon theme across the home screen and app drawer. Samsung performs the final step; DarkFrame
 *    never claims to have performed it.
 * 3. **Pinned shortcut** — [ShortcutManagerCompat.requestPinShortcut] places a home-screen shortcut
 *    carrying our bitmap. Used only where nothing better exists (Pixel Launcher), and never on
 *    Samsung: testing on a Galaxy Z Fold8 showed it adds a second icon beside the original rather
 *    than replacing it, which is a worse product than doing nothing.
 * 4. **Export** — write the rendered PNG out so the user can feed it to whatever their launcher or
 *    theme engine does support.
 */
class IconApplyService(private val context: Context) {

    /** The user's current home launcher, with what DarkFrame can do on it. */
    fun currentLauncher(): LauncherProfile {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolved = runCatching {
            context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        }.getOrNull()
        val packageName = resolved?.activityInfo?.packageName
        val label = runCatching { resolved?.loadLabel(context.packageManager)?.toString() }
            .getOrNull()
            .orEmpty()
        return LauncherCapabilityTable.resolve(packageName, label)
    }

    /**
     * The capability actually available right now.
     *
     * The table's answer is narrowed by a runtime probe, never widened by optimism: a launcher
     * listed as [ApplyCapability.PINNED_SHORTCUT] that turns out not to support pinning is
     * downgraded to export, and an unknown launcher is upgraded to pinning only once the platform
     * has confirmed it supports it.
     */
    fun effectiveCapability(profile: LauncherProfile = currentLauncher()): ApplyCapability {
        val canPin = runCatching {
            ShortcutManagerCompat.isRequestPinShortcutSupported(context)
        }.getOrDefault(false)

        return when (profile.capability) {
            ApplyCapability.SAMSUNG_THEME_PARK -> ApplyCapability.SAMSUNG_THEME_PARK
            ApplyCapability.ICON_PACK_NATIVE -> ApplyCapability.ICON_PACK_NATIVE
            ApplyCapability.PER_ICON_PICKER -> ApplyCapability.PER_ICON_PICKER
            ApplyCapability.PINNED_SHORTCUT ->
                if (canPin) ApplyCapability.PINNED_SHORTCUT else ApplyCapability.EXPORT_ONLY
            // An unrecognised launcher is upgraded to pinning only once the platform confirms it,
            // and never on Samsung, where pinning produces duplicate icons.
            ApplyCapability.EXPORT_ONLY ->
                if (canPin) ApplyCapability.PINNED_SHORTCUT else ApplyCapability.EXPORT_ONLY
        }
    }

    // ---- Samsung One UI --------------------------------------------------------------------

    /** Whether this device is running Samsung's own home launcher right now. */
    fun isSamsungHome(): Boolean =
        SamsungThemeSupport.isOneUiHome(currentLauncher().packageName)

    private fun isInstalled(packageName: String): Boolean = runCatching {
        context.packageManager.getLaunchIntentForPackage(packageName) != null
    }.getOrDefault(false)

    /**
     * Which Samsung step the user is on.
     *
     * Every value maps to an intent [samsungIntentFor] can actually fire, so the UI cannot offer a
     * button with nothing behind it.
     */
    fun samsungStep(): SamsungApplyStep = SamsungThemeSupport.stepFor(
        themeParkInstalled = isInstalled(SamsungThemeSupport.THEME_PARK),
        goodLockInstalled = isInstalled(SamsungThemeSupport.GOOD_LOCK) ||
            isInstalled(SamsungThemeSupport.GOOD_LOCK_LEGACY),
    )

    /**
     * The intent for [step], or null when nothing on this device can serve it.
     *
     * Returning null rather than a best guess matters: the caller disables the action instead of
     * firing an intent that lands the user on an error screen.
     */
    fun samsungIntentFor(step: SamsungApplyStep): Intent? = when (step) {
        SamsungApplyStep.OPEN_THEME_PARK ->
            context.packageManager.getLaunchIntentForPackage(SamsungThemeSupport.THEME_PARK)

        SamsungApplyStep.OPEN_GOOD_LOCK ->
            context.packageManager.getLaunchIntentForPackage(SamsungThemeSupport.GOOD_LOCK)
                ?: context.packageManager.getLaunchIntentForPackage(SamsungThemeSupport.GOOD_LOCK_LEGACY)

        SamsungApplyStep.INSTALL_GOOD_LOCK -> storeIntent(SamsungThemeSupport.GOOD_LOCK)
    }

    /** Galaxy Store first, Play Store second — Good Lock is a Galaxy Store product. */
    private fun storeIntent(packageName: String): Intent? {
        val galaxy = Intent(Intent.ACTION_VIEW, Uri.parse(SamsungThemeSupport.galaxyStoreUri(packageName)))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (galaxy.resolveActivity(context.packageManager) != null) return galaxy
        val play = Intent(Intent.ACTION_VIEW, Uri.parse(SamsungThemeSupport.playStoreUri(packageName)))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return if (play.resolveActivity(context.packageManager) != null) play else null
    }

    /**
     * Asks the launcher to pin a shortcut that opens [identity] and shows [icon].
     *
     * Returns [ApplyOutcome.Requested], not "applied": the platform hands the request to the
     * launcher, which normally shows a confirmation the user can dismiss. There is no callback that
     * reliably reports whether the icon ended up on the home screen, so DarkFrame does not claim it
     * did.
     */
    fun pinThemedShortcut(identity: AppIdentity, icon: Bitmap): ApplyOutcome {
        if (!runCatching { ShortcutManagerCompat.isRequestPinShortcutSupported(context) }
                .getOrDefault(false)
        ) {
            return ApplyOutcome.NotSupported(
                "This launcher does not accept pinned shortcuts. You can still export the icon " +
                    "and apply it with whatever your launcher supports.",
            )
        }

        val launchIntent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(ComponentName(identity.packageName, identity.activityName))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)

        return runCatching {
            val shortcut = ShortcutInfoCompat.Builder(context, "df:${identity.componentKey}")
                .setShortLabel(identity.label.take(MAX_SHORT_LABEL))
                .setLongLabel(identity.label)
                // createWithBitmap, not createWithAdaptiveBitmap: our bitmap is a finished icon
                // with its own corner geometry, not an adaptive foreground, and handing it over as
                // one would make the launcher crop a quarter of it away as mask bleed.
                .setIcon(IconCompat.createWithBitmap(icon))
                .setIntent(launchIntent)
                .build()
            ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
            ApplyOutcome.Requested
        }.getOrElse { error ->
            ApplyOutcome.Failed(error.message ?: "The launcher refused the shortcut request.")
        }
    }

    /**
     * Writes a themed icon to shared cache storage and returns a shareable content URI.
     *
     * Exported at the full 512px design grid regardless of the size on screen, because an export
     * is destined for a theme engine or a launcher's own picker rather than for our grid.
     */
    @WorkerThread
    fun exportIcon(identity: AppIdentity, style: IconStyle, icon: Bitmap): Uri? = runCatching {
        val directory = File(context.cacheDir, EXPORT_DIRECTORY).apply { mkdirs() }
        val file = File(directory, exportFileName(identity, style))
        file.outputStream().use { stream -> icon.compress(Bitmap.CompressFormat.PNG, 100, stream) }
        FileProvider.getUriForFile(context, "${context.packageName}.exports", file)
    }.getOrNull()

    /** Share sheet for an exported icon, so the user can save it wherever they need it. */
    fun shareIntent(uri: Uri): Intent = Intent(Intent.ACTION_SEND)
        .setType("image/png")
        .putExtra(Intent.EXTRA_STREAM, uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

    /** Clears previously exported files. They are disposable once the user has saved them. */
    @WorkerThread
    fun clearExports() {
        runCatching { File(context.cacheDir, EXPORT_DIRECTORY).deleteRecursively() }
    }

    private fun exportFileName(identity: AppIdentity, style: IconStyle): String {
        val safeComponent = identity.componentKey.replace(Regex("[^A-Za-z0-9._-]"), "_")
        return "darkframe_${style.id}_$safeComponent.png"
    }

    private companion object {
        const val EXPORT_DIRECTORY = "exports"

        /** Launchers truncate well before this; the platform's own guidance is ~10 characters. */
        const val MAX_SHORT_LABEL = 24
    }
}
