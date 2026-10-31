package com.aminopy.avatek.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Portable JSON backup of AVATEK's own local preferences; music files are never copied. */
class BackupStore(context: Context) {
    private val app = context.applicationContext

    fun exportJson(): String {
        val root = JSONObject()
            .put("format", FORMAT)
            .put("version", VERSION)
            .put("exportedAt", System.currentTimeMillis())
        val preferences = JSONObject()
        PREFERENCE_FILES.forEach { name ->
            val values = JSONObject()
            app.getSharedPreferences(name, Context.MODE_PRIVATE).all.forEach { (key, value) ->
                when (value) {
                    is String, is Boolean, is Int, is Long, is Float -> values.put(key, value)
                    is Set<*> -> values.put(key, JSONArray(value.filterIsInstance<String>()))
                }
            }
            preferences.put(name, values)
        }
        return root.put("preferences", preferences).toString(2)
    }

    fun importJson(raw: String): Boolean = runCatching {
        val root = JSONObject(raw)
        require(root.optString("format") == FORMAT)
        require(root.optInt("version") == VERSION)
        val source = root.getJSONObject("preferences")
        PREFERENCE_FILES.forEach { name ->
            val target = app.getSharedPreferences(name, Context.MODE_PRIVATE)
            val values = source.optJSONObject(name) ?: JSONObject()
            val editor = target.edit().clear()
            val keys = values.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                when (val value = values.opt(key)) {
                    null, JSONObject.NULL -> Unit
                    is Boolean -> editor.putBoolean(key, value)
                    is Int -> editor.putInt(key, value)
                    is Long -> editor.putLong(key, value)
                    is Double -> editor.putFloat(key, value.toFloat())
                    is String -> editor.putString(key, value)
                    is JSONArray -> {
                        if (name == "avatek_library") {
                            val set = (0 until value.length()).mapNotNull { value.optString(it).takeIf(String::isNotBlank) }.toSet()
                            editor.putStringSet(key, set)
                        }
                    }
                }
            }
            check(editor.commit())
        }
        true
    }.getOrDefault(false)

    private companion object {
        const val FORMAT = "avatek-backup"
        const val VERSION = 1
        val PREFERENCE_FILES = listOf(
            "avatek_library",
            "avatek_playlists",
            "avatek_history",
            "avatek_settings",
            "avatek_eq",
            "avatek_queue",
            "avatek_lyrics"
        )
    }
}
