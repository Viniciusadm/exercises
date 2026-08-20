package com.archieapps.exercises.ui.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archieapps.exercises.core.data.AuthRepository
import com.archieapps.exercises.core.data.ExerciseRepository
import com.archieapps.exercises.core.data.SessionRepository
import com.archieapps.exercises.core.net.ApiResult
import com.archieapps.exercises.core.settings.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val userName: String? = null,
    val defaultRestSeconds: Int = 90,
    val keepScreenOn: Boolean = true,
    val catalogSize: Int = 0,
    val pendingWorkouts: Int = 0,
    val syncing: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false,
)

class SettingsViewModel(
    private val settings: SettingsStore,
    private val exercises: ExerciseRepository,
    private val sessions: SessionRepository,
    private val auth: AuthRepository,
) : ViewModel() {
    private val syncing = MutableStateFlow(false)
    private val message = MutableStateFlow<Pair<String, Boolean>?>(null)

    val state: StateFlow<SettingsUiState> = combine(
        settings.state,
        exercises.count,
        sessions.pendingCount,
        syncing,
        message,
    ) { appSettings, catalogSize, pendingWorkouts, isSyncing, notice ->
        SettingsUiState(
            userName = appSettings.userName,
            defaultRestSeconds = appSettings.defaultRestSeconds,
            keepScreenOn = appSettings.keepScreenOnDuringWorkout,
            catalogSize = catalogSize,
            pendingWorkouts = pendingWorkouts,
            syncing = isSyncing,
            message = notice?.first,
            isError = notice?.second ?: false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setDefaultRest(seconds: Int) {
        viewModelScope.launch { settings.setDefaultRestSeconds(seconds) }
    }

    fun setKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch { settings.setKeepScreenOn(enabled) }
    }

    fun syncCatalog() {
        viewModelScope.launch {
            syncing.value = true

            message.value = when (val result = exercises.sync()) {
                is ApiResult.Success -> "${result.value} exercícios sincronizados." to false
                is ApiResult.Failure -> result.message to true
            }

            syncing.value = false
        }
    }

    fun flushPending() {
        viewModelScope.launch {
            syncing.value = true
            val sent = sessions.flushPending()

            message.value = if (sent > 0) {
                "$sent treino(s) enviados." to false
            } else {
                "Nada pendente para enviar." to false
            }

            syncing.value = false
        }
    }

    fun logout() {
        viewModelScope.launch { auth.logout() }
    }
}
