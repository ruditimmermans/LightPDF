package com.light.lighpdf

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

data class RecentFile(
    val uriString: String,
    val name: String,
    val timestamp: Long
)

data class UserPreferences(
    val isDarkMode: Boolean,
    val isContinuousMode: Boolean,
    val recentFiles: List<RecentFile> = emptyList()
)

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        val IS_CONTINUOUS_MODE = booleanPreferencesKey("is_continuous_mode")
        val RECENT_FILES_JSON = stringPreferencesKey("recent_files_json")
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
            val recentFilesJson = preferences[PreferencesKeys.RECENT_FILES_JSON] ?: "[]"
            val recentFiles = parseRecentFilesJson(recentFilesJson)
            UserPreferences(isDarkMode, isContinuousMode, recentFiles)
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

    suspend fun addRecentFile(uriString: String, name: String) {
        context.dataStore.edit { preferences ->
            val currentJson = preferences[PreferencesKeys.RECENT_FILES_JSON] ?: "[]"
            val currentList = parseRecentFilesJson(currentJson).toMutableList()
            currentList.removeAll { it.uriString == uriString }
            currentList.add(0, RecentFile(uriString, name, System.currentTimeMillis()))
            val trimmedList = currentList.take(10)
            preferences[PreferencesKeys.RECENT_FILES_JSON] = toJson(trimmedList)
        }
    }

    suspend fun removeRecentFile(uriString: String) {
        context.dataStore.edit { preferences ->
            val currentJson = preferences[PreferencesKeys.RECENT_FILES_JSON] ?: "[]"
            val currentList = parseRecentFilesJson(currentJson).toMutableList()
            currentList.removeAll { it.uriString == uriString }
            preferences[PreferencesKeys.RECENT_FILES_JSON] = toJson(currentList)
        }
    }

    suspend fun clearRecentFiles() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.RECENT_FILES_JSON] = "[]"
        }
    }

    private fun parseRecentFilesJson(jsonString: String): List<RecentFile> {
        return try {
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<RecentFile>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    RecentFile(
                        uriString = obj.getString("uri"),
                        name = obj.getString("name"),
                        timestamp = obj.getLong("timestamp")
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun toJson(list: List<RecentFile>): String {
        val jsonArray = JSONArray()
        for (file in list) {
            val obj = JSONObject().apply {
                put("uri", file.uriString)
                put("name", file.name)
                put("timestamp", file.timestamp)
            }
            jsonArray.put(obj)
        }
        return jsonArray.toString()
    }
}
