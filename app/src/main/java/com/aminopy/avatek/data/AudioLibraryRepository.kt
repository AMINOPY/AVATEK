package com.aminopy.avatek.data

import android.content.ContentResolver
import android.content.ContentUris
import android.os.Build
import android.provider.MediaStore

class AudioLibraryRepository(private val contentResolver: ContentResolver) {

    fun loadTracks(minDurationMs: Long = 0L): List<AudioTrack> {
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val folderColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.MediaColumns.RELATIVE_PATH
        } else {
            MediaStore.Audio.Media.DATA
        }
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
            folderColumn
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= ?"
        val selectionArgs = arrayOf(minDurationMs.coerceAtLeast(0L).toString())
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        return runCatching {
            val tracks = contentResolver.query(collection, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val addedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val folderIndex = cursor.getColumnIndex(folderColumn)
                buildList {
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idColumn)
                        val artist = cursor.getString(artistColumn).orEmpty()
                            .let { if (it.isBlank() || it == "<unknown>") UNKNOWN_ARTIST else it }
                        val album = cursor.getString(albumColumn).orEmpty()
                            .let { if (it.isBlank() || it == "<unknown>") UNKNOWN_ALBUM else it }
                        val rawFolder = if (folderIndex >= 0) cursor.getString(folderIndex).orEmpty() else ""
                        val folder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            rawFolder.trim('/').ifBlank { "Music" }
                        } else {
                            rawFolder.substringBeforeLast('/', "").substringAfterLast('/').ifBlank { "Music" }
                        }
                        add(
                            AudioTrack(
                                id = id,
                                title = cursor.getString(titleColumn).orEmpty().ifBlank { "Unknown title" },
                                artist = artist,
                                album = album,
                                durationMs = cursor.getLong(durationColumn),
                                uri = ContentUris.withAppendedId(collection, id),
                                albumId = cursor.getLong(albumIdColumn),
                                dateAddedSec = cursor.getLong(addedColumn),
                                folderPath = folder
                            )
                        )
                    }
                }
            }.orEmpty()
            val genres = loadGenreMap()
            tracks.map { track -> track.copy(genre = genres[track.id] ?: "نامشخص") }
        }.getOrDefault(emptyList())
    }

    /** Reads the platform's local genre index; unavailable providers simply yield unknown genres. */
    private fun loadGenreMap(): Map<Long, String> = runCatching {
        val result = mutableMapOf<Long, String>()
        contentResolver.query(
            MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Audio.Genres._ID, MediaStore.Audio.Genres.NAME),
            null, null, null
        )?.use { genres ->
            val idColumn = genres.getColumnIndexOrThrow(MediaStore.Audio.Genres._ID)
            val nameColumn = genres.getColumnIndexOrThrow(MediaStore.Audio.Genres.NAME)
            while (genres.moveToNext()) {
                val genreId = genres.getLong(idColumn)
                val name = genres.getString(nameColumn).orEmpty().ifBlank { "نامشخص" }
                val membersUri = MediaStore.Audio.Genres.Members.getContentUri("external", genreId)
                contentResolver.query(
                    membersUri,
                    arrayOf(MediaStore.Audio.Media._ID),
                    null, null, null
                )?.use { members ->
                    val memberId = members.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    while (members.moveToNext()) result.putIfAbsent(members.getLong(memberId), name)
                }
            }
        }
        result
    }.getOrDefault(emptyMap())

    companion object {
        const val UNKNOWN_ARTIST = "Unknown artist"
        const val UNKNOWN_ALBUM = "Unknown album"
    }
}
