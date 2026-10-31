@file:OptIn(UnstableApi::class)

package com.aminopy.avatek.playback

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/** Live levels of the audio that is really being played. Written by [AnalyzerProcessor], read by the UI. */
object AudioLevels {
    const val BANDS = 32
    @Volatile var bands: FloatArray = FloatArray(BANDS)
    @Volatile var bass: Float = 0f
    @Volatile var beat: Float = 0f

    fun clear() { bands = FloatArray(BANDS); bass = 0f; beat = 0f }
}

/**
 * Pass-through audio processor that runs a 1024-point FFT over the PCM stream and publishes
 * 32 log-spaced band levels plus a bass level and a beat pulse into [AudioLevels]. The audio itself is not changed.
 */
class AnalyzerProcessor : BaseAudioProcessor() {
    private val n = 1024
    private val ring = FloatArray(n)
    private var fill = 0
    private var channels = 2
    private var sampleRate = 44100
    private val re = FloatArray(n)
    private val im = FloatArray(n)
    private val window = FloatArray(n) { (0.5 - 0.5 * cos(2.0 * PI * it / (n - 1))).toFloat() }
    private val cosT = FloatArray(n / 2) { cos(2.0 * PI * it / n).toFloat() }
    private val sinT = FloatArray(n / 2) { -sin(2.0 * PI * it / n).toFloat() }
    private var smooth = FloatArray(AudioLevels.BANDS)
    private var bassAvg = 0f
    private var beatLevel = 0f
    private var edges = IntArray(AudioLevels.BANDS + 1)

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) return AudioProcessor.AudioFormat.NOT_SET
        channels = max(1, inputAudioFormat.channelCount)
        sampleRate = inputAudioFormat.sampleRate
        val binHz = sampleRate.toFloat() / n
        val lo = 40f
        val hi = minOf(14000f, sampleRate / 2f - 100f)
        var prev = max(1, (lo / binHz).toInt())
        edges[0] = prev
        for (b in 1..AudioLevels.BANDS) {
            val f = lo * Math.pow((hi / lo).toDouble(), b.toDouble() / AudioLevels.BANDS).toFloat()
            val bin = max(prev + 1, (f / binHz).toInt())
            edges[b] = minOf(bin, n / 2)
            prev = edges[b]
        }
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val size = inputBuffer.remaining()
        if (size == 0) return
        val view = inputBuffer.duplicate().order(ByteOrder.nativeOrder())
        val frame = 2 * channels
        while (view.remaining() >= frame) {
            var sum = 0f
            for (c in 0 until channels) sum += view.short / 32768f
            ring[fill++] = sum / channels
            if (fill == n) { analyze(); fill = 0 }
        }
        val out = replaceOutputBuffer(size)
        out.put(inputBuffer)
        out.flip()
    }

    override fun onFlush() { fill = 0; AudioLevels.clear(); smooth = FloatArray(AudioLevels.BANDS); beatLevel = 0f }
    override fun onReset() { onFlush() }

    private fun analyze() {
        for (i in 0 until n) { re[i] = ring[i] * window[i]; im[i] = 0f }
        fft()
        val out = FloatArray(AudioLevels.BANDS)
        for (b in 0 until AudioLevels.BANDS) {
            var s = 0f
            val a = edges[b]; val e = max(edges[b + 1], a + 1)
            for (k in a until e) s += re[k] * re[k] + im[k] * im[k]
            val amp = sqrt(s / (e - a)) / (n / 4f)
            val db = 20f * log10(max(amp, 1e-5f))
            val v = ((db + 66f) / 66f).coerceIn(0f, 1f)
            val old = smooth[b]
            smooth[b] = if (v > old) old + (v - old) * 0.65f else old + (v - old) * 0.18f
            out[b] = smooth[b]
        }
        var bass = 0f
        for (b in 0 until 5) bass += out[b]
        bass /= 5f
        val flux = bass - bassAvg
        bassAvg += (bass - bassAvg) * 0.08f
        beatLevel *= 0.88f
        if (flux > 0.07f && beatLevel < 0.45f) beatLevel = 1f
        AudioLevels.bands = out
        AudioLevels.bass = bass
        AudioLevels.beat = beatLevel
    }

    private fun fft() {
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) { j = j xor bit; bit = bit shr 1 }
            j = j xor bit
            if (i < j) { val t = re[i]; re[i] = re[j]; re[j] = t }
        }
        var len = 2
        while (len <= n) {
            val half = len / 2
            val step = n / len
            var i = 0
            while (i < n) {
                for (k in 0 until half) {
                    val wr = cosT[k * step]; val wi = sinT[k * step]
                    val a = i + k; val b = a + half
                    val tr = re[b] * wr - im[b] * wi
                    val ti = re[b] * wi + im[b] * wr
                    re[b] = re[a] - tr; im[b] = im[a] - ti
                    re[a] += tr; im[a] += ti
                }
                i += len
            }
            len = len shl 1
        }
    }
}
