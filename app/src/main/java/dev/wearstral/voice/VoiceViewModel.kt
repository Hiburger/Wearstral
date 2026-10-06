package dev.wearstral.voice

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.wearstral.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class VoiceLanguageUi(
    val language: VoiceLanguage,
    val downloaded: Boolean,
    val active: Boolean,
    val progress: Int?,
    val failed: Boolean
)

class VoiceViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = VoiceModelRepository(app)
    private val settings = SettingsRepository(app)

    private val progress = MutableStateFlow<Pair<String, Int>?>(null)
    private val failedId = MutableStateFlow<String?>(null)
    private val refresh = MutableStateFlow(0)
    private var downloadJob: Job? = null

    private val activeLanguage = settings.voiceLanguage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val rows: StateFlow<List<VoiceLanguageUi>> = combine(
        activeLanguage,
        progress.asStateFlow(),
        failedId.asStateFlow(),
        refresh
    ) { active, downloadProgress, failed, _ ->
        buildRows(active, downloadProgress, failed)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private suspend fun buildRows(
        active: String?,
        downloadProgress: Pair<String, Int>?,
        failed: String?
    ): List<VoiceLanguageUi> = withContext(Dispatchers.IO) {
        val downloaded = repository.downloadedLanguageIds()
        repository.languages.map { language ->
            VoiceLanguageUi(
                language = language,
                downloaded = language.id in downloaded,
                active = language.id == active,
                progress = downloadProgress?.takeIf { it.first == language.id }?.second,
                failed = language.id == failed
            )
        }
    }

    fun onLanguageTap(languageId: String) {
        if (downloadJob?.isActive == true) return
        val language = repository.languageById(languageId) ?: return
        failedId.value = null
        downloadJob = viewModelScope.launch {
            // the on-disk probe happens off the main thread
            val modelDir = withContext(Dispatchers.IO) { repository.modelDir(languageId) }
            if (modelDir != null) {
                settings.setVoiceLanguage(languageId)
                return@launch
            }
            progress.value = languageId to 0
            try {
                repository.download(language) { percent ->
                    progress.value = languageId to percent
                }
                settings.setVoiceLanguage(languageId)
            } catch (e: Exception) {
                android.util.Log.d(
                    "WearstralVoice",
                    "download failed ${e.javaClass.simpleName}: ${e.message?.take(120)}"
                )
                failedId.value = languageId
            }
            progress.value = null
            refresh.value++
        }
    }
}
