package com.pukikiko.funny.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.pukikiko.funny.data.WatchedRepository
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * The parts of the feed that look the same everywhere: the player surfaces,
 * the paused indicator, vote feedback and status messages. Input and on-screen
 * controls come from [PlatformControls], which each flavour supplies.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun VideoPlayerScreen(repository: WatchedRepository) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val feed = remember { FeedController(context, repository, coroutineScope) }

    DisposableEffect(Unit) {
        onDispose { feed.release() }
    }

    LaunchedEffect(Unit) {
        feed.start()
    }

    LaunchedEffect(Unit) {
        while (true) {
            feed.syncPlaybackState()
            delay(200)
        }
    }

    // Settings live in a separate screen on mobile, so pick up any change on return.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) feed.refreshFromPrefs()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(FunnyColors.Background),
        contentAlignment = Alignment.Center
    ) {
        val heightPx = with(LocalDensity.current) { maxHeight.toPx() }
        val scroll = remember(heightPx) { FeedScroll(feed, coroutineScope, heightPx) }

        // Auto-scroll reaches the next video through the same animated swipe.
        DisposableEffect(scroll) {
            feed.onAutoScroll = { scroll.next() }
            onDispose { feed.onAutoScroll = null }
        }

        if (feed.videos.isEmpty()) {
            Text(
                if (feed.isLoading) "Loading..." else "No more videos available.",
                color = FunnyColors.Text
            )
        } else {
            // Each surface keeps its own player for the whole session; only the
            // offsets change, so swapping never re-attaches a video surface.
            feed.players.forEachIndexed { slot, player ->
                val restingOffset = if (slot == feed.activeSlot) 0f else heightPx
                PlayerSurface(
                    player = player,
                    modifier = Modifier
                        .fillMaxSize()
                        .offset {
                            IntOffset(0, (scroll.offset.value + restingOffset).roundToInt())
                        }
                )
            }

            AnimatedVisibility(
                visible = !feed.isPlaying,
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
            scroll = scroll,
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
    }
}

@Composable
private fun PlayerSurface(player: ExoPlayer, modifier: Modifier = Modifier) {
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                useController = false
                setBackgroundColor(android.graphics.Color.BLACK)
            }
        },
        update = { it.player = player },
        modifier = modifier
    )
}
