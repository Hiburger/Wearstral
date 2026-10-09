package dev.wearstral.settings

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * the following takes care of storing the API key encrypted at rest (see [ApiKeyCipher]);
 * only ciphertext ever touches the disk.
 *
 * if there0s an unreadable stored value, it is discarded so the user is
 * simply asked for the key again instead of the app crashing, and a save that
 * cannot be encrypted is a logged no-op rather than a crash.
 */
class SettingsRepository(private val context: Context) {

    val key: Flow<String?> = context.dataStore.data.map { prefs -> readKey(prefs) }

    suspend fun setKey(value: String) {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return
        val encrypted = runCatching { ApiKeyCipher.encrypt(trimmed) }
            .getOrElse { e ->
                Log.w(TAG, "Could not encrypt the API key! Nothing got saved.", e)
                return
            }
        context.dataStore.edit { prefs ->
            prefs[KEY_API_ENCRYPTED] = encrypted
            prefs.remove(KEY_API_PLAINTEXT_LEGACY)
        }
    }

    suspend fun clearKey() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_API_ENCRYPTED)
            prefs.remove(KEY_API_PLAINTEXT_LEGACY)
        }
    }

    val nostalgicMode: Flow<Boolean> =
        context.dataStore.data.map { prefs -> prefs[KEY_NOSTALGIC] ?: false }

    suspend fun setNostalgicMode(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_NOSTALGIC] = enabled }
    }

    val voiceLanguage: Flow<String?> =
        context.dataStore.data.map { prefs -> prefs[KEY_VOICE_LANGUAGE] }

    suspend fun setVoiceLanguage(languageId: String) {
        context.dataStore.edit { prefs -> prefs[KEY_VOICE_LANGUAGE] = languageId }
    }

    suspend fun clearVoiceLanguage() {
        context.dataStore.edit { prefs -> prefs.remove(KEY_VOICE_LANGUAGE) }
    }

    private suspend fun readKey(prefs: Preferences): String? {
        val encrypted = prefs[KEY_API_ENCRYPTED] ?: return null
        return runCatching { ApiKeyCipher.decrypt(encrypted) }
            .getOrElse { e ->
                Log.w(TAG, "Stored API key is unreadable! Discarding it", e)
                context.dataStore.edit { p -> p.remove(KEY_API_ENCRYPTED) }
                null
            }
    }

    private companion object {
        const val TAG = "WearstralSettings"
        val KEY_API_ENCRYPTED = stringPreferencesKey("api_key_encrypted")
        val KEY_NOSTALGIC = booleanPreferencesKey("nostalgic_mode")
        val KEY_VOICE_LANGUAGE = stringPreferencesKey("voice_language")

        @Deprecated("pre-encryption storage; removed on the next save or clear")
        val KEY_API_PLAINTEXT_LEGACY = stringPreferencesKey("api_key")
    }
}
