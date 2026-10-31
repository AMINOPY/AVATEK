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

        // Validate and decode the complete backup before changing any local data.
        // A missing section must never be interpreted as an empty store and erase user data.
        val decoded = PREFERENCE_FILES.associateWith { name ->
            val values = source.optJSONObject(name) ?: error("Missing preference section: $name")
            buildMap<String, Any> {
                val keys = values.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    when (val value = values.opt(key)) {
                        is Boolean, is Int, is Long, is Double, is String -> put(key, value)
                        is JSONArray -> {
                            require(name == "avatek_library") { "Unexpected array in $name" }
                            val set = (0 until value.length())
                                .mapNotNull { value.optString(it).takeIf(String::isNotBlank) }
                                .toSet()
                            put(key, set)
                        }
                        else -> error("Unsupported backup value in $name")
                    }
                }
            }
        }

        val previous = PREFERENCE_FILES.associateWith { name ->
            app.getSharedPreferences(name, Context.MODE_PRIVATE).all.toMap()
        }
        try {
            PREFERENCE_FILES.forEach { name ->
                val editor = app.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear()
                decoded.getValue(name).forEach { (key, value) ->
                    when (value) {
                        is Boolean -> editor.putBoolean(key, value)
                        is Int -> editor.putInt(key, value)
                        is Long -> editor.putLong(key, value)
                        is Double -> editor.putFloat(key, value.toFloat())
                        is String -> editor.putString(key, value)
                        is Set<*> -> editor.putStringSet(key, value.filterIsInstance<String>().toSet())
                    }
                }
                check(editor.commit())
            }
        } catch (failure: Throwable) {
            // Best-effort rollback if a storage commit fails partway through restore.
            previous.forEach { (name, values) ->
                val editor = app.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear()
                values.forEach { (key, value) ->
                    when (value) {
                        is Boolean -> editor.putBoolean(key, value)
                        is Int -> editor.putInt(key, value)
                        is Long -> editor.putLong(key, value)
                        is Float -> editor.putFloat(key, value)
                        is String -> editor.putString(key, value)
                        is Set<*> -> editor.putStringSet(key, value.filterIsInstance<String>().toSet())
                    }
                }
                editor.commit()
            }
            throw failure
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
