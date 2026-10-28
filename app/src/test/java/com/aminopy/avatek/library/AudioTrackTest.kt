package com.aminopy.avatek.library

import android.net.Uri
import com.aminopy.avatek.data.AudioTrack
import org.junit.Assert.assertEquals
import org.junit.Test

class AudioTrackTest {
    @Test
    fun audioTrackKeepsDisplayMetadataAndUri() {
        val uri = Uri.parse("content://media/external/audio/media/42")
        val track = AudioTrack(42L, "Night Drive", "AVATEK Test Artist", "First Light", 180_000L, uri)

        assertEquals(42L, track.id)
        assertEquals("Night Drive", track.title)
        assertEquals("AVATEK Test Artist", track.artist)
        assertEquals("First Light", track.album)
        assertEquals(180_000L, track.durationMs)
        assertEquals(uri, track.uri)
    }
}
