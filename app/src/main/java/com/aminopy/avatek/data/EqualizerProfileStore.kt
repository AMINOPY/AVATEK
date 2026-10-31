package com.aminopy.avatek.data

import android.content.Context

/** User-created equalizer curves, stored locally and scoped to this app installation. */
class EqualizerProfileStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("avatek_eq_profiles", Context.MODE_PRIVATE)

    fun names(): List<String> = prefs.getStringSet(KEY_NAMES, emptySet()).orEmpty().toList().sorted()

    fun save(name: String, levels: List<Int>): Boolean {
        val safe = name.trim().take(32)
        if (safe.isBlank() || levels.isEmpty() || levels.size > 32) return false
        val names = prefs.getStringSet(KEY_NAMES, emptySet()).orEmpty().toMutableSet()
        names += safe
        prefs.edit()
            .putStringSet(KEY_NAMES, names)
            .putString("profile:$safe", levels.joinToString(","))
            .apply()
        return true
    }

    fun load(name: String): List<Int>? = prefs.getString("profile:$name", null)
        ?.split(',')?.mapNotNull(String::toIntOrNull)?.takeIf { it.isNotEmpty() }

    fun delete(name: String) {
        val names = prefs.getStringSet(KEY_NAMES, emptySet()).orEmpty().toMutableSet()
        names.remove(name)
        prefs.edit().putStringSet(KEY_NAMES, names).remove("profile:$name").apply()
    }

    companion object { private const val KEY_NAMES = "names" }
}
