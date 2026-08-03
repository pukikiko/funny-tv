package com.pukikiko.funny.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MobilePlayerScreen(viewModel: VideoPlayerViewModel) {
    val videos by viewModel.videos.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val instanceUrl by viewModel.instanceUrl.collectAsState()
    val feedbackMessage by viewModel.feedbackMessage.collectAsState()
    val votedVideoIds by viewModel.votedVideoIds.collectAsState()

    val pagerState = rememberPagerState(pageCount = { videos.size })
    var isPlaying by remember { mutableStateOf(true) }
    var showSettings by remember { mutableStateOf(false) }

    LaunchedEffect(videos.size, pagerState.currentPage) {
        if (videos.isNotEmpty() && pagerState.currentPage >= videos.size - 2) {
            viewModel.loadNextVideo()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (videos.isEmpty() && isLoading) {
            CircularProgressIndicator(color = Color.White)
        } else if (videos.isEmpty()) {
            Text("No more videos available.", color = Color.White)
        } else {
            VerticalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { isPlaying = !isPlaying }
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
                visible = !isPlaying,
                enter = fadeIn(tween(300)),
                exit = fadeOut(tween(300)),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(80.dp)
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .systemBarsPadding()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val hasVoted = videos.isNotEmpty() && votedVideoIds.contains(videos[pagerState.currentPage].id)
                IconButton(
                    onClick = { viewModel.vote(videos[pagerState.currentPage].id, "up") },
                    enabled = !hasVoted
                ) {
                    Icon(
                        Icons.Default.ThumbUp, 
                        contentDescription = "Like", 
                        tint = if (hasVoted) Color.White.copy(alpha = 0.5f) else Color.White, 
                        modifier = Modifier.size(32.dp)
                    )
                }
                IconButton(
                    onClick = { viewModel.vote(videos[pagerState.currentPage].id, "down") },
                    enabled = !hasVoted
                ) {
                    Icon(
                        Icons.Default.ThumbDown, 
                        contentDescription = "Dislike", 
                        tint = if (hasVoted) Color.White.copy(alpha = 0.5f) else Color.White, 
                        modifier = Modifier.size(32.dp)
                    )
                }
                IconButton(onClick = { showSettings = true }) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White, modifier = Modifier.size(32.dp))
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
                    .background(Color.Black.copy(alpha = 0.8f))
                    .clickable { showSettings = false },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .clickable { /* prevent propagation */ }
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
                            modifier = Modifier.width(300.dp),
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
                            Text("Reset")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(onClick = {
                        showSettings = false
                    }) {
                        Text("Close")
                    }
                }
            }
        }
    }
}
