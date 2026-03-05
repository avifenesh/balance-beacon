package app.balancebeacon.mobileandroid.core.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "session")

data class AuthTokens(
    val accessToken: String,
    val refreshToken: String?
)

class SessionStore(
    private val context: Context
) {
    fun observeTokens(): Flow<AuthTokens?> {
        return context.sessionDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                val accessToken = preferences[ACCESS_TOKEN]
                if (accessToken.isNullOrBlank()) {
                    null
                } else {
                    AuthTokens(
                        accessToken = accessToken,
                        refreshToken = preferences[REFRESH_TOKEN]
                    )
                }
            }
    }

    suspend fun saveTokens(tokens: AuthTokens) {
        context.sessionDataStore.edit { preferences ->
            preferences[ACCESS_TOKEN] = tokens.accessToken
            if (tokens.refreshToken.isNullOrBlank()) {
                preferences.remove(REFRESH_TOKEN)
            } else {
                preferences[REFRESH_TOKEN] = tokens.refreshToken
            }
        }
    }

    suspend fun clearTokens() {
        context.sessionDataStore.edit { preferences ->
            preferences.remove(ACCESS_TOKEN)
            preferences.remove(REFRESH_TOKEN)
        }
    }

    private companion object {
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
    }
}
