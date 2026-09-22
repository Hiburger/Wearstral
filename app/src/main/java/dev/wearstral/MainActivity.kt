package dev.wearstral

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.wear.compose.material3.MaterialTheme
import dev.wearstral.settings.SettingsRepository
import dev.wearstral.ui.App
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val repository = remember { SettingsRepository(applicationContext) }
                val apiKey by repository.key.collectAsState(initial = null)
                val scope = rememberCoroutineScope()
                App(
                    apiKey = apiKey,
                    onSaveKey = { value -> scope.launch { repository.setKey(value) } },
                    onClearKey = { scope.launch { repository.clearKey() } }
                )
            }
        }
    }
}
