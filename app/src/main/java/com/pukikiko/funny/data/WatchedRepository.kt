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

    private val MODE_KEY = "feed_mode"
    private val VOLUME_KEY = "volume"
    private val AUTO_SCROLL_KEY = "auto_scroll"

    fun getFeedMode(): String {
        val mode = prefs.getString(MODE_KEY, FeedMode.DEFAULT)
        return if (mode in FeedMode.ALL) mode!! else FeedMode.DEFAULT
    }

    fun setFeedMode(mode: String) {
        prefs.edit().putString(MODE_KEY, if (mode in FeedMode.ALL) mode else FeedMode.DEFAULT).apply()
    }

    // Matches the web player's default of half volume.
    fun getVolume(): Float {
        return prefs.getFloat(VOLUME_KEY, 0.5f).coerceIn(0f, 1f)
    }

    fun setVolume(volume: Float) {
        prefs.edit().putFloat(VOLUME_KEY, volume.coerceIn(0f, 1f)).apply()
    }

    fun getAutoScroll(): Boolean {
        return prefs.getBoolean(AUTO_SCROLL_KEY, false)
    }

    fun setAutoScroll(enabled: Boolean) {
        prefs.edit().putBoolean(AUTO_SCROLL_KEY, enabled).apply()
    }
}

/** Feed selection modes offered by the server's /api/video/next endpoint. */
object FeedMode {
    const val ALGORITHM = "algorithm"
    const val RANDOM = "random"

    const val DEFAULT = ALGORITHM
    val ALL = listOf(ALGORITHM, RANDOM)

    fun label(mode: String): String = when (mode) {
        RANDOM -> "Randomised"
        else -> "PipeAI Algorithm"
    }

    fun next(mode: String): String = ALL[(ALL.indexOf(mode) + 1).mod(ALL.size)]
}
