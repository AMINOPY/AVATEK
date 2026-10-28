package com.aminopy.avatek

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackFormatTest {
    @Test
    fun formatsMinutesAndSeconds() {
        assertEquals("0:00", formatDuration(0L))
        assertEquals("0:09", formatDuration(9_000L))
        assertEquals("1:05", formatDuration(65_000L))
        assertEquals("61:01", formatDuration(3_661_000L))
    }

    @Test
    fun negativeDurationIsClampedToZero() {
        assertEquals("0:00", formatDuration(-1L))
    }
}
