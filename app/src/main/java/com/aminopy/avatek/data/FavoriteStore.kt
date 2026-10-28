package com.aminopy.avatek.data

import android.content.Context

class FavoriteStore(context: Context) {
    private val preferences = context.getSharedPreferences("avatek_library", Context.MODE_PRIVATE)

    fun loadFavoriteIds(): Set<Long> = preferences.getStringSet(KEY_FAVORITES, emptySet())
        .orEmpty()
        .mapNotNull { it.toLongOrNull() }
        .toSet()

    fun saveFavoriteIds(ids: Set<Long>) {
        preferences.edit().putStringSet(KEY_FAVORITES, ids.map(Long::toString).toSet()).apply()
    }

    companion object {
        private const val KEY_FAVORITES = "favorite_track_ids"
    }
}
