package dev.wearstral.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.wearstral.ui.ApiKeyState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = SettingsRepository(app)

    val state: StateFlow<ApiKeyState> = repository.key
        .map { key -> key?.let(ApiKeyState::Set) ?: ApiKeyState.Unset }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ApiKeyState.Loading)

    fun saveKey(value: String) = viewModelScope.launch { repository.setKey(value) }
    fun clearKey() = viewModelScope.launch { repository.clearKey() }
}
