package com.pukikiko.funny.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.pukikiko.funny.data.WatchedRepository
import kotlinx.coroutines.delay

/**
 * The parts of the feed that look the same everywhere: the player surface, the
 * paused indicator, vote feedback and status messages. Input and on-screen
 * controls come from [PlatformControls], which each flavour supplies.
 */
@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun VideoPlayerScreen(repository: WatchedRepository) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ALL
            playWhenReady = true
        }
    }
    val feed = remember { FeedController(context, repository, coroutineScope, player) }

    var showSettings by remember { mutableStateOf(false) }

    val pickVideo = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { feed.upload(it) }
    }
    val onUpload = { pickVideo.launch("video/*") }

    DisposableEffect(Unit) {
        onDispose { player.release() }
    }

    LaunchedEffect(Unit) {
        feed.next()
    }

    LaunchedEffect(Unit) {
        while (true) {
            feed.syncPlaybackState()
            delay(200)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FunnyColors.Background),
        contentAlignment = Alignment.Center
    ) {
        if (feed.videos.isEmpty()) {
            Text(
                if (feed.isLoading) "Loading..." else "No more videos available.",
                color = FunnyColors.Text
            )
        } else {
            AnimatedContent(
                targetState = feed.currentIndex,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInVertically(tween(500)) { height -> height } togetherWith
                            slideOutVertically(tween(500)) { height -> -height }
                    } else {
                        slideInVertically(tween(500)) { height -> -height } togetherWith
                            slideOutVertically(tween(500)) { height -> height }
                    }
                },
                label = "VideoScroll"
            ) { index ->
                if (index in feed.videos.indices) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                this.player = player
                                useController = false
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            AnimatedVisibility(
                visible = !feed.isPlaying && !showSettings,
                enter = fadeIn(tween(150)),
                exit = fadeOut(tween(300)),
                modifier = Modifier.align(Alignment.Center)
            ) {
                PausedIndicator()
            }

            AnimatedVisibility(
                visible = feed.voteFeedback != null,
                enter = fadeIn(tween(300)),
                exit = fadeOut(tween(500)),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Text(
                    text = feed.voteFeedback ?: "",
                    color = Color.White,
                    style = MaterialTheme.typography.displayLarge
                )
            }
        }

        PlatformControls(
            feed = feed,
            settingsOpen = showSettings,
            setSettingsOpen = { showSettings = it },
            onUpload = onUpload,
            modifier = Modifier.fillMaxSize()
        )

        AnimatedVisibility(
            visible = feed.status != null,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(400)),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            StatusPill(feed.status ?: "")
        }

        if (showSettings) {
            SettingsDialog(
                feed = feed,
                onUpload = onUpload,
                autoFocusUrl = isTvFlavour,
                onDismiss = { showSettings = false }
            )
        }
    }
}
