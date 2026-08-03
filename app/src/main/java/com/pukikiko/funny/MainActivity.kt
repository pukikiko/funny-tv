package com.pukikiko.funny

import android.os.Bundle
import kotlin.OptIn
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import com.pukikiko.funny.data.WatchedRepository
import com.pukikiko.funny.ui.VideoPlayerScreen

@OptIn(ExperimentalTvMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = WatchedRepository(this)
        
        setContent {
            MaterialTheme {
                Surface(shape = androidx.compose.ui.graphics.RectangleShape) {
                    VideoPlayerScreen(repository)
                }
            }
        }
    }
}
