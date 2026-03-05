package app.balancebeacon.mobileandroid.feature.assistant.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.balancebeacon.mobileandroid.feature.assistant.model.AssistantSessionSnapshot
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import java.io.IOException

private val Context.assistantSessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "assistant_sessions")

interface AssistantSessionStore {
    suspend fun load(scopeKey: String): AssistantSessionSnapshot
    suspend fun save(scopeKey: String, snapshot: AssistantSessionSnapshot)
}

class DataStoreAssistantSessionStore(
    private val context: Context
) : AssistantSessionStore {
    override suspend fun load(scopeKey: String): AssistantSessionSnapshot {
        val preferences = context.assistantSessionDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .first()

        val raw = preferences[sessionsKey(scopeKey)].orEmpty()
        if (raw.isBlank()) {
            return AssistantSessionSnapshot()
        }

        return runCatching {
            json.decodeFromString(AssistantSessionSnapshot.serializer(), raw)
        }.getOrElse {
            AssistantSessionSnapshot()
        }
    }

    override suspend fun save(scopeKey: String, snapshot: AssistantSessionSnapshot) {
        context.assistantSessionDataStore.edit { preferences ->
            preferences[sessionsKey(scopeKey)] = json.encodeToString(AssistantSessionSnapshot.serializer(), snapshot)
        }
    }

    private fun sessionsKey(scopeKey: String): Preferences.Key<String> {
        val sanitized = scopeKey.lowercase().replace(Regex("[^a-z0-9_]"), "_")
        return stringPreferencesKey("assistant_sessions_$sanitized")
    }

    private companion object {
        val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
}
