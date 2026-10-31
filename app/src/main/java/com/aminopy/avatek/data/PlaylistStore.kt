package com.aminopy.avatek.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class AudioPlaylist(
    val id: String,
    val name: String,
    val trackIds: List<Long>
)

/**
 * Device-local playlist persistence. Playlist membership is stored by MediaStore ID,
 * so no audio files are copied and playlists remain fully offline.
 */
class PlaylistStore(context: Context) {
    private val preferences = context.applicationContext
        .getSharedPreferences("avatek_playlists", Context.MODE_PRIVATE)

    @Synchronized
    fun loadPlaylists(): List<AudioPlaylist> =
        runCatching {
            val root = JSONObject(preferences.getString(KEY_PLAYLISTS, "{}") ?: "{}")
            root.keys().asSequence().mapNotNull { id ->
                val item = root.optJSONObject(id) ?: return@mapNotNull null
                val name = item.optString("name").trim()
                if (name.isBlank()) return@mapNotNull null
                val ids = item.optJSONArray("tracks") ?: JSONArray()
                AudioPlaylist(
                    id = id,
                    name = name,
                    trackIds = (0 until ids.length()).mapNotNull { index ->
                        ids.optLong(index).takeIf { it > 0L }
                    }.distinct()
                )
            }.sortedBy { it.name.lowercase() }.toList()
        }.getOrDefault(emptyList())

    @Synchronized
    fun createPlaylist(name: String): AudioPlaylist? {
        val cleanName = name.trim().replace(Regex("\\s+"), " ")
        if (cleanName.isBlank()) return null
        val existing = loadPlaylists()
        if (existing.any { it.name.equals(cleanName, ignoreCase = true) }) return null
        val playlist = AudioPlaylist(
            id = java.util.UUID.randomUUID().toString(),
            name = cleanName,
            trackIds = emptyList()
        )
        persist(existing + playlist)
        return playlist
    }

    @Synchronized
    fun addTrack(playlistId: String, trackId: Long): Boolean {
        if (trackId <= 0L) return false
        val playlists = loadPlaylists()
        val target = playlists.firstOrNull { it.id == playlistId } ?: return false
        if (trackId in target.trackIds) return false
        persist(playlists.map {
            if (it.id == playlistId) it.copy(trackIds = it.trackIds + trackId) else it
        })
        return true
    }

    @Synchronized
    fun removeTrack(playlistId: String, trackId: Long): Boolean {
        val playlists = loadPlaylists()
        val target = playlists.firstOrNull { it.id == playlistId } ?: return false
        if (trackId !in target.trackIds) return false
        persist(playlists.map {
            if (it.id == playlistId) it.copy(trackIds = it.trackIds - trackId) else it
        })
        return true
    }

    @Synchronized
    fun renamePlaylist(playlistId: String, name: String): Boolean {
        val cleanName = name.trim().replace(Regex("\\s+"), " ")
        if (cleanName.isBlank()) return false
        val playlists = loadPlaylists()
        if (playlists.none { it.id == playlistId }) return false
        if (playlists.any { it.id != playlistId && it.name.equals(cleanName, ignoreCase = true) }) return false
        persist(playlists.map { if (it.id == playlistId) it.copy(name = cleanName) else it })
        return true
    }

    @Synchronized
    fun deletePlaylist(playlistId: String): Boolean {
        val playlists = loadPlaylists()
        if (playlists.none { it.id == playlistId }) return false
        persist(playlists.filterNot { it.id == playlistId })
        return true
    }

    private fun persist(playlists: List<AudioPlaylist>) {
        val root = JSONObject()
        playlists.forEach { playlist ->
            root.put(
                playlist.id,
                JSONObject()
                    .put("name", playlist.name)
                    .put("tracks", JSONArray(playlist.trackIds))
            )
        }
        preferences.edit().putString(KEY_PLAYLISTS, root.toString()).apply()
    }

    companion object {
        private const val KEY_PLAYLISTS = "playlists_v1"
    }
}
