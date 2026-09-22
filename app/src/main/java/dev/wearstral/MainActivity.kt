package dev.wearstral

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.wear.compose.material3.MaterialTheme
import dev.wearstral.settings.SettingsViewModel
import dev.wearstral.ui.App

class MainActivity : ComponentActivity() {
    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                App(
                    state = viewModel.state.collectAsState().value,
                    onSaveKey = viewModel::saveKey,
                    onClearKey = viewModel::clearKey
                )
            }
        }
    }
}
