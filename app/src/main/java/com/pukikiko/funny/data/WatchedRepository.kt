package com.pukikiko.funny.data

import android.content.Context
import android.content.SharedPreferences

class WatchedRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("funny_prefs", Context.MODE_PRIVATE)
    private val WATCHED_KEY = "watched_ids"
    private val MAX_IDS = 500

    fun addWatchedId(id: Int) {
        val currentIds = getWatchedIds().toMutableList()
        if (!currentIds.contains(id)) {
            currentIds.add(id)
            if (currentIds.size > MAX_IDS) {
                currentIds.removeAt(0) // Remove the oldest
            }
            saveIds(currentIds)
        }
    }

    fun getWatchedIds(): List<Int> {
        val stringData = prefs.getString(WATCHED_KEY, "") ?: ""
        if (stringData.isEmpty()) return emptyList()
        return stringData.split(",").mapNotNull { it.toIntOrNull() }
    }

    fun getWatchedString(): String {
        return getWatchedIds().joinToString(",")
    }

    private fun saveIds(ids: List<Int>) {
        prefs.edit().putString(WATCHED_KEY, ids.joinToString(",")).apply()
    }

    private val BASE_URL_KEY = "base_url"

    fun getBaseUrl(): String {
        return prefs.getString(BASE_URL_KEY, "https://funny.mfc.pw") ?: "https://funny.mfc.pw"
    }

    fun setBaseUrl(url: String) {
        prefs.edit().putString(BASE_URL_KEY, url).apply()
    }
}
