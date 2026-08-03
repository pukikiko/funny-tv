package com.pukikiko.funny.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pukikiko.funny.api.RetrofitClient
import com.pukikiko.funny.api.VideoModel
import com.pukikiko.funny.api.VoteRequest
import com.pukikiko.funny.data.WatchedRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VideoPlayerViewModel(
    private val repository: WatchedRepository
) : ViewModel() {

    private val _videos = MutableStateFlow<List<VideoModel>>(emptyList())
    val videos: StateFlow<List<VideoModel>> = _videos.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    private val _instanceUrl = MutableStateFlow(repository.getBaseUrl())
    val instanceUrl: StateFlow<String> = _instanceUrl.asStateFlow()

    private val _votedVideoIds = MutableStateFlow<Set<Int>>(repository.getVotedIds().toSet())
    val votedVideoIds: StateFlow<Set<Int>> = _votedVideoIds.asStateFlow()

    init {
        loadNextVideo()
    }

    fun loadNextVideo() {
        if (_isLoading.value) return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val watchedString = repository.getWatchedString()
                val api = RetrofitClient.getApi(_instanceUrl.value)
                val video = api.getNextVideo(watchedString)
                _videos.value = _videos.value + video
                repository.addWatchedId(video.id)
            } catch (e: Exception) {
                Log.e("VideoPlayerViewModel", "Error loading next video", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun vote(videoId: Int, action: String) {
        if (_votedVideoIds.value.contains(videoId)) return
        viewModelScope.launch {
            try {
                _votedVideoIds.value = _votedVideoIds.value + videoId
                repository.addVotedId(videoId)
                RetrofitClient.getApi(_instanceUrl.value).voteVideo(videoId, VoteRequest(action))
                _feedbackMessage.value = if (action == "up") "👍" else "👎"
                delay(2000)
                _feedbackMessage.value = null
            } catch (e: Exception) {
                Log.e("VideoPlayerViewModel", "Error voting", e)
            }
        }
    }

    fun setInstanceUrl(url: String) {
        _instanceUrl.value = url
        repository.setBaseUrl(url)
        _videos.value = emptyList() // clear and reload
        loadNextVideo()
    }
}
