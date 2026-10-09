package dev.wearstral.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import dev.wearstral.R
import dev.wearstral.chat.ChatState
import dev.wearstral.chat.Conversation
import dev.wearstral.voice.VoiceLanguageUi
import dev.wearstral.voice.VoiceState
import kotlinx.coroutines.flow.StateFlow

/**
 * Nav destinations collect the flows themselves instead of receiving snapshot
 * values captured in the destination lambda: SwipeDismissableNavHost does not
 * reliably re-invoke destination content when only the captured values change,
 * which used to freeze the chat screen on stale state until re-navigation.
 */
@Composable
fun App(
    keyStateFlow: StateFlow<ApiKeyState>,
    chatStateFlow: StateFlow<ChatState>,
    nostalgicModeFlow: StateFlow<Boolean>,
    historyFlow: StateFlow<List<Conversation>>,
    activeConversationIdFlow: StateFlow<Long>,
    voiceRowsFlow: StateFlow<List<VoiceLanguageUi>>,
    voiceStateFlow: StateFlow<VoiceState>,
    voiceFinalFlow: StateFlow<String?>,
    onSend: (String, String) -> Unit,
    onNewChat: () -> Unit,
    onOpenConversation: (Long) -> Unit,
    onDeleteConversation: (Long) -> Unit,
    onTogglePin: (Long) -> Unit,
    onClearHistory: () -> Unit,
    onSaveKey: (String) -> Unit,
    onClearKey: () -> Unit,
    onSetNostalgic: (Boolean) -> Unit,
    onVoiceTap: () -> Unit,
    onVoiceFinalConsumed: () -> Unit,
    onVoiceLanguageTap: (String) -> Unit,
    onVoiceLanguageDelete: (String) -> Unit
) {
    val nostalgic by nostalgicModeFlow.collectAsState()
    val keyState by keyStateFlow.collectAsState()
    when (keyState) {
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
            val navController = rememberSwipeDismissableNavController()
            var openPanelOnChat by remember { mutableStateOf(false) }
            SwipeDismissableNavHost(
                navController = navController,
                startDestination = "chat"
            ) {
                composable("chat") {
                    val destKeyState by keyStateFlow.collectAsState()
                    val chatState by chatStateFlow.collectAsState()
                    val history by historyFlow.collectAsState()
                    val activeId by activeConversationIdFlow.collectAsState()
                    val voiceState by voiceStateFlow.collectAsState()
                    val voiceFinal by voiceFinalFlow.collectAsState()
                    val voiceRows by voiceRowsFlow.collectAsState()
                    val apiKey = (destKeyState as? ApiKeyState.Set)?.key
                    if (apiKey != null) {
                        ChatScreen(
                            state = chatState,
                            nostalgic = nostalgic,
                            history = history,
                            activeId = activeId,
                            voiceState = voiceState,
                            voiceFinal = voiceFinal,
                            voiceReady = voiceRows.any { it.active && it.downloaded },
                            onSend = { onSend(apiKey, it) },
                            onNewChat = onNewChat,
                            onOpenSettings = { navController.navigate("settings") },
                            onOpenHistory = { navController.navigate("history") },
                            onOpenConversation = onOpenConversation,
                            onVoiceTap = onVoiceTap,
                            onVoiceFinalConsumed = onVoiceFinalConsumed,
                            openPanelOnChat = openPanelOnChat,
                            onPanelConsumed = { openPanelOnChat = false }
                        )
                    }
                }
                composable("history") {
                    LaunchedEffect(Unit) { openPanelOnChat = true }
                    val history by historyFlow.collectAsState()
                    HistoryScreen(
                        history = history,
                        onOpen = { id ->
                            openPanelOnChat = false
                            onOpenConversation(id)
                            navController.popBackStack()
                        },
                        onDelete = onDeleteConversation,
                        onTogglePin = onTogglePin
                    )
                }
                composable("info") {
                    val voiceRows by voiceRowsFlow.collectAsState()
                    val context = LocalContext.current
                    val version = try {
                        context.packageManager
                            .getPackageInfo(context.packageName, 0).versionName ?: "?"
                    } catch (_: Exception) {
                        "?"
                    }
                    InfoScreen(
                        version = version,
                        modelName = "mistral-small-latest",
                        nostalgic = nostalgic,
                        voiceModel = voiceRows.firstOrNull { it.active }?.language?.name
                    )
                }
                composable("voice") {
                    val rows by voiceRowsFlow.collectAsState()
                    VoiceScreen(
                        rows = rows,
                        onLanguageTap = onVoiceLanguageTap,
                        onDeleteLanguage = onVoiceLanguageDelete
                    )
                }
                composable("settings") {
                    SettingsScreen(
                        nostalgic = nostalgic,
                        onToggleNostalgic = onSetNostalgic,
                        onOpenApiKey = { navController.navigate("key") },
                        onOpenVoice = { navController.navigate("voice") },
                        onClearHistory = onClearHistory,
                        onOpenInfo = { navController.navigate("info") }
                    )
                }
                composable("key") {
                    val destKeyState by keyStateFlow.collectAsState()
                    KeyScreen(
                        currentKey = (destKeyState as? ApiKeyState.Set)?.key,
                        onSave = onSaveKey,
                        onClear = onClearKey
                    )
                }
            }
        }
    }
}
