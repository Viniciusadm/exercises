package com.archieapps.exercises.core.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.archieapps.exercises.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking

data class AppSettings(
    val baseUrl: String = BuildConfig.DEFAULT_BASE_URL,
    val token: String? = null,
    val userName: String? = null,
    val defaultRestSeconds: Int = 90,
    val keepScreenOnDuringWorkout: Boolean = true,
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

    suspend fun setBaseUrl(value: String) {
        val normalized = normalizeBaseUrl(value) ?: return
        store.edit { it[Keys.baseUrl] = normalized }
    }

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

    private fun read(preferences: Preferences) = AppSettings(
        baseUrl = preferences[Keys.baseUrl] ?: BuildConfig.DEFAULT_BASE_URL,
        token = preferences[Keys.token],
        userName = preferences[Keys.userName],
        defaultRestSeconds = preferences[Keys.restSeconds] ?: 90,
        keepScreenOnDuringWorkout = preferences[Keys.keepScreenOn] ?: true,
    )

    private object Keys {
        val baseUrl = stringPreferencesKey("base_url")
        val token = stringPreferencesKey("token")
        val userName = stringPreferencesKey("user_name")
        val restSeconds = intPreferencesKey("default_rest_seconds")
        val keepScreenOn = booleanPreferencesKey("keep_screen_on")
    }
}

fun normalizeBaseUrl(input: String): String? {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return null

    val withScheme = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
        trimmed
    } else {
        "http://$trimmed"
    }

    return withScheme.trimEnd('/') + "/"
}
