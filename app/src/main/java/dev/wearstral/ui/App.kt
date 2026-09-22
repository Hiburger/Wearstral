package dev.wearstral.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import dev.wearstral.R

@Composable
fun App(
    state: ApiKeyState,
    onSaveKey: (String) -> Unit,
    onClearKey: () -> Unit
) {
    when (state) {
        ApiKeyState.Loading -> AppScaffold(timeText = { TimeText() }) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        ApiKeyState.Unset -> KeyScreen(
            currentKey = null,
            onSave = onSaveKey,
            onClear = onClearKey
        )
        is ApiKeyState.Set -> {
            val apiKey = state.key
            val navController = rememberSwipeDismissableNavController()
            SwipeDismissableNavHost(
                navController = navController,
                startDestination = "chat"
            ) {
                composable("chat") {
                    AppScaffold(timeText = { TimeText() }) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = stringResource(R.string.chat_placeholder))
                                Button(
                                    onClick = { navController.navigate("settings") },
                                    modifier = Modifier.padding(top = 12.dp)
                                ) {
                                    Text(text = stringResource(R.string.settings))
                                }
                            }
                        }
                    }
                }
                composable("settings") {
                    KeyScreen(
                        currentKey = apiKey,
                        onSave = onSaveKey,
                        onClear = onClearKey
                    )
                }
            }
        }
    }
}
