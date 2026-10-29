package com.aminopy.avatek.data

import android.content.ContentResolver
import android.content.ContentUris
import android.provider.MediaStore

class AudioLibraryRepository(private val contentResolver: ContentResolver) {

    fun loadTracks(): List<AudioTrack> {
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        return runCatching {
            contentResolver.query(collection, projection, selection, null, sortOrder)?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                buildList {
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idColumn)
                        add(
                            AudioTrack(
                                id = id,
                                title = cursor.getString(titleColumn).orEmpty().ifBlank { "Unknown title" },
                                artist = cursor.getString(artistColumn).orEmpty().ifBlank { "Unknown artist" },
                                album = cursor.getString(albumColumn).orEmpty().ifBlank { "Unknown album" },
                                durationMs = cursor.getLong(durationColumn),
                                uri = ContentUris.withAppendedId(collection, id)
                            )
                        )
                    }
                }
            }.orEmpty()
        }.getOrDefault(emptyList())
    }
}
