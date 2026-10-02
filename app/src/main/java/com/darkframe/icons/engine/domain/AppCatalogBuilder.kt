package com.darkframe.icons.engine.domain

/** A launcher activity exactly as the platform reported it, before DarkFrame's own rules apply. */
data class RawLauncherEntry(
    val packageName: String,
    val activityName: String,
    val label: String,
    val versionStamp: Long,
    val isWorkProfile: Boolean = false,
)

/**
 * Turns the platform's raw list of launcher activities into the catalog DarkFrame shows.
 *
 * The platform list is messy in ways that matter:
 *  - DarkFrame itself is in it, and theming our own icon is noise.
 *  - Some packages publish several launcher activities. Those are real, separately themeable
 *    entries and must be kept, but only the first one keeps the bare label; the rest are marked
 *    as secondary so the UI can disambiguate instead of showing duplicate-looking rows.
 *  - Duplicate `(package, activity)` pairs show up across profiles and after partial updates.
 *  - Blank labels happen; falling back to the package name keeps the row usable and searchable.
 *
 * Pure function so all of that is pinned down by unit tests rather than discovered on a device.
 */
object AppCatalogBuilder {

    fun build(entries: List<RawLauncherEntry>, selfPackage: String): List<AppIdentity> {
        val seen = HashSet<String>()
        val deduped = ArrayList<RawLauncherEntry>(entries.size)
        for (entry in entries) {
            if (entry.packageName == selfPackage) continue
            if (entry.packageName.isBlank() || entry.activityName.isBlank()) continue
            if (!seen.add("${entry.packageName}/${entry.activityName}|${entry.isWorkProfile}")) continue
            deduped += entry
        }

        val launcherCountByPackage = deduped.groupingBy { it.packageName }.eachCount()
        val primaryClaimed = HashSet<String>()

        return deduped
            .map { entry ->
                val label = entry.label.ifBlank { entry.packageName }
                val multi = (launcherCountByPackage[entry.packageName] ?: 1) > 1
                AppIdentity(
                    packageName = entry.packageName,
                    activityName = entry.activityName,
                    label = label,
                    versionStamp = entry.versionStamp,
                    isSecondaryEntryPoint = multi,
                    isWorkProfile = entry.isWorkProfile,
                )
            }
            // Sorted before primary/secondary is assigned, so "primary" is the alphabetically
            // first activity rather than whatever order the platform happened to return —
            // otherwise a row's label changes between launches, which reads as a bug.
            .sortedWith(
                compareBy(
                    { it.label.lowercase() },
                    { it.packageName },
                    { it.activityName },
                )
            )
            .map { identity ->
                if (!identity.isSecondaryEntryPoint) return@map identity
                if (primaryClaimed.add(identity.packageName)) {
                    identity.copy(isSecondaryEntryPoint = false)
                } else {
                    identity
                }
            }
    }

    /**
     * Case- and diacritic-insensitive filter over labels and package names. Matching the package
     * name too is deliberate: it is how a user finds an app whose display label does not contain
     * the word they remember.
     */
    fun filter(catalog: List<AppIdentity>, query: String): List<AppIdentity> {
        val needle = query.trim().lowercase()
        if (needle.isEmpty()) return catalog
        return catalog.filter {
            it.label.lowercase().contains(needle) || it.packageName.lowercase().contains(needle)
        }
    }
}
