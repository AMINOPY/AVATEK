package com.aminopy.avatek.playback

data class LyricLine(val timeMs: Long, val text: String)

/** Parses standard LRC timestamps, multiple timestamps per line and optional global offset. */
object LrcParser {
    private val timestamp = Regex("""\[(\d{1,2}):(\d{2})(?:[.:](\d{1,3}))?]""")
    private val offsetPattern = Regex("""\[offset:([+-]?\d+)]""")

    fun parse(raw: String): List<LyricLine> {
        val offset = offsetPattern.find(raw)?.groupValues?.getOrNull(1)?.toLongOrNull() ?: 0L
        return raw.lineSequence().flatMap { line ->
            val matches = timestamp.findAll(line).toList()
            val text = timestamp.replace(line, "").trim()
            if (matches.isEmpty() || text.isBlank()) emptySequence()
            else matches.asSequence().mapNotNull { match ->
                val minutes = match.groupValues[1].toLongOrNull() ?: return@mapNotNull null
                val seconds = match.groupValues[2].toLongOrNull() ?: return@mapNotNull null
                if (seconds > 59L) return@mapNotNull null
                val fraction = match.groupValues.getOrNull(3).orEmpty().padEnd(3, '0').take(3).toLongOrNull() ?: 0L
                LyricLine((minutes * 60_000L + seconds * 1_000L + fraction + offset).coerceAtLeast(0L), text)
            }
        }.sortedBy { it.timeMs }.toList()
    }
}
