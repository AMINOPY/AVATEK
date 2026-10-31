package com.aminopy.avatek.state

import com.aminopy.avatek.data.AudioTrack

enum class MusicMood(val title: String, val hint: String) {
    ENERGY("انرژی", "ریتم و حال‌وهوای پرتحرک"),
    CALM("آرامش", "فضای ملایم و کم‌تنش"),
    FOCUS("تمرکز", "موسیقی کم‌حواس‌پرتی"),
    NOSTALGIA("نوستالژی", "بازگشت به آهنگ‌های آشنا"),
    DISCOVERY("کشف", "آهنگ‌های کمتر شنیده‌شده")
}

data class TasteSnapshot(
    val totalPlays: Int,
    val uniqueTracks: Int,
    val favoriteCount: Int,
    val topArtists: List<Pair<String, Int>>,
    val topGenres: List<Pair<String, Int>>
)

/** All recommendations are computed locally from MediaStore metadata and local playback history. */
object MusicIntelligence {
    fun snapshot(
        tracks: List<AudioTrack>,
        favorites: Set<Long>,
        playCounts: Map<Long, Int>
    ): TasteSnapshot {
        val topArtists = tracks.groupBy { it.artist.trim().ifBlank { "هنرمند نامشخص" } }
            .map { (name, items) -> name to items.sumOf { playCounts[it.id] ?: 0 } }
            .filter { it.second > 0 }.sortedByDescending { it.second }.take(5)
        val topGenres = tracks.groupBy { it.genre.trim().ifBlank { "نامشخص" } }
            .map { (name, items) -> name to items.sumOf { playCounts[it.id] ?: 0 } }
            .filter { it.second > 0 && it.first != "نامشخص" }
            .sortedByDescending { it.second }.take(5)
        return TasteSnapshot(
            totalPlays = playCounts.values.sum(),
            uniqueTracks = playCounts.count { it.value > 0 },
            favoriteCount = favorites.size,
            topArtists = topArtists,
            topGenres = topGenres
        )
    }

    /** Greedy DJ sequencing balances taste score with artist/album variety and recent-play avoidance. */
    fun smartDj(
        tracks: List<AudioTrack>,
        favorites: Set<Long>,
        playCounts: Map<Long, Int>,
        recentIds: List<Long>,
        limit: Int = 40
    ): List<AudioTrack> {
        val remaining = tracks.distinctBy { it.id }.toMutableList()
        val recent = recentIds.take(12).toSet()
        val result = mutableListOf<AudioTrack>()
        var previousArtist = ""
        var previousAlbum = ""
        while (remaining.isNotEmpty() && result.size < limit.coerceIn(1, 200)) {
            val next = remaining.maxByOrNull { track ->
                val plays = playCounts[track.id] ?: 0
                (if (track.id in favorites) 35.0 else 0.0) +
                    (plays.coerceAtMost(25) * 1.4) +
                    (if (plays == 0) 8.0 else 0.0) +
                    (if (track.id in recent) -32.0 else 0.0) +
                    (if (track.artist.equals(previousArtist, true)) -24.0 else 10.0) +
                    (if (track.albumKey == previousAlbum) -12.0 else 4.0) +
                    stableTieBreak(track.id)
            } ?: break
            result += next
            remaining.remove(next)
            previousArtist = next.artist
            previousAlbum = next.albumKey
        }
        return result
    }

    /** Mood matching is deliberately metadata-based; it does not claim to infer audio emotion. */
    fun moodMix(
        mood: MusicMood,
        tracks: List<AudioTrack>,
        favorites: Set<Long>,
        playCounts: Map<Long, Int>,
        recentIds: List<Long>,
        limit: Int = 40
    ): List<AudioTrack> {
        val recent = recentIds.take(10).toSet()
        return tracks.distinctBy { it.id }.sortedByDescending { track ->
            val text = (track.title + " " + track.album + " " + track.genre).lowercase()
            val genre = track.genre.lowercase()
            val keywords = when (mood) {
                MusicMood.ENERGY -> listOf("rock", "metal", "dance", "electronic", "hip", "رپ", "راک", "انرژی")
                MusicMood.CALM -> listOf("ambient", "acoustic", "classical", "jazz", "آرام", "ملایم", "پیانو")
                MusicMood.FOCUS -> listOf("instrumental", "classical", "ambient", "lofi", "بی‌کلام", "بی کلام")
                MusicMood.NOSTALGIA -> listOf("old", "classic", "nostalgia", "قدیمی", "خاطره", "نوستالژی")
                MusicMood.DISCOVERY -> emptyList()
            }
            val match = if (mood == MusicMood.DISCOVERY) 0.0 else keywords.count { text.contains(it) || genre.contains(it) } * 12.0
            val plays = playCounts[track.id] ?: 0
            val taste = if (track.id in favorites) 10.0 else 0.0
            val discovery = if (mood == MusicMood.DISCOVERY) (if (plays == 0) 26.0 else -plays.coerceAtMost(20).toDouble()) else 0.0
            match + taste + discovery - (if (track.id in recent) 18.0 else 0.0) + stableTieBreak(track.id)
        }.take(limit.coerceIn(1, 200))
    }

    private fun stableTieBreak(id: Long): Double = ((id xor (id ushr 17)) and 0xFF).toDouble() / 255.0
}
