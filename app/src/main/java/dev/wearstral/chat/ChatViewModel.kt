package dev.wearstral.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.wearstral.settings.SettingsRepository
import dev.wearstral.voice.VoiceInputController
import dev.wearstral.voice.VoiceModelRepository
import dev.wearstral.voice.VoiceState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

class ChatViewModel(app: Application) : AndroidViewModel(app) {
    private val client = MistralClient()
    private val historyRepository = ChatHistoryRepository(app)
    private val settingsRepository = SettingsRepository(app)
    private var sendJob: Job? = null

    private val voiceLanguage = settingsRepository.voiceLanguage
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private val voiceController = VoiceInputController(VoiceModelRepository(app)) {
        voiceLanguage.value
    }

    val voiceState: StateFlow<VoiceState> = voiceController.state
    val voiceFinal: StateFlow<String?> = voiceController.finalText

    fun onVoiceTap() = voiceController.tap()
    fun onVoiceFinalConsumed() = voiceController.clearFinal()

    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()

    private val _activeConversationId = MutableStateFlow(System.currentTimeMillis())
    val activeConversationId: StateFlow<Long> = _activeConversationId.asStateFlow()

    val history: StateFlow<List<Conversation>> = historyRepository.history

    fun deleteConversation(id: Long) {
        sendJob?.cancel()
        historyRepository.delete(id)
        if (_activeConversationId.value == id) {
            _activeConversationId.value = System.currentTimeMillis()
            _state.update { ChatState() }
        }
    }

    fun newChat() {
        sendJob?.cancel()
        persistCurrent()
        _activeConversationId.value = System.currentTimeMillis()
        _state.update { ChatState() }
    }

    fun clearHistory() {
        sendJob?.cancel()
        historyRepository.clear()
        _activeConversationId.value = System.currentTimeMillis()
        _state.update { ChatState() }
    }

    fun send(apiKey: String, text: String) {
        val prompt = text.trim()
        android.util.Log.d(TAG, "send() prompt='${prompt.take(20)}' busy=${_state.value.isSending}")
        if (prompt.isEmpty() || _state.value.isSending) return

        _state.update {
            it.copy(
                messages = it.messages + ChatMessage(Role.User, prompt),
                isSending = true,
                error = null
            )
        }

        sendJob = viewModelScope.launch {
            try {
                val reply = client.chat(apiKey, _state.value.messages)
                android.util.Log.d(TAG, "success len=${reply.length}")
                _state.update {
                    it.copy(
                        messages = it.messages + ChatMessage(Role.Assistant, reply),
                        isSending = false
                    )
                }
                persistCurrent()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.d(TAG, "failure ${e.javaClass.simpleName}: ${e.message?.take(120)}")
                _state.update {
                    it.copy(
                        messages = it.messages.dropLast(1),
                        isSending = false,
                        error = errorFor(e)
                    )
                }
            }
        }
    }

    fun togglePin(id: Long) {
        val conversation = historyRepository.history.value.firstOrNull { it.id == id } ?: return
        historyRepository.setPinned(id, !conversation.pinned)
    }

    fun openConversation(id: Long) {
        sendJob?.cancel()
        val target = historyRepository.history.value.firstOrNull { it.id == id } ?: return
        _activeConversationId.value = target.id
        _state.update { ChatState(messages = target.messages) }
    }

    private fun persistCurrent() {
        val messages = _state.value.messages
        if (messages.isEmpty()) return
        historyRepository.upsert(
            Conversation(
                id = _activeConversationId.value,
                updatedAt = System.currentTimeMillis(),
                messages = messages
            )
        )
    }

    private fun errorFor(e: Exception): ChatError = when {
        e is MistralApiException -> when (e.code) {
            401, 402, 403 -> ChatError.InvalidKey
            429 -> ChatError.RateLimited
            in 500..599 -> ChatError.Server
            else -> ChatError.Unknown
        }
        e is IOException -> ChatError.Network
        else -> ChatError.Unknown
    }

    private companion object {
        const val TAG = "WearstralChat"
    }
}
