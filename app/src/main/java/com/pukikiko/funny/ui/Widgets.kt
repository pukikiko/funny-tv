package com.pukikiko.funny.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

/** The web player's palette, so both front ends look like the same product. */
object FunnyColors {
    val Background = Color(0xFF020617)
    val Panel = Color(0xE60F172A)
    val Border = Color(0x1AFFFFFF)
    val Text = Color(0xFFF8FAFC)
    val Muted = Color(0xFF94A3B8)
    val Accent = Color(0xFFEC4899)
    val Success = Color(0xFF10B981)
    val Danger = Color(0xFFEF4444)
}

@Composable
fun ProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 4.dp,
    color: Color = FunnyColors.Accent
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(height / 2))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(color, RoundedCornerShape(height / 2))
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun StatusPill(message: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(24.dp)
            .background(FunnyColors.Panel, RoundedCornerShape(24.dp))
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Text(message, color = FunnyColors.Text, style = MaterialTheme.typography.titleMedium)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PausedIndicator(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(88.dp)
            .background(Color.Black.copy(alpha = 0.45f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.PlayArrow,
            contentDescription = "Paused",
            tint = FunnyColors.Text,
            modifier = Modifier.size(48.dp)
        )
    }
}
