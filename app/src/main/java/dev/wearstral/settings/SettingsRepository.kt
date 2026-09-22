package dev.wearstral.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    val key: Flow<String?> = context.dataStore.data.map { prefs -> prefs[KEY_API] }

    suspend fun setKey(value: String) {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return
        context.dataStore.edit { prefs -> prefs[KEY_API] = trimmed }
    }

    suspend fun clearKey() {
        context.dataStore.edit { prefs -> prefs.remove(KEY_API) }
    }

    private companion object {
        val KEY_API = stringPreferencesKey("api_key")
    }
}
