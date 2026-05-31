package com.example.recipebytes.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Create DataStore instance
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesRepository(private val context: Context) {

    companion object {
        private val DARK_MODE_KEY = booleanPreferencesKey("is_dark_mode")
        private val LAST_SCREEN_KEY = stringPreferencesKey("last_screen_id")
    }

    // Read dark mode preference as Flow (reactive)
    val isDarkModeFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[DARK_MODE_KEY] ?: false // default to light mode
    }

    // Read last screen preference as Flow
    val lastScreenFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[LAST_SCREEN_KEY] ?: "home" // default to home
    }

    // Save dark mode preference
    suspend fun setDarkMode(isDarkMode: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DARK_MODE_KEY] = isDarkMode
        }
    }

    // Save last screen preference
    suspend fun setLastScreen(screenId: String) {
        context.dataStore.edit { preferences ->
            preferences[LAST_SCREEN_KEY] = screenId
        }
    }

    // Clear all preferences (useful on logout)
    suspend fun clearPreferences() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
