package dev.wearstral.ui

sealed interface ApiKeyState {
    data object Loading : ApiKeyState
    data object Unset : ApiKeyState
    data class Set(val key: String) : ApiKeyState
}
