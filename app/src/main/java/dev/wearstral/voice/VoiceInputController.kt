package dev.wearstral.voice

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService

sealed class VoiceState {
    data object Idle : VoiceState()
    data object Preparing : VoiceState()
    data class Listening(val partial: String) : VoiceState()
    data class Failed(val reason: Reason) : VoiceState()

    enum class Reason { NoModel, MicError }
}

/**
 * Runs one speech-recognition session at a time with the active Vosk model.
 * The native Model is loaded for the duration of the session only (watches are
 * usually RAM tight) and closed after the session ends. Final text is emitted through
 * [finalText] and a partial hypotheses stream through [state]
 *
 * Native teardown is deliberately asynchronous: Vosk's decoder thread keeps
 * touching the recognizer after [SpeechService.stop], so freeing immediately
 * from the callback path corrupts the heap. References are
 * grabbed and nulled first then closed after a grace delay.
 */

class VoiceInputController(
    private val repository: VoiceModelRepository,
    private val activeLanguageId: () -> String?
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow<VoiceState>(VoiceState.Idle)
    val state: StateFlow<VoiceState> = _state.asStateFlow()

    private val _finalText = MutableStateFlow<String?>(null)
    val finalText: StateFlow<String?> = _finalText.asStateFlow()

    private var activeService: SpeechService? = null
    private var sessionRecognizer: Recognizer? = null
    private var sessionModel: Model? = null
    private var timeoutJob: Job? = null

    fun tap() {
        Log.d(TAG, "tap() state=${_state.value}")
        when (_state.value) {
            is VoiceState.Listening, VoiceState.Preparing -> stop()
            else -> start()
        }
    }

    private fun start() {
        val languageId = activeLanguageId()
            ?: run { _state.value = VoiceState.Failed(VoiceState.Reason.NoModel); return }
        val modelDir = repository.modelDir(languageId)
            ?: run { _state.value = VoiceState.Failed(VoiceState.Reason.NoModel); return }

        Log.d(TAG, "start() lang=$languageId dir=${modelDir.name}")
        _state.value = VoiceState.Preparing
        scope.launch {
            var loadedModel: Model? = null
            var loadedRecognizer: Recognizer? = null
            try {
                loadedModel = withContext(Dispatchers.Default) { Model(modelDir.absolutePath) }
                loadedRecognizer = withContext(Dispatchers.Default) {
                    Recognizer(loadedModel, SAMPLE_RATE)
                }
                if (_state.value != VoiceState.Preparing) {
                    runCatching { loadedRecognizer?.close() }
                    runCatching { loadedModel?.close() }
                    return@launch
                }
                sessionModel = loadedModel
                sessionRecognizer = loadedRecognizer
                activeService = SpeechService(loadedRecognizer, SAMPLE_RATE).also { service ->
                    service.startListening(listener)
                }
                Log.d(TAG, "listening started")
                _state.value = VoiceState.Listening(partial = "")
                timeoutJob = scope.launch {
                    delay(MAX_DURATION_MS)
                    stop()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                // Throwable, not Exception: a native lib that cannot load
                //is an error and must degrade to a hint instead of killing the process
                Log.d(TAG, "voice start failed ${t.javaClass.simpleName}: ${t.message?.take(120)}")
                runCatching { loadedRecognizer?.close() }
                runCatching { loadedModel?.close() }
                _state.value = VoiceState.Failed(VoiceState.Reason.MicError)
            }
        }
    }

    fun stop() {
        Log.d(TAG, "stop() state=${_state.value}")
        timeoutJob?.cancel()
        timeoutJob = null
        val service = activeService
        val recognizer = sessionRecognizer
        val model = sessionModel
        activeService = null
        sessionRecognizer = null
        sessionModel = null
        _state.value = VoiceState.Idle
        if (service == null && recognizer == null) return
        shutdown(service, recognizer, model)
    }

    fun clearFinal() {
        _finalText.value = null
    }

    private val listener = object : RecognitionListener {
        override fun onPartialResult(hypothesis: String?) {
            val text = hypothesis?.let { runCatching { JSONObject(it).optString("partial") }.getOrNull() }
            if (_state.value is VoiceState.Listening) {
                _state.value = VoiceState.Listening(partial = text.orEmpty())
            }
        }

        override fun onResult(hypothesis: String?) = emitFinal(hypothesis)

        override fun onFinalResult(hypothesis: String?) = emitFinal(hypothesis)

        override fun onError(e: Exception?) {
            Log.d(TAG, "voice error: ${e?.message?.take(120)}")
            val service = activeService
            val recognizer = sessionRecognizer
            val model = sessionModel
            activeService = null
            sessionRecognizer = null
            sessionModel = null
            timeoutJob?.cancel()
            timeoutJob = null
            _state.value = VoiceState.Failed(VoiceState.Reason.MicError)
            shutdown(service, recognizer, model)
        }

        override fun onTimeout() = stop()
    }

    private fun emitFinal(hypothesis: String?) {
        val text = hypothesis
            ?.let { runCatching { JSONObject(it).optString("text") }.getOrNull() }
            .orEmpty()
        Log.d(TAG, "final: '$text'")
        _finalText.value = text
        if (_state.value !is VoiceState.Idle) {
            _state.value = VoiceState.Idle
        }
    }

    // Frees Vosk natives only after the decoder thread has wound down
    private fun shutdown(service: SpeechService?, recognizer: Recognizer?, model: Model?) {
        scope.launch {
            try {
                service?.stop()
            } catch (e: Exception) {
                Log.d(TAG, "voice stop failed: ${e.message?.take(120)}")
            }
            delay(TEARDOWN_GRACE_MS)
            try {
                recognizer?.close()
            } catch (_: Exception) {
            }
            try {
                model?.close()
            } catch (_: Exception) {
            }
        }
    }

    private companion object {
        const val TAG = "WearstralVoice"
        const val SAMPLE_RATE = 16000f
        const val MAX_DURATION_MS = 20_000L
        const val TEARDOWN_GRACE_MS = 1_500L
    }
}
