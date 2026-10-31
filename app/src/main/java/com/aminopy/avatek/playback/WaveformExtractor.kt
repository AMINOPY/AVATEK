package com.aminopy.avatek.playback

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.LruCache
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/** Decodes a track once in the background and keeps loudness peaks so the seek bar can draw the real waveform. */
object WaveformExtractor {
    private val cache = LruCache<String, FloatArray>(24)

    fun extract(context: Context, uri: Uri, buckets: Int = 96, alive: () -> Boolean = { true }): FloatArray? {
        cache.get(uri.toString())?.let { return it }
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        return try {
            extractor.setDataSource(context, uri, null)
            var index = -1
            for (i in 0 until extractor.trackCount) {
                if (extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) { index = i; break }
            }
            if (index < 0) return null
            val format = extractor.getTrackFormat(index)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: return null
            val durationUs = if (format.containsKey(MediaFormat.KEY_DURATION)) format.getLong(MediaFormat.KEY_DURATION) else 0L
            if (durationUs <= 0L) return null
            extractor.selectTrack(index)
            val c = MediaCodec.createDecoderByType(mime)
            codec = c
            c.configure(format, null, null, 0)
            c.start()
            val peaks = FloatArray(buckets)
            val info = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false
            while (!outputDone && alive()) {
                if (!inputDone) {
                    val i = c.dequeueInputBuffer(5000)
                    if (i >= 0) {
                        val buf = c.getInputBuffer(i)
                        val n = if (buf != null) extractor.readSampleData(buf, 0) else -1
                        if (n < 0) {
                            c.queueInputBuffer(i, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            c.queueInputBuffer(i, 0, n, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }
                val o = c.dequeueOutputBuffer(info, 5000)
                if (o >= 0) {
                    val ob = c.getOutputBuffer(o)
                    if (ob != null && info.size > 0) {
                        ob.position(info.offset)
                        ob.limit(info.offset + info.size)
                        val sb = ob.order(ByteOrder.nativeOrder()).asShortBuffer()
                        val b = ((info.presentationTimeUs.toDouble() / durationUs) * buckets).toInt().coerceIn(0, buckets - 1)
                        var m = 0
                        var k = 0
                        val lim = sb.limit()
                        while (k < lim) { val v = abs(sb.get(k).toInt()); if (v > m) m = v; k += 5 }
                        peaks[b] = max(peaks[b], m / 32768f)
                    }
                    c.releaseOutputBuffer(o, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                }
            }
            if (!alive()) return null
            val top = peaks.max().takeIf { it > 0f } ?: return null
            val result = FloatArray(buckets) { sqrt(peaks[it] / top).coerceIn(0.04f, 1f) }
            cache.put(uri.toString(), result)
            result
        } catch (e: Exception) {
            null
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            runCatching { extractor.release() }
        }
    }
}
