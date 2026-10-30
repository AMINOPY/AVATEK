package com.aminopy.avatek.playback

import android.os.Bundle
import androidx.media3.session.SessionCommand

/** Custom session commands that let the UI drive the audio-effects chain living in the service. */
object AvatekCommands {
    const val EQ_INFO = "avatek.eq.info"
    const val EQ_ENABLED = "avatek.eq.enabled"
    const val EQ_BAND = "avatek.eq.band"
    const val BASS = "avatek.eq.bass"
    const val VIRTUALIZER = "avatek.eq.virtualizer"
    const val EQ_RESET = "avatek.eq.reset"

    const val KEY_ENABLED = "enabled"
    const val KEY_BAND = "band"
    const val KEY_LEVEL = "level"
    const val KEY_STRENGTH = "strength"
    const val KEY_BANDS = "bands"
    const val KEY_MIN = "min"
    const val KEY_MAX = "max"
    const val KEY_CENTERS = "centers"
    const val KEY_LEVELS = "levels"
    const val KEY_BASS = "bass"
    const val KEY_VIRT = "virt"
    const val KEY_AVAILABLE = "available"

    val all: List<SessionCommand> = listOf(EQ_INFO, EQ_ENABLED, EQ_BAND, BASS, VIRTUALIZER, EQ_RESET)
        .map { SessionCommand(it, Bundle.EMPTY) }
}

/** Snapshot of the equalizer returned to the UI. */
data class EqualizerInfo(
    val available: Boolean = false,
    val enabled: Boolean = false,
    val minLevel: Int = -1500,
    val maxLevel: Int = 1500,
    val centerFreqsHz: List<Int> = emptyList(),
    val levels: List<Int> = emptyList(),
    val bass: Int = 0,
    val virtualizer: Int = 0
)
