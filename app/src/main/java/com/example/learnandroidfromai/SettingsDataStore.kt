package com.example.learnandroidfromai

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "settings"
)

private val SHOW_STUDY_TIP =
    booleanPreferencesKey("show_study_tip")

class SettingsDataStore(
    private val context: Context
) {

    val showStudyTip: Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[SHOW_STUDY_TIP] ?: true
        }

    suspend fun setShowStudyTip(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SHOW_STUDY_TIP] = value
        }
    }
}