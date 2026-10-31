package com.aminopy.avatek.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Last playback session, stored locally so the queue can be restored after process death. */
data class QueueSnapshot(
    val trackIds: List<Long>,
    val currentIndex: Int,
    val positionMs: Long,
    val repeatMode: Int,
    val shuffle: Boolean,
    val speed: Float,
    val playWhenReady: Boolean
)

class QueueStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("avatek_queue", Context.MODE_PRIVATE)

    @Synchronized
    fun load(): QueueSnapshot? = runCatching {
        val root = JSONObject(prefs.getString(KEY, null) ?: return null)
        val ids = root.optJSONArray("ids") ?: JSONArray()
        val trackIds = (0 until ids.length()).mapNotNull { ids.optLong(it).takeIf { id -> id > 0L } }
        if (trackIds.isEmpty()) return null
        QueueSnapshot(
            trackIds = trackIds,
            currentIndex = root.optInt("index", 0).coerceIn(0, trackIds.lastIndex),
            positionMs = root.optLong("position", 0L).coerceAtLeast(0L),
            repeatMode = root.optInt("repeat", 0).coerceIn(0, 2),
            shuffle = root.optBoolean("shuffle", false),
            speed = root.optDouble("speed", 1.0).toFloat().coerceIn(0.5f, 2f),
            playWhenReady = root.optBoolean("playing", false)
        )
    }.getOrNull()

    @Synchronized
    fun save(snapshot: QueueSnapshot) {
        if (snapshot.trackIds.isEmpty()) {
            clear()
            return
        }
        val root = JSONObject()
            .put("ids", JSONArray(snapshot.trackIds))
            .put("index", snapshot.currentIndex.coerceIn(0, snapshot.trackIds.lastIndex))
            .put("position", snapshot.positionMs.coerceAtLeast(0L))
            .put("repeat", snapshot.repeatMode.coerceIn(0, 2))
            .put("shuffle", snapshot.shuffle)
            .put("speed", snapshot.speed.coerceIn(0.5f, 2f).toDouble())
            .put("playing", snapshot.playWhenReady)
        prefs.edit().putString(KEY, root.toString()).apply()
    }

    @Synchronized
    fun clear() {
        prefs.edit().remove(KEY).apply()
    }

    private companion object {
        const val KEY = "last_session_v1"
    }
}
