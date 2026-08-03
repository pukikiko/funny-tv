package com.pukikiko.funny.ui

import android.view.KeyEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalTvMaterial3Api::class)
@Composable
fun TvPlayerScreen(viewModel: VideoPlayerViewModel) {
    val videos by viewModel.videos.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val instanceUrl by viewModel.instanceUrl.collectAsState()
    val feedbackMessage by viewModel.feedbackMessage.collectAsState()
    val votedVideoIds by viewModel.votedVideoIds.collectAsState()

    val pagerState = rememberPagerState(pageCount = { videos.size })
    val coroutineScope = rememberCoroutineScope()
    
    val focusRequester = remember { FocusRequester() }
    val settingsFocusRequester = remember { FocusRequester() }
    
    var showSettings by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(true) }
    var centerDownTime by remember { mutableLongStateOf(0L) }
    
    LaunchedEffect(videos.size, pagerState.currentPage) {
        if (videos.isNotEmpty() && pagerState.currentPage >= videos.size - 2) {
            viewModel.loadNextVideo()
        }
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
                            coroutineScope.launch {
                                if (pagerState.currentPage < videos.size - 1) {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                    isPlaying = true
                                }
                            }
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_UP -> {
                            coroutineScope.launch {
                                if (pagerState.currentPage > 0) {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                    isPlaying = true
                                }
                            }
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            if (videos.isNotEmpty()) {
                                viewModel.vote(videos[pagerState.currentPage].id, "up")
                            }
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                            if (videos.isNotEmpty()) {
                                viewModel.vote(videos[pagerState.currentPage].id, "down")
                            }
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
                                isPlaying = !isPlaying
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
        if (videos.isEmpty() && isLoading) {
            Text("Loading...", color = Color.White)
        } else if (videos.isEmpty()) {
            Text("No more videos available.", color = Color.White)
        } else {
            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = false // Managed by D-Pad keys
            ) { page ->
                val video = videos[page]
                val baseUrl = if (instanceUrl.endsWith("/")) instanceUrl else "$instanceUrl/"
                val videoUrl = "${baseUrl}videos/${video.filename}"
                val isVisible = (page == pagerState.currentPage)
                VideoPlayerItem(
                    videoUrl = videoUrl,
                    isPlaying = isVisible && isPlaying,
                    modifier = Modifier.fillMaxSize()
                )
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
                        val hasVoted = videos.isNotEmpty() && votedVideoIds.contains(videos[pagerState.currentPage].id)
                        val iconTint = if (hasVoted) Color.White.copy(alpha = 0.5f) else Color.White
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ThumbDown, contentDescription = "Left", tint = iconTint, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(Icons.Default.ThumbUp, contentDescription = "Right", tint = iconTint, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Vote", color = iconTint, style = MaterialTheme.typography.titleMedium)
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
                if (feedbackMessage != null) {
                    Text(
                        text = feedbackMessage!!,
                        color = Color.White,
                        style = MaterialTheme.typography.displayLarge,
                    )
                }
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
                                viewModel.setInstanceUrl(editingUrl)
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
