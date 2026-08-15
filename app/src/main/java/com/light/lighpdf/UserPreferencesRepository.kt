package com.light.lighpdf

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

data class UserPreferences(
    val isDarkMode: Boolean,
    val isContinuousMode: Boolean
)

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        val IS_CONTINUOUS_MODE = booleanPreferencesKey("is_continuous_mode")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val isDarkMode = preferences[PreferencesKeys.IS_DARK_MODE] ?: false
            val isContinuousMode = preferences[PreferencesKeys.IS_CONTINUOUS_MODE] ?: false
            UserPreferences(isDarkMode, isContinuousMode)
        }

    suspend fun updateDarkMode(isDarkMode: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_DARK_MODE] = isDarkMode
        }
    }

    suspend fun updateContinuousMode(isContinuousMode: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_CONTINUOUS_MODE] = isContinuousMode
        }
    }
}
