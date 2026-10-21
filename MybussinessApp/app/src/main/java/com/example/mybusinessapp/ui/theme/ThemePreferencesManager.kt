package com.example.mybusinessapp.ui.theme

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_preferences")

class ThemePreferencesManager private constructor(private val context: Context) {

    companion object {
        val KEY_THEME_ACCENT = stringPreferencesKey("theme_accent_palette")
        val KEY_DARK_MODE = booleanPreferencesKey("theme_dark_mode")

        @Volatile
        private var instance: ThemePreferencesManager? = null

        fun getInstance(context: Context): ThemePreferencesManager {
            return instance ?: synchronized(this) {
                instance ?: ThemePreferencesManager(context.applicationContext).also { instance = it }
            }
        }
    }

    val themeAccentFlow: Flow<ThemeAccent> = context.themeDataStore.data
        .map { preferences ->
            val name = preferences[KEY_THEME_ACCENT] ?: ThemeAccent.SUNBURST_AMBER.name
            try {
                ThemeAccent.valueOf(name)
            } catch (e: Exception) {
                ThemeAccent.SUNBURST_AMBER
            }
        }
        .distinctUntilChanged()

    val darkModeFlow: Flow<Boolean> = context.themeDataStore.data
        .map { preferences ->
            preferences[KEY_DARK_MODE] ?: true // Default to true for rich dark aesthetic
        }
        .distinctUntilChanged()

    suspend fun setThemeAccent(accent: ThemeAccent) {
        context.themeDataStore.edit { preferences ->
            preferences[KEY_THEME_ACCENT] = accent.name
        }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.themeDataStore.edit { preferences ->
            preferences[KEY_DARK_MODE] = enabled
        }
    }
}
