package com.archieapps.exercises.core.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking

data class AppSettings(
    val token: String? = null,
    val userName: String? = null,
    val defaultRestSeconds: Int = 90,
    val keepScreenOnDuringWorkout: Boolean = true,
    val planObjective: String? = null,
    val planDaysPerWeek: Int? = null,
    val planMinutesPerDay: Int? = null,
    val planEquipment: List<String> = emptyList(),
) {
    val isLoggedIn: Boolean get() = !token.isNullOrBlank()
}

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsStore(context: Context, scope: CoroutineScope) {
    private val store = context.settingsDataStore

    val state: StateFlow<AppSettings> = store.data
        .map(::read)
        .stateIn(scope, SharingStarted.Eagerly, runBlocking { read(store.data.first()) })

    val current: AppSettings get() = state.value

    suspend fun setSession(token: String, userName: String?) {
        store.edit {
            it[Keys.token] = token
            if (userName != null) it[Keys.userName] = userName
        }
    }

    suspend fun clearSession() {
        store.edit {
            it.remove(Keys.token)
            it.remove(Keys.userName)
        }
    }

    suspend fun setDefaultRestSeconds(seconds: Int) {
        store.edit { it[Keys.restSeconds] = seconds.coerceIn(15, 600) }
    }

    suspend fun setKeepScreenOn(enabled: Boolean) {
        store.edit { it[Keys.keepScreenOn] = enabled }
    }

    suspend fun setPlanForm(objective: String, daysPerWeek: Int, minutesPerDay: Int, equipment: List<String>) {
        store.edit {
            it[Keys.planObjective] = objective
            it[Keys.planDays] = daysPerWeek
            it[Keys.planMinutes] = minutesPerDay
            it[Keys.planEquipment] = equipment.toSet()
        }
    }

    private fun read(preferences: Preferences) = AppSettings(
        token = preferences[Keys.token],
        userName = preferences[Keys.userName],
        defaultRestSeconds = preferences[Keys.restSeconds] ?: 90,
        keepScreenOnDuringWorkout = preferences[Keys.keepScreenOn] ?: true,
        planObjective = preferences[Keys.planObjective],
        planDaysPerWeek = preferences[Keys.planDays],
        planMinutesPerDay = preferences[Keys.planMinutes],
        planEquipment = preferences[Keys.planEquipment]?.toList() ?: emptyList(),
    )

    private object Keys {
        val token = stringPreferencesKey("token")
        val userName = stringPreferencesKey("user_name")
        val restSeconds = intPreferencesKey("default_rest_seconds")
        val keepScreenOn = booleanPreferencesKey("keep_screen_on")
        val planObjective = stringPreferencesKey("plan_objective")
        val planDays = intPreferencesKey("plan_days_per_week")
        val planMinutes = intPreferencesKey("plan_minutes_per_day")
        val planEquipment = stringSetPreferencesKey("plan_equipment")
    }
}
