package com.pukikiko.funny.ui

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.pukikiko.funny.api.RetrofitClient
import com.pukikiko.funny.api.VideoModel
import com.pukikiko.funny.api.VoteRequest
import com.pukikiko.funny.data.FeedMode
import com.pukikiko.funny.data.WatchedRepository
import com.pukikiko.funny.data.copyToClipboard
import com.pukikiko.funny.data.downloadVideo
import com.pukikiko.funny.data.shareLink
import com.pukikiko.funny.data.uploadVideo
import com.pukikiko.funny.data.videoUrl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.HttpException

private const val TAG = "FunnyTV"

/**
 * Everything the feed does, independent of how it is driven. The tv flavour
 * points a D-pad at this, the mobile flavour points fingers at it.
 *
 * Two players are kept alive: one showing the current video and one quietly
 * buffering the next, so a skip starts instantly instead of waiting on the
 * network. Advancing swaps which is which.
 */
class FeedController(
    private val context: Context,
    private val repository: WatchedRepository,
    private val scope: CoroutineScope
) {
    val videos = mutableStateListOf<VideoModel>()

    val players: List<ExoPlayer> = List(2) { createPlayer() }

    var activeSlot by mutableIntStateOf(0)
        private set
    var currentIndex by mutableIntStateOf(-1)
        private set
    var isLoading by mutableStateOf(true)
        private set
    var status by mutableStateOf<String?>(null)
        private set
    var voteFeedback by mutableStateOf<String?>(null)
        private set
    var isPlaying by mutableStateOf(false)
        private set
    var positionMs by mutableLongStateOf(0L)
        private set
    var durationMs by mutableLongStateOf(0L)
        private set
    var volume by mutableFloatStateOf(repository.getVolume())
        private set
    var feedMode by mutableStateOf(repository.getFeedMode())
        private set
    var instanceUrl by mutableStateOf(repository.getBaseUrl())
        private set

    /** "up", "down", or null when the current video hasn't been voted on. */
    var votedAction by mutableStateOf<String?>(null)
        private set

    private var statusJob: Job? = null

    val player: ExoPlayer get() = players[activeSlot]
    private val standbyPlayer: ExoPlayer get() = players[1 - activeSlot]

    val current: VideoModel? get() = videos.getOrNull(currentIndex)
    val canGoBack: Boolean get() = currentIndex > 0
    val progress: Float
        get() = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val currentUrl: String? get() = current?.let { videoUrl(instanceUrl, it.filename) }

    private fun createPlayer(): ExoPlayer =
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ALL
            playWhenReady = false
            volume = repository.getVolume()
        }

    fun release() {
        players.forEach { it.release() }
    }

    fun showStatus(message: String, millis: Long = 2500) {
        statusJob?.cancel()
        status = message
        statusJob = scope.launch {
            delay(millis)
            status = null
        }
    }

    // ---- Feed ----

    private suspend fun fetchOne(): VideoModel? {
        return try {
            val video = RetrofitClient.getApi(instanceUrl)
                .getNextVideo(repository.getWatchedString(), feedMode)
            videos.add(video)
            repository.addWatchedId(video.id)
            video
        } catch (e: HttpException) {
            Log.e(TAG, "Error loading next video", e)
            if (e.code() == 404) showStatus("No videos on this instance yet")
            else showStatus("Server returned ${e.code()}")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error loading next video", e)
            showStatus("Can't reach $instanceUrl")
            null
        }
    }

    private fun bind(player: ExoPlayer, video: VideoModel) {
        if (player.currentMediaItem?.mediaId == video.id.toString()) return
        player.setMediaItem(
            MediaItem.Builder()
                .setUri(Uri.parse(videoUrl(instanceUrl, video.filename)))
                .setMediaId(video.id.toString())
                .build()
        )
        player.prepare()
    }

    fun start() {
        scope.launch {
            if (videos.isEmpty()) {
                isLoading = true
                fetchOne()
                isLoading = false
            }
            if (currentIndex < 0 && videos.isNotEmpty()) {
                currentIndex = 0
                playCurrent()
            }
            ensureNext()
        }
    }

    private fun playCurrent() {
        val video = current ?: return
        votedAction = null
        positionMs = 0L
        durationMs = 0L
        val active = player
        bind(active, video)
        active.volume = volume
        active.seekTo(0)
        active.play()
        standbyPlayer.pause()
    }

    /**
     * Guarantees the next video exists and is buffering on the standby player.
     * Returns false when there is nothing more to show.
     */
    suspend fun ensureNext(): Boolean {
        if (currentIndex + 1 >= videos.size) {
            isLoading = true
            val fetched = fetchOne()
            isLoading = false
            if (fetched == null) return false
        }
        val nextVideo = videos.getOrNull(currentIndex + 1) ?: return false
        val standby = standbyPlayer
        bind(standby, nextVideo)
        standby.volume = volume
        standby.playWhenReady = false
        return true
    }

    /** Promotes the buffered standby player to the visible one. */
    fun advance() {
        if (currentIndex + 1 >= videos.size) return
        player.pause()
        activeSlot = 1 - activeSlot
        currentIndex++
        playCurrent()
        scope.launch { ensureNext() }
    }

    fun goBack() {
        if (!canGoBack) return
        player.pause()
        activeSlot = 1 - activeSlot
        currentIndex--
        playCurrent()
        scope.launch { ensureNext() }
    }

    // ---- Playback ----

    fun togglePlayPause() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun seekToFraction(fraction: Float) {
        if (durationMs <= 0) return
        val target = (fraction.coerceIn(0f, 1f) * durationMs).toLong()
        player.seekTo(target)
        positionMs = target
    }

    fun seekBy(deltaMs: Long) {
        if (durationMs <= 0) return
        val target = (player.currentPosition + deltaMs).coerceIn(0L, durationMs)
        player.seekTo(target)
        positionMs = target
    }

    /** Polled from the UI so the scrubber and play indicator stay in step. */
    fun syncPlaybackState() {
        val active = player
        isPlaying = active.isPlaying
        positionMs = active.currentPosition.coerceAtLeast(0L)
        val duration = active.duration
        durationMs = if (duration > 0) duration else 0L
    }

    // ---- Volume ----

    fun changeVolume(value: Float) {
        volume = value.coerceIn(0f, 1f)
        players.forEach { it.volume = volume }
        repository.setVolume(volume)
    }

    // ---- Feed mode ----

    fun cycleMode() {
        applyFeedMode(FeedMode.next(feedMode))
        repository.setFeedMode(feedMode)
        showStatus("Mode: ${FeedMode.label(feedMode)}")
    }

    private fun applyFeedMode(mode: String) {
        feedMode = mode
        // Drop anything queued under the old mode so the switch takes effect now.
        while (videos.size > currentIndex + 1) {
            videos.removeAt(videos.size - 1)
        }
        standbyPlayer.clearMediaItems()
        scope.launch { ensureNext() }
    }

    // ---- Voting ----

    fun vote(action: String) {
        if (votedAction != null) return
        val video = current ?: return
        votedAction = action
        voteFeedback = if (action == "up") "👍" else "👎"
        scope.launch {
            try {
                val updated = RetrofitClient.getApi(instanceUrl).voteVideo(video.id, VoteRequest(action))
                val index = videos.indexOfFirst { it.id == updated.id }
                if (index >= 0) videos[index] = updated
            } catch (e: Exception) {
                Log.e(TAG, "Error voting", e)
                votedAction = null
                showStatus("Vote failed")
            }
            delay(2000)
            voteFeedback = null
        }
    }

    // ---- Sharing ----

    fun copyLink() {
        val url = currentUrl ?: return
        copyToClipboard(context, url)
        showStatus("Link copied")
    }

    fun share() {
        val url = currentUrl ?: return
        try {
            shareLink(context, url)
        } catch (e: Exception) {
            Log.e(TAG, "No app to share with", e)
            copyToClipboard(context, url)
            showStatus("Nothing to share with, link copied instead")
        }
    }

    fun download() {
        val video = current ?: return
        val url = currentUrl ?: return
        try {
            showStatus(downloadVideo(context, url, video.filename))
        } catch (e: Exception) {
            Log.e(TAG, "Download failed", e)
            showStatus("Download failed")
        }
    }

    fun upload(uri: Uri) {
        scope.launch {
            showStatus("Uploading…", 60_000)
            showStatus(uploadVideo(context, instanceUrl, uri), 4000)
        }
    }

    // ---- Instance ----

    /** Picks up anything the settings screen changed while we were backgrounded. */
    fun refreshFromPrefs() {
        val savedVolume = repository.getVolume()
        if (savedVolume != volume) {
            volume = savedVolume
            players.forEach { it.volume = savedVolume }
        }

        val savedMode = repository.getFeedMode()
        if (savedMode != feedMode) applyFeedMode(savedMode)

        val savedUrl = repository.getBaseUrl()
        if (savedUrl != instanceUrl) {
            instanceUrl = savedUrl
            restart()
        }
    }

    fun changeInstanceUrl(url: String) {
        val trimmed = url.trim()
        if (trimmed.isEmpty() || trimmed == instanceUrl) return
        instanceUrl = trimmed
        repository.setBaseUrl(trimmed)
        restart()
    }

    private fun restart() {
        videos.clear()
        currentIndex = -1
        // Ids repeat across instances, so wipe the items or bind() would skip.
        players.forEach {
            it.stop()
            it.clearMediaItems()
        }
        start()
    }
}
