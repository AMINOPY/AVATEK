package com.aminopy.avatek.playback

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.os.Bundle
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

/**
 * Owns the playback engine and the audio-effects chain (equalizer, bass boost, virtualizer)
 * so audio keeps playing when the UI is not visible. The activity talks to it through
 * a MediaController and the custom commands in [AvatekCommands].
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class MusicPlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
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
        val exo = ExoPlayer.Builder(this, renderers)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
        exo.addListener(sessionListener)
        player = exo
        mediaSession = MediaSession.Builder(this, exo).setCallback(EffectsCallback()).build()
        attachEffects(exo.audioSessionId)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        releaseEffects()
        player?.removeListener(sessionListener)
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        player = null
        super.onDestroy()
    }

    // ---- audio effects ------------------------------------------------------------------------

    private fun releaseEffects() {
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        runCatching { virtualizer?.release() }
        equalizer = null; bassBoost = null; virtualizer = null
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
                AvatekCommands.EQ_RESET -> {
                    val eq = equalizer
                    if (eq != null) for (b in 0 until eq.numberOfBands) {
                        runCatching { eq.setBandLevel(b.toShort(), 0) }
                    }
                    prefs.edit().putString("levels", "").putInt("bass", 0).putInt("virt", 0).apply()
                    runCatching { if (bassBoost?.strengthSupported == true) bassBoost?.setStrength(0) }
                    runCatching { if (virtualizer?.strengthSupported == true) virtualizer?.setStrength(0) }
                }
                AvatekCommands.EQ_INFO -> Unit
                else -> return Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED))
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS, buildInfo()))
        }
    }
}
