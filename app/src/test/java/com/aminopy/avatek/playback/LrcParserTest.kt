package com.aminopy.avatek.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class LrcParserTest {
    @Test
    fun parsesHundredthsAndMultipleTimestamps() {
        val result = LrcParser.parse("[00:01.25][00:03.50]Hello\n[01:02.00]World")
        assertEquals(
            listOf(
                LyricLine(1_250L, "Hello"),
                LyricLine(3_500L, "Hello"),
                LyricLine(62_000L, "World")
            ),
            result
        )
    }

    @Test
    fun appliesGlobalOffsetAndClampsNegativeTimes() {
        val result = LrcParser.parse("[offset:-1500]\n[00:01.00]Before\n[00:02.00]After")
        assertEquals(listOf(LyricLine(0L, "Before"), LyricLine(500L, "After")), result)
    }

    @Test
    fun ignoresMetadataAndInvalidTimestampLines() {
        val result = LrcParser.parse("[ar:Artist]\n[00:75.00]Bad\n[00:04.2]Good")
        assertEquals(listOf(LyricLine(4_200L, "Good")), result)
    }
}
