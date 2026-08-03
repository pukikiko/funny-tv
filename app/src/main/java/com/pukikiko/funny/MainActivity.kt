package com.pukikiko.funny

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.pukikiko.funny.data.WatchedRepository
import com.pukikiko.funny.ui.MobilePlayerScreen
import com.pukikiko.funny.ui.TvPlayerScreen
import com.pukikiko.funny.ui.VideoPlayerViewModel

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = WatchedRepository(this)
        
        val uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        val isTv = uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
        
        setContent {
            val factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return VideoPlayerViewModel(repository) as T
                }
            }
            val viewModel: VideoPlayerViewModel = viewModel(factory = factory)

            if (isTv) {
                androidx.tv.material3.MaterialTheme {
                    androidx.tv.material3.Surface(shape = androidx.compose.ui.graphics.RectangleShape) {
                        TvPlayerScreen(viewModel)
                    }
                }
            } else {
                androidx.compose.material3.MaterialTheme {
                    androidx.compose.material3.Surface {
                        MobilePlayerScreen(viewModel)
                    }
                }
            }
        }
    }
}
