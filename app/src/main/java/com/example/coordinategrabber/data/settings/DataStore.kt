package com.example.coordinategrabber.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/**
 * Single DataStore instance shared by the Application and the UI layer.
 * Declaring this in more than one place throws "multiple DataStores active".
 */
val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
