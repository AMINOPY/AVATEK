package com.aminopy.avatek.data

import android.net.Uri

data class AudioTrack(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val uri: Uri,
    val albumId: Long = 0L,
    val dateAddedSec: Long = 0L
) {
    /** Stable key used to group tracks into albums (album + artist). */
    val albumKey: String get() = "$album\u0000$artist"
}

data class AlbumInfo(
    val key: String,
    val title: String,
    val artist: String,
    val trackIds: List<Long>,
    val durationMs: Long
)

data class ArtistInfo(
    val name: String,
    val trackIds: List<Long>,
    val albumCount: Int
)
