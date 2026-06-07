package com.example.recipebytes.preferences

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesRepository(private val context: Context) {

    companion object {
        private val DARK_MODE_KEY = booleanPreferencesKey("is_dark_mode")
        private val LAST_SCREEN_KEY = stringPreferencesKey("last_screen_id")

        fun initTheme(context: Context) {
            runBlocking {
                val prefs = context.dataStore.data.first()
                val isDarkMode = prefs[DARK_MODE_KEY]
                val mode = when (isDarkMode) {
                    null  -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                    true  -> AppCompatDelegate.MODE_NIGHT_YES
                    false -> AppCompatDelegate.MODE_NIGHT_NO
                }
                AppCompatDelegate.setDefaultNightMode(mode)
            }
        }
    }

    val isDarkModeFlow: Flow<Boolean?> = context.dataStore.data.map { preferences ->
        preferences[DARK_MODE_KEY] // null = not set by user yet
    }

    val lastScreenFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[LAST_SCREEN_KEY] ?: "home"
    }

    suspend fun setDarkMode(isDarkMode: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DARK_MODE_KEY] = isDarkMode
        }
    }

    suspend fun setLastScreen(screenId: String) {
        context.dataStore.edit { preferences ->
            preferences[LAST_SCREEN_KEY] = screenId
        }
    }

    suspend fun clearPreferences() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
