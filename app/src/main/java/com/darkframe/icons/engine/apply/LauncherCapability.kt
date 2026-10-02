package com.darkframe.icons.engine.apply

/**
 * What DarkFrame can actually do on a given home launcher.
 *
 * Android has no API that lets an app replace another app's launcher icon system-wide. There is
 * no permission for it, no AppOp, and no vendor hook that a Play-distributed app may use. Anything
 * that claims otherwise is either a launcher doing it for itself, a root/Xposed module, or a lie.
 *
 * So DarkFrame models capability per launcher and never offers an action it cannot perform.
 */
enum class ApplyCapability {
    /**
     * The launcher reads icon packs. DarkFrame ships a standard `appfilter.xml`, and the user
     * selects "DarkFrame" in that launcher's icon-pack setting. The launcher then substitutes
     * icons itself, for every app the pack covers.
     *
     * Note what this does *and does not* mean for the dynamic engine: the appfilter mechanism can
     * only reference drawables that exist in our APK, so it covers curated artwork. Dynamically
     * generated icons are per-device bitmaps and are applied through [PINNED_SHORTCUT] or
     * [EXPORT_ONLY] instead.
     */
    ICON_PACK_NATIVE,

    /**
     * The launcher supports editing a single app's icon, picking an image from the device.
     * DarkFrame exports the themed icon and guides the user through that launcher's own flow.
     */
    PER_ICON_PICKER,

    /**
     * No icon-pack and no per-icon support, but the platform's pin-shortcut API works. DarkFrame
     * can place a *shortcut* carrying the themed icon on the home screen. This is a real,
     * supported mechanism, and it is honest about its limits: the shortcut sits alongside the
     * original app entry, and the app drawer is unchanged.
     */
    PINNED_SHORTCUT,

    /**
     * Nothing can be applied programmatically. DarkFrame exports the rendered icon as a file and
     * explains the manual route (for example a Galaxy Themes icon pack, or switching to a launcher
     * that supports icon packs).
     */
    EXPORT_ONLY,
}

/**
 * A home launcher DarkFrame knows about, and the single best mechanism available on it.
 *
 * [requiresManualStep] records whether the user has to finish the job in another app's UI. It is
 * surfaced in the setup flow so the product never implies a one-tap result it cannot deliver.
 */
data class LauncherProfile(
    val packageName: String,
    val displayName: String,
    val capability: ApplyCapability,
    val requiresManualStep: Boolean,
    /** Short, literal explanation shown in guided setup. No marketing language. */
    val note: String,
)

/**
 * Static knowledge of launcher behaviour, kept as data so it is testable and cheap to correct.
 *
 * Entries reflect each launcher's documented/observable support for third-party icon packs. When a
 * launcher is not listed, [resolve] deliberately returns the conservative fallback rather than
 * guessing: being wrong in the optimistic direction means promising the user something that then
 * silently fails.
 */
object LauncherCapabilityTable {

    private val profiles: List<LauncherProfile> = listOf(
        // ---- Launchers with first-class icon-pack support -------------------------------------
        LauncherProfile(
            "com.teslacoilsw.launcher", "Nova Launcher", ApplyCapability.ICON_PACK_NATIVE,
            requiresManualStep = true,
            note = "Nova Settings → Look & feel → Icon style → Icon pack → DarkFrame.",
        ),
        LauncherProfile(
            "app.lawnchair", "Lawnchair", ApplyCapability.ICON_PACK_NATIVE,
            requiresManualStep = true,
            note = "Lawnchair Settings → General → Icon pack → DarkFrame.",
        ),
        LauncherProfile(
            "ch.deletescape.lawnchair.plah", "Lawnchair (legacy)", ApplyCapability.ICON_PACK_NATIVE,
            requiresManualStep = true,
            note = "Lawnchair Settings → General → Icon pack → DarkFrame.",
        ),
        LauncherProfile(
            "com.microsoft.launcher", "Microsoft Launcher", ApplyCapability.ICON_PACK_NATIVE,
            requiresManualStep = true,
            note = "Launcher settings → Personalization → Icon pack → DarkFrame.",
        ),
        LauncherProfile(
            "ginlemon.flowerfree", "Smart Launcher", ApplyCapability.ICON_PACK_NATIVE,
            requiresManualStep = true,
            note = "Smart Launcher settings → Global appearance → Icon pack → DarkFrame.",
        ),
        LauncherProfile(
            "ginlemon.flowerpro", "Smart Launcher Pro", ApplyCapability.ICON_PACK_NATIVE,
            requiresManualStep = true,
            note = "Smart Launcher settings → Global appearance → Icon pack → DarkFrame.",
        ),
        LauncherProfile(
            "com.actionlauncher.playstore", "Action Launcher", ApplyCapability.ICON_PACK_NATIVE,
            requiresManualStep = true,
            note = "Action Launcher settings → Appearance → Icon pack → DarkFrame.",
        ),
        LauncherProfile(
            "com.anddoes.launcher", "Apex Launcher", ApplyCapability.ICON_PACK_NATIVE,
            requiresManualStep = true,
            note = "Apex settings → Theme settings → Icon pack → DarkFrame.",
        ),
        LauncherProfile(
            "home.solo.launcher.free", "Solo Launcher", ApplyCapability.ICON_PACK_NATIVE,
            requiresManualStep = true,
            note = "Solo settings → Theme → Icon pack → DarkFrame.",
        ),
        LauncherProfile(
            "com.gau.go.launcherex", "GO Launcher", ApplyCapability.ICON_PACK_NATIVE,
            requiresManualStep = true,
            note = "GO Launcher → Theme → Icon pack → DarkFrame.",
        ),

        // ---- Launchers with per-icon editing but no icon-pack support -------------------------
        LauncherProfile(
            "bitpit.launcher", "Niagara Launcher", ApplyCapability.PER_ICON_PICKER,
            requiresManualStep = true,
            note = "Long-press an app → Edit → change its icon, then pick a DarkFrame export. " +
                "Niagara applies icons one at a time; it does not read icon packs.",
        ),

        // ---- Stock launchers without third-party icon theming --------------------------------
        LauncherProfile(
            "com.google.android.apps.nexuslauncher", "Pixel Launcher",
            ApplyCapability.PINNED_SHORTCUT,
            requiresManualStep = false,
            note = "Pixel Launcher does not support third-party icon packs. DarkFrame can pin a " +
                "themed shortcut to your home screen for each app; the app drawer keeps the " +
                "original icons. For a fully themed home screen, use a launcher that supports " +
                "icon packs.",
        ),
        LauncherProfile(
            "com.sec.android.app.launcher", "Samsung One UI Home",
            ApplyCapability.PINNED_SHORTCUT,
            requiresManualStep = false,
            note = "One UI Home themes icons only through Galaxy Themes, which Samsung " +
                "distributes itself — a third-party app cannot set it. DarkFrame can pin themed " +
                "shortcuts to your home screen, and can export icons for manual use.",
        ),
        LauncherProfile(
            "com.android.launcher3", "Launcher3 / AOSP", ApplyCapability.PINNED_SHORTCUT,
            requiresManualStep = false,
            note = "No icon-pack support. DarkFrame can pin themed shortcuts to the home screen.",
        ),
    )

    private val byPackage = profiles.associateBy { it.packageName }

    /**
     * Conservative fallback for an unrecognised launcher.
     *
     * [ApplyCapability.EXPORT_ONLY] rather than a guess: pin-shortcut support is probed at runtime
     * by [ShortcutApplyService], and an unknown launcher gets upgraded only once that probe has
     * actually succeeded.
     */
    fun fallback(packageName: String, displayName: String): LauncherProfile = LauncherProfile(
        packageName = packageName,
        displayName = displayName,
        capability = ApplyCapability.EXPORT_ONLY,
        requiresManualStep = true,
        note = "DarkFrame has not verified icon theming on this launcher. You can export themed " +
            "icons and apply them with whatever your launcher supports.",
    )

    fun resolve(packageName: String?, displayName: String = packageName.orEmpty()): LauncherProfile {
        if (packageName.isNullOrBlank()) return fallback("", "Unknown launcher")
        return byPackage[packageName] ?: fallback(packageName, displayName.ifBlank { packageName })
    }

    fun knownLaunchers(): List<LauncherProfile> = profiles

    /** True when the launcher can consume our shipped `appfilter.xml` directly. */
    fun supportsIconPack(packageName: String?): Boolean =
        resolve(packageName).capability == ApplyCapability.ICON_PACK_NATIVE
}
