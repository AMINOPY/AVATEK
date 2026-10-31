package com.aminopy.avatek.data

import android.content.Context

enum class AccentTheme(val id: String, val label: String) {
    GOLD("gold", "طلای سیاه"),
    CRIMSON("crimson", "سرخ خون"),
    AURORA("aurora", "شفق"),
    EMERALD("emerald", "زمرد"),
    VIOLET("violet", "بنفش نئون"),
    WASTELAND("wasteland", "آخرین بازمانده");

    companion object {
        fun fromId(id: String?) = entries.firstOrNull { it.id == id } ?: GOLD
    }
}

data class UiSettings(
    val accent: AccentTheme = AccentTheme.GOLD,
    val glass: Float = 0.7f,
    val animations: Boolean = true,
    val haptics: Boolean = true,
    val ambientColors: Boolean = true,
    val visualizer: Boolean = true,
    val customNotification: Boolean = true,
    val minDurationSec: Int = 30
)

class SettingsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("avatek_settings", Context.MODE_PRIVATE)

    fun load() = UiSettings(
        accent = AccentTheme.fromId(prefs.getString("accent", null)),
        glass = prefs.getFloat("glass", 0.7f).coerceIn(0f, 1f),
        animations = prefs.getBoolean("animations", true),
        haptics = prefs.getBoolean("haptics", true),
        ambientColors = prefs.getBoolean("ambient", true),
        visualizer = prefs.getBoolean("visualizer", true),
        customNotification = prefs.getBoolean("custom_notification", true),
        minDurationSec = prefs.getInt("min_duration", 30)
    )

    fun save(s: UiSettings) {
        prefs.edit()
            .putString("accent", s.accent.id)
            .putFloat("glass", s.glass)
            .putBoolean("animations", s.animations)
            .putBoolean("haptics", s.haptics)
            .putBoolean("ambient", s.ambientColors)
            .putBoolean("visualizer", s.visualizer)
            .putBoolean("custom_notification", s.customNotification)
            .putInt("min_duration", s.minDurationSec)
            .apply()
    }
}
