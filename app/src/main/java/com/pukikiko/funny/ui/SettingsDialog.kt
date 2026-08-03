package com.pukikiko.funny.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import com.pukikiko.funny.data.FeedMode
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private const val DEFAULT_INSTANCE = "https://funny.mfc.pw"

/**
 * Shared settings surface. Every control here is reachable with a D-pad, so the
 * tv flavour gets the same feature set as the touch one.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SettingsDialog(
    feed: FeedController,
    onUpload: () -> Unit,
    autoFocusUrl: Boolean,
    onDismiss: () -> Unit
) {
    var editingUrl by remember { mutableStateOf(feed.instanceUrl) }
    val urlFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        if (autoFocusUrl) {
            // Let the dialog compose before grabbing focus.
            delay(50)
            runCatching { urlFocusRequester.requestFocus() }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .background(FunnyColors.Panel, RoundedCornerShape(16.dp))
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Settings", style = MaterialTheme.typography.headlineMedium, color = FunnyColors.Text)
            Spacer(Modifier.height(20.dp))

            Text("Server Instance URL", color = FunnyColors.Muted)
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                    .padding(16.dp)
            ) {
                BasicTextField(
                    value = editingUrl,
                    onValueChange = { editingUrl = it },
                    textStyle = TextStyle(color = FunnyColors.Text, fontSize = 20.sp),
                    modifier = Modifier
                        .width(320.dp)
                        .focusRequester(urlFocusRequester),
                    cursorBrush = SolidColor(FunnyColors.Accent)
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    feed.changeInstanceUrl(editingUrl)
                    onDismiss()
                }) {
                    Text("Apply URL")
                }
                Button(onClick = { editingUrl = DEFAULT_INSTANCE }) {
                    Text("Reset to Default")
                }
            }

            Spacer(Modifier.height(24.dp))

            Text("Feed mode", color = FunnyColors.Muted)
            Spacer(Modifier.height(8.dp))
            Button(onClick = { feed.cycleMode() }) {
                Text(FeedMode.label(feed.feedMode))
            }

            Spacer(Modifier.height(24.dp))

            Text("Volume — ${(feed.volume * 100).roundToInt()}%", color = FunnyColors.Muted)
            Spacer(Modifier.height(8.dp))
            ProgressBar(
                progress = feed.volume,
                modifier = Modifier.width(320.dp),
                height = 6.dp
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { feed.changeVolume(feed.volume - 0.1f) }) { Text("−") }
                Button(onClick = { feed.toggleMute() }) {
                    Text(if (feed.volume == 0f) "Unmute" else "Mute")
                }
                Button(onClick = { feed.changeVolume(feed.volume + 0.1f) }) { Text("+") }
            }

            Spacer(Modifier.height(24.dp))

            Text("This video", color = FunnyColors.Muted)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { feed.copyLink() }) { Text("Copy link") }
                Button(onClick = { feed.share() }) { Text("Share") }
                Button(onClick = { feed.download() }) { Text("Download") }
            }

            Spacer(Modifier.height(24.dp))
            Button(onClick = {
                onDismiss()
                onUpload()
            }) {
                Text("Upload a video")
            }

            Spacer(Modifier.height(20.dp))
            OutlinedButton(onClick = onDismiss) {
                Text("Close", color = FunnyColors.Text)
            }
        }
    }
}
