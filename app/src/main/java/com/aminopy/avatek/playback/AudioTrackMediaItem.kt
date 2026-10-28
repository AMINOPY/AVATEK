package com.aminopy.avatek.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.aminopy.avatek.data.AudioTrack

fun AudioTrack.toMediaItem(): MediaItem = MediaItem.Builder()
    .setMediaId(id.toString())
    .setUri(uri)
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .build()
    )
    .build()
