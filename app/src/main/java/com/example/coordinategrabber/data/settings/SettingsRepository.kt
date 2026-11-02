package com.example.coordinategrabber.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepository(
    private val dataStore: DataStore<Preferences>
) {
    private val darkMode = booleanPreferencesKey("dark_mode")

    val darkModeFlow: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[darkMode] ?: false }

    suspend fun setDarkMode(enabled: Boolean) {
        dataStore.edit {preferences ->
            preferences[darkMode] = enabled
        }
    }
}