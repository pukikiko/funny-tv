package com.pukikiko.funny.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.pukikiko.funny.SettingsActivity
import com.pukikiko.funny.data.FeedMode

/** Touch controls: swipe the feed, tap to pause, drag to seek. */
@Composable
fun PlatformControls(
    feed: FeedController,
    scroll: FeedScroll,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var volumeOpen by remember { mutableStateOf(false) }
    var shareOpen by remember { mutableStateOf(false) }

    val pickVideo = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { feed.upload(it) }
    }

    fun closePopups() {
        volumeOpen = false
        shareOpen = false
    }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        if (volumeOpen || shareOpen) closePopups() else feed.togglePlayPause()
                    }
                )
            }
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { delta -> scroll.onDrag(delta) },
                onDragStarted = { closePopups() },
                onDragStopped = { velocity -> scroll.onDragEnd(velocity) }
            )
    ) {
        ActionRail(
            feed = feed,
            volumeOpen = volumeOpen,
            shareOpen = shareOpen,
            onToggleVolume = {
                volumeOpen = !volumeOpen
                shareOpen = false
            },
            onToggleShare = {
                shareOpen = !shareOpen
                volumeOpen = false
            },
            onUpload = { pickVideo.launch("video/*") },
            onOpenSettings = {
                closePopups()
                context.startActivity(Intent(context, SettingsActivity::class.java))
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 40.dp)
        )

        Scrubber(
            feed = feed,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 8.dp)
        )
    }
}

@Composable
private fun ActionRail(
    feed: FeedController,
    volumeOpen: Boolean,
    shareOpen: Boolean,
    onToggleVolume: () -> Unit,
    onToggleShare: () -> Unit,
    onUpload: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        if (volumeOpen) {
            VolumeSlider(feed)
        }
        RailButton(
            icon = when {
                feed.volume == 0f -> Icons.Default.VolumeOff
                feed.volume < 0.5f -> Icons.Default.VolumeDown
                else -> Icons.Default.VolumeUp
            },
            description = "Volume",
            onClick = onToggleVolume
        )

        RailButton(
            icon = if (feed.feedMode == FeedMode.RANDOM) Icons.Default.Shuffle else Icons.Default.AutoAwesome,
            description = FeedMode.label(feed.feedMode),
            onClick = { feed.cycleMode() }
        )

        RailButton(
            icon = Icons.Default.ThumbUp,
            description = "Like",
            count = feed.current?.thumbs_up,
            tint = if (feed.votedAction == "up") FunnyColors.Success else FunnyColors.Text,
            onClick = { feed.vote("up") }
        )

        RailButton(
            icon = Icons.Default.ThumbDown,
            description = "Dislike",
            count = feed.current?.thumbs_down,
            tint = if (feed.votedAction == "down") FunnyColors.Danger else FunnyColors.Text,
            onClick = { feed.vote("down") }
        )

        if (shareOpen) {
            SharePopup(feed)
        }
        RailButton(
            icon = Icons.Default.Share,
            description = "Share",
            onClick = onToggleShare
        )

        RailButton(
            icon = Icons.Default.Add,
            description = "Upload",
            tint = FunnyColors.Accent,
            onClick = onUpload
        )

        RailButton(
            icon = Icons.Default.Settings,
            description = "Settings",
            onClick = onOpenSettings
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun RailButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    count: Int? = null,
    tint: Color = FunnyColors.Text
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .background(FunnyColors.Glass, CircleShape)
                .border(1.dp, FunnyColors.Border, CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(24.dp))
        }
        if (count != null) {
            Text("$count", color = FunnyColors.Text, style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** Vertical drag slider; top is full volume, bottom is muted. */
@Composable
private fun VolumeSlider(feed: FeedController) {
    Box(
        modifier = Modifier
            .width(46.dp)
            .height(130.dp)
            .background(FunnyColors.Glass, RoundedCornerShape(23.dp))
            .border(1.dp, FunnyColors.Border, RoundedCornerShape(23.dp))
            .padding(vertical = 12.dp, horizontal = 19.dp)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        feed.changeVolume(1f - offset.y / size.height)
                    }
                ) { change, _ ->
                    feed.changeVolume(1f - change.position.y / size.height)
                }
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(8.dp)
                .background(Color.White.copy(alpha = 0.25f), RoundedCornerShape(4.dp))
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxHeight(feed.volume)
                .width(8.dp)
                .background(FunnyColors.Accent, RoundedCornerShape(4.dp))
        )
    }
}

@Composable
private fun SharePopup(feed: FeedController) {
    Column(
        modifier = Modifier
            .background(FunnyColors.Glass, RoundedCornerShape(12.dp))
            .border(1.dp, FunnyColors.Border, RoundedCornerShape(12.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ShareOption(Icons.Default.ContentCopy, "Copy link") { feed.copyLink() }
        ShareOption(Icons.Default.Share, "Share") { feed.share() }
        ShareOption(Icons.Default.Download, "Download") { feed.download() }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ShareOption(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = label, tint = FunnyColors.Text, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = FunnyColors.Text, style = MaterialTheme.typography.labelLarge)
    }
}

/** Drag or tap anywhere along the bar to seek. */
@Composable
private fun Scrubber(feed: FeedController, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset -> feed.seekToFraction(offset.x / size.width) }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    feed.seekToFraction(change.position.x / size.width)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        ProgressBar(progress = feed.progress, height = 5.dp)
    }
}
