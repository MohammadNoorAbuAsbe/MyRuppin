package com.example.myruppin.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class TokenManager(private val context: Context) {
    companion object {
        private val TOKEN_KEY = stringPreferencesKey("auth_token")
        private val STUDENT_ID_KEY = stringPreferencesKey("student_id")
        private val PASSWORD_KEY = stringPreferencesKey("password")
        private val GRADES_KEY = stringSetPreferencesKey("grades")
    }

    // Save all credentials
    suspend fun saveCredentials(token: String, studentId: String, password: String) {
        context.dataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
            preferences[STUDENT_ID_KEY] = studentId
            preferences[PASSWORD_KEY] = password
        }
    }

    // Save grades
    suspend fun saveGrades(grades: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[GRADES_KEY] = grades
        }
    }

    // Get token
    val token: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[TOKEN_KEY]
    }

    // Get student ID
    val studentId: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[STUDENT_ID_KEY]
    }

    // Get password
    val password: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PASSWORD_KEY]
    }

    // Get grades
    val grades: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[GRADES_KEY] ?: emptySet()
    }

    // Clear all data
    suspend fun clearAll() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}