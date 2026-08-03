package com.pukikiko.funny

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pukikiko.funny.data.FeedMode
import com.pukikiko.funny.data.WatchedRepository
import kotlin.math.roundToInt

private const val DEFAULT_INSTANCE = "https://funny.mfc.pw"

private val FunnyDarkColors = darkColorScheme(
    primary = Color(0xFFEC4899),
    onPrimary = Color(0xFF1A0512),
    background = Color(0xFF020617),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF0F172A),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF94A3B8)
)

/**
 * Settings as a normal Android screen. The feed picks these up in onResume, so
 * anything changed here applies as soon as you come back.
 */
class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = WatchedRepository(this)
        setContent {
            MaterialTheme(colorScheme = FunnyDarkColors) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SettingsScreen(repository, onBack = { finish() })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(repository: WatchedRepository, onBack: () -> Unit) {
    var instanceUrl by remember { mutableStateOf(repository.getBaseUrl()) }
    var feedMode by remember { mutableStateOf(repository.getFeedMode()) }
    var volume by remember { mutableFloatStateOf(repository.getVolume()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            SectionTitle("Instance")
            OutlinedTextField(
                value = instanceUrl,
                onValueChange = { instanceUrl = it },
                label = { Text("Server URL") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = {
                    repository.setBaseUrl(instanceUrl.trim())
                    onBack()
                }) {
                    Text("Save")
                }
                OutlinedButton(onClick = { instanceUrl = DEFAULT_INSTANCE }) {
                    Text("Reset to default")
                }
            }

            SectionDivider()

            SectionTitle("Feed mode")
            Column(Modifier.selectableGroup()) {
                FeedMode.ALL.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = feedMode == mode,
                                role = Role.RadioButton,
                                onClick = {
                                    feedMode = mode
                                    repository.setFeedMode(mode)
                                }
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = feedMode == mode, onClick = null)
                        Spacer(Modifier.width(12.dp))
                        Text(FeedMode.label(mode), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            SectionDivider()

            SectionTitle("Volume — ${(volume * 100).roundToInt()}%")
            Slider(
                value = volume,
                onValueChange = {
                    volume = it
                    repository.setVolume(it)
                }
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(vertical = 12.dp)
    )
}

@Composable
private fun SectionDivider() {
    Spacer(Modifier.height(12.dp))
    Divider()
}
