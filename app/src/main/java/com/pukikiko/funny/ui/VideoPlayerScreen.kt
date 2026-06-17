package com.pukikiko.funny.ui

import android.graphics.Bitmap
import android.net.Uri
import kotlin.OptIn
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.TextureView
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Text
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Button
import androidx.tv.material3.OutlinedButton
import com.pukikiko.funny.R
import com.pukikiko.funny.api.RetrofitClient
import com.pukikiko.funny.api.VideoModel
import com.pukikiko.funny.api.VoteRequest
import com.pukikiko.funny.data.WatchedRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun VideoPlayerScreen(repository: WatchedRepository) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val sessionVideos = remember { mutableStateListOf<VideoModel>() }
    var currentIndex by remember { mutableIntStateOf(-1) }
    
    var isLoading by remember { mutableStateOf(false) }
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    val focusRequester = remember { FocusRequester() }
    val settingsFocusRequester = remember { FocusRequester() }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var lastFeedback by remember { mutableStateOf("") }
    
    var showControls by remember { mutableStateOf(true) }
    var showSettings by remember { mutableStateOf(false) }
    
    var instanceUrl by remember { mutableStateOf(repository.getBaseUrl()) }
    var centerDownTime by remember { mutableLongStateOf(0L) }
    var hasVotedCurrentVideo by remember { mutableStateOf(false) }

    val currentTextureView = remember { mutableStateOf<TextureView?>(null) }
    var currentSnapshot by remember { mutableStateOf<Bitmap?>(null) }

    fun playCurrentVideo() {
        if (currentIndex in sessionVideos.indices) {
            hasVotedCurrentVideo = false
            val video = sessionVideos[currentIndex]
            exoPlayer?.let {
                it.stop()
                val baseUrl = if (instanceUrl.endsWith("/")) instanceUrl else "$instanceUrl/"
                val uri = Uri.parse("${baseUrl}videos/${video.filename}")
                val mediaItem = MediaItem.fromUri(uri)
                it.setMediaItem(mediaItem)
                it.prepare()
                it.play()
            }
        }
    }

    fun navigateTo(index: Int) {
        if (index == currentIndex) return
        currentSnapshot = currentTextureView.value?.bitmap
        currentIndex = index
        playCurrentVideo()
    }

    fun loadNextVideo() {
        if (currentIndex < sessionVideos.size - 1) {
            navigateTo(currentIndex + 1)
        } else {
            if (isLoading) return
            isLoading = true
            coroutineScope.launch {
                try {
                    val watchedString = repository.getWatchedString()
                    val api = RetrofitClient.getApi(instanceUrl)
                    val video = api.getNextVideo(watchedString)
                    sessionVideos.add(video)
                    repository.addWatchedId(video.id)
                    navigateTo(currentIndex + 1)
                } catch (e: Exception) {
                    Log.e("FunnyTV", "Error loading next video", e)
                } finally {
                    isLoading = false
                }
            }
        }
    }
    
    fun loadPreviousVideo() {
        if (currentIndex > 0) {
            navigateTo(currentIndex - 1)
        }
    }

    fun vote(action: String) {
        if (hasVotedCurrentVideo) return
        val video = sessionVideos.getOrNull(currentIndex)
        video?.let {
            coroutineScope.launch {
                hasVotedCurrentVideo = true
                try {
                    RetrofitClient.getApi(instanceUrl).voteVideo(it.id, VoteRequest(action))
                    feedbackMessage = if (action == "up") "👍" else "👎"
                    delay(2000)
                    if (feedbackMessage == "👍" || feedbackMessage == "👎") {
                        feedbackMessage = null
                    }
                } catch (e: Exception) {
                    Log.e("FunnyTV", "Error voting", e)
                    hasVotedCurrentVideo = false
                }
            }
        }
    }

    DisposableEffect(Unit) {
        val player = ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ALL
            playWhenReady = true
        }
        exoPlayer = player
        loadNextVideo()

        onDispose {
            player.release()
        }
    }

    LaunchedEffect(feedbackMessage) {
        if (feedbackMessage != null) lastFeedback = feedbackMessage!!
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(showSettings) {
        if (showSettings) {
            delay(50)
            try { settingsFocusRequester.requestFocus() } catch (e: Exception) {}
        } else {
            try { focusRequester.requestFocus() } catch (e: Exception) {}
        }
    }

    LaunchedEffect(showControls, showSettings) {
        if (showControls && !showSettings) {
            delay(3000)
            showControls = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                if (showSettings) {
                    if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN && keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_BACK) {
                        showSettings = false
                        return@onKeyEvent true
                    }
                    return@onKeyEvent false
                }

                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    showControls = true

                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            loadNextVideo()
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_UP -> {
                            loadPreviousVideo()
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            vote("up")
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                            vote("down")
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                            if (keyEvent.nativeKeyEvent.repeatCount == 0) {
                                centerDownTime = System.currentTimeMillis()
                            }
                            true
                        }
                        else -> false
                    }
                } else if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_UP) {
                    if (keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
                        if (centerDownTime > 0) {
                            val duration = System.currentTimeMillis() - centerDownTime
                            if (duration > 600) {
                                showSettings = true
                            } else {
                                exoPlayer?.let {
                                    if (it.isPlaying) it.pause() else it.play()
                                }
                            }
                        }
                        centerDownTime = 0L
                        true
                    } else {
                        false
                    }
                } else {
                    false
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (isLoading && sessionVideos.isEmpty()) {
            Text("Loading...", color = Color.White)
        } else if (sessionVideos.isEmpty()) {
            Text("No more videos available.", color = Color.White)
        } else {
            AnimatedContent(
                targetState = currentIndex,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInVertically(
                            animationSpec = tween(500),
                            initialOffsetY = { fullHeight -> fullHeight }
                        ) togetherWith slideOutVertically(
                            animationSpec = tween(500),
                            targetOffsetY = { fullHeight -> -fullHeight }
                        )
                    } else {
                        slideInVertically(
                            animationSpec = tween(500),
                            initialOffsetY = { fullHeight -> -fullHeight }
                        ) togetherWith slideOutVertically(
                            animationSpec = tween(500),
                            targetOffsetY = { fullHeight -> fullHeight }
                        )
                    }
                },
                label = "VideoScroll"
            ) { index ->
                if (index == currentIndex) {
                    AndroidView(
                        factory = { ctx ->
                            val inflater = LayoutInflater.from(ctx)
                            val view = inflater.inflate(R.layout.texture_player_view, null) as PlayerView
                            currentTextureView.value = view.videoSurfaceView as? TextureView
                            view.player = exoPlayer
                            view
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    if (currentSnapshot != null) {
                        Image(
                            bitmap = currentSnapshot!!.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Box(Modifier.fillMaxSize().background(Color.Black))
                    }
                }
            }

            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(tween(300)),
                exit = fadeOut(tween(500)),
                modifier = Modifier.align(Alignment.BottomStart)
            ) {
                Row(
                    modifier = Modifier
                        .padding(32.dp)
                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                        .padding(vertical = 12.dp, horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = "Prev", tint = Color.White, modifier = Modifier.size(16.dp))
                            Icon(Icons.Default.ArrowDownward, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Navigate", color = Color.White, style = MaterialTheme.typography.titleMedium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ThumbDown, contentDescription = "Left", tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(Icons.Default.ThumbUp, contentDescription = "Right", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Vote", color = Color.White, style = MaterialTheme.typography.titleMedium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Center", tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Play/Pause", color = Color.White, style = MaterialTheme.typography.titleMedium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Settings, contentDescription = "Long Center", tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Hold Center for Settings", color = Color.White, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            AnimatedVisibility(
                visible = feedbackMessage != null,
                enter = fadeIn(tween(300)),
                exit = fadeOut(tween(500)),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Text(
                    text = lastFeedback,
                    color = Color.White,
                    style = MaterialTheme.typography.displayLarge,
                )
            }
        }
        
        if (showSettings) {
            var editingUrl by remember { mutableStateOf(instanceUrl) }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .background(Color.DarkGray, RoundedCornerShape(12.dp))
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Settings", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Server Instance URL", color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Box(
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                            .padding(16.dp)
                    ) {
                        BasicTextField(
                            value = editingUrl,
                            onValueChange = { editingUrl = it },
                            textStyle = TextStyle(color = Color.White, fontSize = 20.sp),
                            modifier = Modifier
                                .width(300.dp)
                                .focusRequester(settingsFocusRequester),
                            cursorBrush = SolidColor(Color.White)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                instanceUrl = editingUrl
                                repository.setBaseUrl(instanceUrl)
                                sessionVideos.clear()
                                currentIndex = -1
                                currentSnapshot = null
                                loadNextVideo()
                                showSettings = false
                            }
                        ) {
                            Text("Apply URL")
                        }
                        Button(onClick = {
                            editingUrl = "https://funny.mfc.pw"
                        }) {
                            Text("Reset to Default")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(onClick = {
                        showSettings = false
                    }) {
                        Text("Close", color = Color.White)
                    }
                }
            }
        }
    }
}
