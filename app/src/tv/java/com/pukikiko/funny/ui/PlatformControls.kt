package com.pukikiko.funny.ui

import android.view.KeyEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import kotlinx.coroutines.delay

private const val LONG_PRESS_MS = 600L
private const val SEEK_STEP_MS = 10_000L

/** D-pad driven controls for Android TV remotes. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PlatformControls(
    feed: FeedController,
    scroll: FeedScroll,
    modifier: Modifier = Modifier
) {
    var showControls by remember { mutableStateOf(true) }
    var showSettings by remember { mutableStateOf(false) }
    var centerDownTime by remember { mutableLongStateOf(0L) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(showSettings) {
        if (!showSettings) {
            runCatching { focusRequester.requestFocus() }
        }
    }

    LaunchedEffect(showControls, showSettings) {
        if (showControls && !showSettings) {
            delay(3000)
            showControls = false
        }
    }

    Box(
        modifier = modifier
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                val event = keyEvent.nativeKeyEvent

                if (showSettings) {
                    if (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_BACK) {
                        showSettings = false
                        return@onKeyEvent true
                    }
                    // Let the focus system drive the buttons inside the dialog.
                    return@onKeyEvent false
                }

                when (event.action) {
                    KeyEvent.ACTION_DOWN -> {
                        showControls = true
                        when (event.keyCode) {
                            KeyEvent.KEYCODE_DPAD_DOWN -> {
                                scroll.next()
                                true
                            }
                            KeyEvent.KEYCODE_DPAD_UP -> {
                                scroll.previous()
                                true
                            }
                            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                feed.vote("up")
                                true
                            }
                            KeyEvent.KEYCODE_DPAD_LEFT -> {
                                feed.vote("down")
                                true
                            }
                            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                                feed.seekBy(SEEK_STEP_MS)
                                true
                            }
                            KeyEvent.KEYCODE_MEDIA_REWIND -> {
                                feed.seekBy(-SEEK_STEP_MS)
                                true
                            }
                            KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_INFO -> {
                                showSettings = true
                                true
                            }
                            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                                if (event.repeatCount == 0) {
                                    centerDownTime = System.currentTimeMillis()
                                }
                                true
                            }
                            else -> false
                        }
                    }

                    KeyEvent.ACTION_UP -> {
                        val isCenter = event.keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                            event.keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                        if (isCenter) {
                            if (centerDownTime > 0) {
                                val heldFor = System.currentTimeMillis() - centerDownTime
                                if (heldFor > LONG_PRESS_MS) {
                                    showSettings = true
                                } else {
                                    feed.togglePlayPause()
                                }
                            }
                            centerDownTime = 0L
                            true
                        } else {
                            false
                        }
                    }

                    else -> false
                }
            }
    ) {
        AnimatedVisibility(
            visible = showControls && !showSettings,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(500)),
            modifier = Modifier.align(Alignment.BottomStart)
        ) {
            TvOverlay(feed)
        }

        if (showSettings) {
            TvSettingsDialog(feed = feed, onDismiss = { showSettings = false })
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TvOverlay(feed: FeedController) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp)
    ) {
        ProgressBar(progress = feed.progress, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                .padding(vertical = 12.dp, horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Hint {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ArrowUpward, "Prev", tint = Color.White, modifier = Modifier.size(16.dp))
                    Icon(Icons.Default.ArrowDownward, "Next", tint = Color.White, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text("Navigate", color = Color.White, style = MaterialTheme.typography.titleMedium)
            }

            Hint {
                Icon(
                    Icons.Default.ThumbDown,
                    "Left",
                    tint = if (feed.votedAction == "down") FunnyColors.Danger else Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "${feed.current?.thumbs_down ?: 0}",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.width(12.dp))
                Icon(
                    Icons.Default.ThumbUp,
                    "Right",
                    tint = if (feed.votedAction == "up") FunnyColors.Success else Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "${feed.current?.thumbs_up ?: 0}",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Hint {
                Icon(Icons.Default.PlayArrow, "Center", tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Play/Pause", color = Color.White, style = MaterialTheme.typography.titleMedium)
            }

            Hint {
                Icon(Icons.Default.Settings, "Long Center", tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Hold for Settings", color = Color.White, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun Hint(content: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) { content() }
}
