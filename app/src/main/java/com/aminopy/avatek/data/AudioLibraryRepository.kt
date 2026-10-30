package com.aminopy.avatek.data

import android.content.ContentResolver
import android.content.ContentUris
import android.provider.MediaStore

class AudioLibraryRepository(private val contentResolver: ContentResolver) {

    fun loadTracks(minDurationMs: Long = 0L): List<AudioTrack> {
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= ?"
        val selectionArgs = arrayOf(minDurationMs.coerceAtLeast(0L).toString())
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        return runCatching {
            contentResolver.query(collection, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val addedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                buildList {
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idColumn)
                        val artist = cursor.getString(artistColumn).orEmpty()
                            .let { if (it.isBlank() || it == "<unknown>") UNKNOWN_ARTIST else it }
                        val album = cursor.getString(albumColumn).orEmpty()
                            .let { if (it.isBlank() || it == "<unknown>") UNKNOWN_ALBUM else it }
                        add(
                            AudioTrack(
                                id = id,
                                title = cursor.getString(titleColumn).orEmpty().ifBlank { "Unknown title" },
                                artist = artist,
                                album = album,
                                durationMs = cursor.getLong(durationColumn),
                                uri = ContentUris.withAppendedId(collection, id),
                                albumId = cursor.getLong(albumIdColumn),
                                dateAddedSec = cursor.getLong(addedColumn)
                            )
                        )
                    }
                }
            }.orEmpty()
        }.getOrDefault(emptyList())
    }

    companion object {
        const val UNKNOWN_ARTIST = "Unknown artist"
        const val UNKNOWN_ALBUM = "Unknown album"
    }
}
