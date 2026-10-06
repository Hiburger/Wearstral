package dev.wearstral

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.wear.compose.material3.MaterialTheme
import dev.wearstral.chat.ChatViewModel
import dev.wearstral.settings.SettingsViewModel
import dev.wearstral.ui.App
import dev.wearstral.update.ApkInstaller
import dev.wearstral.voice.VoiceViewModel

class MainActivity : ComponentActivity() {
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val chatViewModel: ChatViewModel by viewModels()
    private val voiceViewModel: VoiceViewModel by viewModels()

    private val debugSendReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val text = intent.getStringExtra("text") ?: return
            when (intent.action) {
                "dev.wearstral.DEBUG_KEY" -> settingsViewModel.saveKey(text)
                "dev.wearstral.DEBUG_SEND" -> {
                    val key = settingsViewModel.state.value
                    if (key is dev.wearstral.ui.ApiKeyState.Set) {
                        chatViewModel.send(key.key, text)
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ApkInstaller.cleanup(this)
        if (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            ContextCompat.registerReceiver(
                this,
                debugSendReceiver,
                IntentFilter().apply {
                    addAction("dev.wearstral.DEBUG_SEND")
                    addAction("dev.wearstral.DEBUG_KEY")
                },
                ContextCompat.RECEIVER_EXPORTED
            )
        }
        setContent {
            MaterialTheme {
                App(
                    keyStateFlow = settingsViewModel.state,
                    chatStateFlow = chatViewModel.state,
                    nostalgicModeFlow = settingsViewModel.nostalgicMode,
                    historyFlow = chatViewModel.history,
                    activeConversationIdFlow = chatViewModel.activeConversationId,
                    voiceRowsFlow = voiceViewModel.rows,
                    voiceStateFlow = chatViewModel.voiceState,
                    voiceFinalFlow = chatViewModel.voiceFinal,
                    onSend = chatViewModel::send,
                    onNewChat = chatViewModel::newChat,
                    onOpenConversation = chatViewModel::openConversation,
                    onDeleteConversation = chatViewModel::deleteConversation,
                    onTogglePin = chatViewModel::togglePin,
                    onClearHistory = chatViewModel::clearHistory,
                    onSaveKey = settingsViewModel::saveKey,
                    onClearKey = settingsViewModel::clearKey,
                    onSetNostalgic = settingsViewModel::setNostalgicMode,
                    onVoiceTap = chatViewModel::onVoiceTap,
                    onVoiceFinalConsumed = chatViewModel::onVoiceFinalConsumed,
                    onVoiceLanguageTap = voiceViewModel::onLanguageTap
                )
            }
        }
    }
}
