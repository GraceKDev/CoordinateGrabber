package com.example.coordinategrabber.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepository(
    private val dataStore: DataStore<Preferences>
) {
    private val darkMode = booleanPreferencesKey("dark_mode")
    private val userEmail = stringPreferencesKey("user_email")
    private val authToken = stringPreferencesKey("auth_token")

    val darkModeFlow: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[darkMode] ?: false
        }

    val userEmailFlow: Flow<String> = dataStore.data
        .map { preferences ->
            preferences[userEmail] ?: ""
        }

    val authTokenFlow: Flow<String> = dataStore.data
        .map { preferences ->
            preferences[authToken] ?: ""
        }

    val isLoggedInFlow: Flow<Boolean> = dataStore.data
        .map { preferences ->
            !preferences[authToken].isNullOrBlank()
        }

    suspend fun setDarkMode(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[darkMode] = enabled
        }
    }

    suspend fun saveAuthSession(email: String, token: String) {
        dataStore.edit { preferences ->
            preferences[userEmail] = email
            preferences[authToken] = token
        }
    }

    suspend fun clearAuthSession() {
        dataStore.edit { preferences ->
            preferences.remove(userEmail)
            preferences.remove(authToken)
        }
    }
}
