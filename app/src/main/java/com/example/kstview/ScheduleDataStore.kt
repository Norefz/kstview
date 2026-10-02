package com.example.kstview

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "schedule_prefs")

object ScheduleDataStore {
    private val SCHEDULE_KEY = stringPreferencesKey("course_schedule_json")

    suspend fun saveSchedule(context: Context, scheduleJson: String) {
        context.dataStore.edit { preferences ->
            preferences[SCHEDULE_KEY] = scheduleJson
        }
    }

    fun getSchedule(context: Context): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[SCHEDULE_KEY] ?: "[]"
        }
    }
}
