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
 */
class FeedController(
    private val context: Context,
    private val repository: WatchedRepository,
    private val scope: CoroutineScope,
    val player: ExoPlayer
) {
    val videos = mutableStateListOf<VideoModel>()

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
    var hasVoted by mutableStateOf(false)
        private set

    private var statusJob: Job? = null
    private var prefetchJob: Job? = null
    private var volumeBeforeMute = 0.5f

    val current: VideoModel? get() = videos.getOrNull(currentIndex)
    val canGoBack: Boolean get() = currentIndex > 0
    val progress: Float
        get() = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val currentUrl: String? get() = current?.let { videoUrl(instanceUrl, it.filename) }

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

    /** Keeps one video queued ahead so a skip doesn't wait on the network. */
    private fun prefetch() {
        if (currentIndex < videos.size - 1) return
        if (prefetchJob?.isActive == true) return
        prefetchJob = scope.launch { fetchOne() }
    }

    fun next() {
        if (currentIndex < videos.size - 1) {
            currentIndex++
            playCurrent()
            prefetch()
            return
        }
        scope.launch {
            isLoading = true
            val video = fetchOne()
            isLoading = false
            if (video != null) {
                currentIndex++
                playCurrent()
                prefetch()
            }
        }
    }

    fun previous() {
        if (!canGoBack) return
        currentIndex--
        playCurrent()
    }

    fun playCurrent() {
        val video = current ?: return
        hasVoted = false
        positionMs = 0L
        durationMs = 0L
        player.stop()
        player.setMediaItem(MediaItem.fromUri(Uri.parse(videoUrl(instanceUrl, video.filename))))
        player.volume = volume
        player.prepare()
        player.play()
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
        isPlaying = player.isPlaying
        positionMs = player.currentPosition.coerceAtLeast(0L)
        val duration = player.duration
        durationMs = if (duration > 0) duration else 0L
    }

    // ---- Volume ----

    fun changeVolume(value: Float) {
        volume = value.coerceIn(0f, 1f)
        player.volume = volume
        repository.setVolume(volume)
    }

    fun toggleMute() {
        if (volume > 0f) {
            volumeBeforeMute = volume
            changeVolume(0f)
        } else {
            changeVolume(if (volumeBeforeMute > 0f) volumeBeforeMute else 0.5f)
        }
    }

    // ---- Feed mode ----

    fun cycleMode() {
        feedMode = FeedMode.next(feedMode)
        repository.setFeedMode(feedMode)
        // Drop anything queued under the old mode so the switch takes effect now.
        while (videos.size > currentIndex + 1) {
            videos.removeAt(videos.size - 1)
        }
        showStatus("Mode: ${FeedMode.label(feedMode)}")
        prefetch()
    }

    // ---- Voting ----

    fun vote(action: String) {
        if (hasVoted) return
        val video = current ?: return
        hasVoted = true
        voteFeedback = if (action == "up") "👍" else "👎"
        scope.launch {
            try {
                val updated = RetrofitClient.getApi(instanceUrl).voteVideo(video.id, VoteRequest(action))
                val index = videos.indexOfFirst { it.id == updated.id }
                if (index >= 0) videos[index] = updated
            } catch (e: Exception) {
                Log.e(TAG, "Error voting", e)
                hasVoted = false
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

    fun changeInstanceUrl(url: String) {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return
        instanceUrl = trimmed
        repository.setBaseUrl(trimmed)
        videos.clear()
        currentIndex = -1
        next()
    }
}
