package com.darkframe.icons.data

import android.content.Context

/** The kinds of thing a user can save. */
enum class FavoriteKind(val key: String) {
    LOOK("look"),
    WALLPAPER("wallpaper"),
    APP("app"),
}

/**
 * What the user has saved.
 *
 * One store for all three kinds, so Favorites is a single useful screen rather than three partial
 * ones. Ids are opaque strings owned by whichever catalog produced them.
 */
class FavoritesStore(context: Context) {

    private val prefs = context.getSharedPreferences("darkframe_favorites", Context.MODE_PRIVATE)

    fun isFavorite(kind: FavoriteKind, id: String): Boolean =
        prefs.getBoolean(entry(kind, id), false)

    fun setFavorite(kind: FavoriteKind, id: String, value: Boolean) {
        prefs.edit().apply {
            if (value) putBoolean(entry(kind, id), true) else remove(entry(kind, id))
        }.apply()
    }

    fun toggle(kind: FavoriteKind, id: String): Boolean {
        val next = !isFavorite(kind, id)
        setFavorite(kind, id, next)
        return next
    }

    /** Saved ids of one kind. Order is not meaningful; callers sort against their own catalog. */
    fun idsOf(kind: FavoriteKind): Set<String> {
        val prefix = "${kind.key}:"
        return prefs.all.keys
            .filter { it.startsWith(prefix) && prefs.getBoolean(it, false) }
            .map { it.removePrefix(prefix) }
            .toSet()
    }

    fun totalCount(): Int = FavoriteKind.entries.sumOf { idsOf(it).size }

    private fun entry(kind: FavoriteKind, id: String) = "${kind.key}:$id"
}
