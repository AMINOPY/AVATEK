package com.aminopy.avatek.data

import android.content.Context

/** Imported, offline LRC sidecars indexed by the local MediaStore track id. */
class LyricsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("avatek_lyrics", Context.MODE_PRIVATE)

    fun load(trackId: Long): String? = prefs.getString(trackId.toString(), null)

    fun save(trackId: Long, lrc: String) {
        prefs.edit().putString(trackId.toString(), lrc.take(MAX_LRC_CHARS)).apply()
    }

    companion object {
        const val MAX_LRC_CHARS = 250_000
    }
}
