package com.aminopy.avatek.playback

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.os.Bundle
import android.os.SystemClock
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionCommands
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Owns the playback engine and the audio-effects chain (equalizer, bass boost, virtualizer)
 * so audio keeps playing when the UI is not visible. The activity talks to it through
 * a MediaController and the custom commands in [AvatekCommands].
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class MusicPlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null
    private var standbyPlayer: ExoPlayer? = null
    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var crossfadeDurationMs = 0L
    private var crossfading = false
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private val prefs by lazy { getSharedPreferences("avatek_eq", Context.MODE_PRIVATE) }

    private val sessionListener = object : Player.Listener {
        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            attachEffects(audioSessionId)
        }
    }

    override fun onCreate() {
        super.onCreate()
        setMediaNotificationProvider(AvatekNotificationProvider(this))
        val renderers = object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: Context, enableFloatOutput: Boolean, enableAudioTrackPlaybackParams: Boolean
            ): AudioSink = DefaultAudioSink.Builder(context)
                .setEnableFloatOutput(enableFloatOutput)
                .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                .setAudioProcessors(arrayOf(AnalyzerProcessor()))
                .build()
        }
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
        fun buildPlayer(handleAudioFocus: Boolean): ExoPlayer = ExoPlayer.Builder(this, renderers)
            .setAudioAttributes(audioAttributes, handleAudioFocus)
            .setHandleAudioBecomingNoisy(handleAudioFocus)
            .build()
        val exo = buildPlayer(true)
        val spare = buildPlayer(false).apply { volume = 0f }
        exo.addListener(sessionListener)
        player = exo
        standbyPlayer = spare
        crossfadeDurationMs = getSharedPreferences("avatek_playback", Context.MODE_PRIVATE)
            .getLong("crossfade_ms", 0L).coerceIn(0L, 12_000L)
        mediaSession = MediaSession.Builder(this, exo).setCallback(EffectsCallback()).build()
        attachEffects(exo.audioSessionId)
        engineScope.launch {
            while (isActive) {
                updateCrossfade()
                delay(100L)
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        engineScope.cancel()
        releaseEffects()
        player?.removeListener(sessionListener)
        mediaSession?.release()
        mediaSession = null
        player?.release()
        standbyPlayer?.release()
        player = null
        standbyPlayer = null
        super.onDestroy()
    }


    // ---- dual-player crossfade ----------------------------------------------------------------

    private fun prepareStandby(active: ExoPlayer, spare: ExoPlayer) {
        if (crossfading || active.mediaItemCount < 2 || active.shuffleModeEnabled ||
            active.repeatMode == Player.REPEAT_MODE_ONE) return
        var next = active.nextMediaItemIndex
        if (next == C.INDEX_UNSET && active.repeatMode == Player.REPEAT_MODE_ALL) next = 0
        if (next !in 0 until active.mediaItemCount) return
        val target = active.getMediaItemAt(next)
        if (spare.currentMediaItem?.mediaId == target.mediaId &&
            spare.playbackState != Player.STATE_IDLE && spare.playbackState != Player.STATE_ENDED) return
        spare.pause()
        spare.volume = 0f
        val items = (0 until active.mediaItemCount).map(active::getMediaItemAt)
        spare.setMediaItems(items, next, 0L)
        spare.repeatMode = active.repeatMode
        spare.shuffleModeEnabled = false
        spare.prepare()
    }

    private fun updateCrossfade() {
        val active = player ?: return
        val spare = standbyPlayer ?: return
        if (crossfadeDurationMs <= 0L || !active.isPlaying || active.shuffleModeEnabled ||
            active.repeatMode == Player.REPEAT_MODE_ONE || active.mediaItemCount < 2) return
        val duration = active.duration
        if (duration <= 0L || duration == C.TIME_UNSET) return
        val remaining = duration - active.currentPosition
        if (remaining <= crossfadeDurationMs + 4_000L && !crossfading) prepareStandby(active, spare)
        if (!crossfading && remaining in 1..crossfadeDurationMs &&
            spare.playbackState == Player.STATE_READY) {
            beginCrossfade(active, spare, active.nextMediaItemIndex)
        }
    }

    private fun beginCrossfade(active: ExoPlayer, spare: ExoPlayer, targetIndex: Int) {
        if (crossfading || targetIndex !in 0 until active.mediaItemCount) return
        crossfading = true
        val sourceIndex = active.currentMediaItemIndex
        spare.seekTo(targetIndex, 0L)
        spare.volume = 0f
        spare.play()
        engineScope.launch {
            val startedAt = SystemClock.elapsedRealtime()
            var completed = false
            try {
                while (isActive) {
                    if (player !== active || active.currentMediaItemIndex != sourceIndex || !active.isPlaying) break
                    val fraction = ((SystemClock.elapsedRealtime() - startedAt).toFloat() /
                        crossfadeDurationMs.coerceAtLeast(1L)).coerceIn(0f, 1f)
                    active.volume = 1f - fraction
                    spare.volume = fraction
                    if (fraction >= 1f) { completed = true; break }
                    delay(30L)
                }
                if (completed && player === active && active.currentMediaItemIndex == sourceIndex) {
                    spare.volume = 1f
                    active.pause()
                    mediaSession?.setPlayer(spare)
                    active.removeListener(sessionListener)
                    spare.addListener(sessionListener)
                    player = spare
                    standbyPlayer = active
                    active.volume = 0f
                    attachEffects(spare.audioSessionId)
                }
            } finally {
                crossfading = false
                if (player === active) {
                    spare.pause()
                    spare.volume = 0f
                }
            }
        }
    }

    private fun setCrossfadeDuration(value: Long) {
        crossfadeDurationMs = value.coerceIn(0L, 12_000L)
        getSharedPreferences("avatek_playback", Context.MODE_PRIVATE).edit()
            .putLong("crossfade_ms", crossfadeDurationMs).apply()
        if (crossfadeDurationMs == 0L) {
            standbyPlayer?.pause()
            standbyPlayer?.volume = 0f
        }
    }

    // ---- audio effects ------------------------------------------------------------------------

    private fun releaseEffects() {
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        runCatching { virtualizer?.release() }
        runCatching { loudnessEnhancer?.release() }
        equalizer = null; bassBoost = null; virtualizer = null; loudnessEnhancer = null
    }

    private fun attachEffects(sessionId: Int) {
        releaseEffects()
        if (sessionId == C.AUDIO_SESSION_ID_UNSET || sessionId == 0) return
        equalizer = runCatching { Equalizer(0, sessionId) }.getOrNull()?.also { eq ->
            val saved = savedLevels()
            for (band in 0 until eq.numberOfBands) {
                val level = saved.getOrNull(band) ?: 0
                runCatching { eq.setBandLevel(band.toShort(), level.toShort()) }
            }
            runCatching { eq.enabled = prefs.getBoolean("enabled", false) }
        }
        bassBoost = runCatching { BassBoost(0, sessionId) }.getOrNull()?.also { bb ->
            runCatching {
                if (bb.strengthSupported) bb.setStrength(prefs.getInt("bass", 0).toShort())
                bb.enabled = prefs.getBoolean("enabled", false)
            }
        }
        virtualizer = runCatching { Virtualizer(0, sessionId) }.getOrNull()?.also { v ->
            runCatching {
                if (v.strengthSupported) v.setStrength(prefs.getInt("virt", 0).toShort())
                v.enabled = prefs.getBoolean("enabled", false)
            }
        }
        loudnessEnhancer = runCatching { LoudnessEnhancer(sessionId) }.getOrNull()?.also { enhancer ->
            runCatching {
                enhancer.setTargetGain(prefs.getInt("loudness", 0))
                enhancer.enabled = prefs.getBoolean("loudness_enabled", false)
            }
        }
    }

    private fun savedLevels(): List<Int> =
        prefs.getString("levels", "").orEmpty().split(',').mapNotNull { it.toIntOrNull() }

    private fun applyEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("enabled", enabled).apply()
        runCatching { equalizer?.enabled = enabled }
        runCatching { bassBoost?.enabled = enabled }
        runCatching { virtualizer?.enabled = enabled }
    }

    private fun buildInfo(): Bundle {
        val eq = equalizer
        val out = Bundle()
        out.putBoolean(AvatekCommands.KEY_AVAILABLE, eq != null)
        out.putBoolean(AvatekCommands.KEY_ENABLED, prefs.getBoolean("enabled", false))
        out.putInt(AvatekCommands.KEY_BASS, prefs.getInt("bass", 0))
        out.putInt(AvatekCommands.KEY_VIRT, prefs.getInt("virt", 0))
        out.putInt(AvatekCommands.KEY_LOUDNESS, prefs.getInt("loudness", 0))
        out.putBoolean(AvatekCommands.KEY_LOUDNESS_ENABLED, prefs.getBoolean("loudness_enabled", false))
        out.putLong(AvatekCommands.KEY_CROSSFADE_MS, crossfadeDurationMs)
        if (eq != null) {
            val bands = eq.numberOfBands.toInt()
            out.putInt(AvatekCommands.KEY_BANDS, bands)
            out.putInt(AvatekCommands.KEY_MIN, eq.bandLevelRange[0].toInt())
            out.putInt(AvatekCommands.KEY_MAX, eq.bandLevelRange[1].toInt())
            out.putIntArray(
                AvatekCommands.KEY_CENTERS,
                IntArray(bands) { eq.getCenterFreq(it.toShort()) / 1000 }
            )
            out.putIntArray(
                AvatekCommands.KEY_LEVELS,
                IntArray(bands) { runCatching { eq.getBandLevel(it.toShort()).toInt() }.getOrDefault(0) }
            )
        }
        return out
    }

    private inner class EffectsCallback : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val commands = SessionCommands.Builder()
                .apply {
                    MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.commands.forEach { add(it) }
                    AvatekCommands.all.forEach { add(it) }
                }
                .build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(commands)
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            when (customCommand.customAction) {
                AvatekCommands.EQ_ENABLED -> applyEnabled(args.getBoolean(AvatekCommands.KEY_ENABLED))
                AvatekCommands.EQ_BAND -> {
                    val band = args.getInt(AvatekCommands.KEY_BAND)
                    val level = args.getInt(AvatekCommands.KEY_LEVEL)
                    runCatching { equalizer?.setBandLevel(band.toShort(), level.toShort()) }
                    val eq = equalizer
                    if (eq != null) {
                        val all = (0 until eq.numberOfBands).map {
                            runCatching { eq.getBandLevel(it.toShort()).toInt() }.getOrDefault(0)
                        }
                        prefs.edit().putString("levels", all.joinToString(",")).apply()
                    }
                }
                AvatekCommands.BASS -> {
                    val strength = args.getInt(AvatekCommands.KEY_STRENGTH).coerceIn(0, 1000)
                    prefs.edit().putInt("bass", strength).apply()
                    runCatching { if (bassBoost?.strengthSupported == true) bassBoost?.setStrength(strength.toShort()) }
                }
                AvatekCommands.VIRTUALIZER -> {
                    val strength = args.getInt(AvatekCommands.KEY_STRENGTH).coerceIn(0, 1000)
                    prefs.edit().putInt("virt", strength).apply()
                    runCatching { if (virtualizer?.strengthSupported == true) virtualizer?.setStrength(strength.toShort()) }
                }
                AvatekCommands.LOUDNESS -> {
                    val gain = args.getInt(AvatekCommands.KEY_GAIN).coerceIn(0, 1000)
                    prefs.edit().putInt("loudness", gain).putBoolean("loudness_enabled", gain > 0).apply()
                    runCatching {
                        loudnessEnhancer?.setTargetGain(gain)
                        loudnessEnhancer?.enabled = gain > 0
                    }
                }
                AvatekCommands.EQ_RESET -> {
                    val eq = equalizer
                    if (eq != null) for (b in 0 until eq.numberOfBands) {
                        runCatching { eq.setBandLevel(b.toShort(), 0) }
                    }
                    prefs.edit().putString("levels", "").putInt("bass", 0).putInt("virt", 0)
                        .putInt("loudness", 0).putBoolean("loudness_enabled", false)
                        .putBoolean("enabled", false).apply()
                    runCatching { if (bassBoost?.strengthSupported == true) bassBoost?.setStrength(0) }
                    runCatching { if (virtualizer?.strengthSupported == true) virtualizer?.setStrength(0) }
                    runCatching { equalizer?.enabled = false }
                    runCatching { bassBoost?.enabled = false }
                    runCatching { virtualizer?.enabled = false }
                    runCatching { loudnessEnhancer?.setTargetGain(0); loudnessEnhancer?.enabled = false }
                }
                AvatekCommands.CROSSFADE -> setCrossfadeDuration(args.getLong(AvatekCommands.KEY_CROSSFADE_MS))
                AvatekCommands.EQ_INFO -> Unit
                else -> return Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED))
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS, buildInfo()))
        }
    }
}
