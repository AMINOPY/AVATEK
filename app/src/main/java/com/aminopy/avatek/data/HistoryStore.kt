package com.aminopy.avatek.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Local-only listening history: play counts, recently played ids and recent searches. */
class HistoryStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("avatek_history", Context.MODE_PRIVATE)

    @Synchronized
    fun loadPlayCounts(): Map<Long, Int> = runCatching {
        val root = JSONObject(prefs.getString("counts", "{}") ?: "{}")
        root.keys().asSequence().mapNotNull { k -> k.toLongOrNull()?.let { it to root.optInt(k) } }.toMap()
    }.getOrDefault(emptyMap())

    @Synchronized
    fun savePlayCounts(counts: Map<Long, Int>) {
        val root = JSONObject()
        counts.forEach { (id, n) -> root.put(id.toString(), n) }
        prefs.edit().putString("counts", root.toString()).apply()
    }

    @Synchronized
    fun loadRecent(): List<Long> = runCatching {
        val arr = JSONArray(prefs.getString("recent", "[]") ?: "[]")
        (0 until arr.length()).map { arr.getLong(it) }
    }.getOrDefault(emptyList())

    @Synchronized
    fun saveRecent(ids: List<Long>) {
        prefs.edit().putString("recent", JSONArray(ids.take(MAX_RECENT)).toString()).apply()
    }

    @Synchronized
    fun loadSearches(): List<String> = runCatching {
        val arr = JSONArray(prefs.getString("searches", "[]") ?: "[]")
        (0 until arr.length()).map { arr.getString(it) }
    }.getOrDefault(emptyList())

    @Synchronized
    fun saveSearches(items: List<String>) {
        prefs.edit().putString("searches", JSONArray(items.take(MAX_SEARCHES)).toString()).apply()
    }

    companion object {
        const val MAX_RECENT = 40
        const val MAX_SEARCHES = 8
    }
}
