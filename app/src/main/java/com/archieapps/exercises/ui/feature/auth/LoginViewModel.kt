package com.archieapps.exercises.ui.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archieapps.exercises.core.data.AuthRepository
import com.archieapps.exercises.core.net.ApiResult
import com.archieapps.exercises.core.settings.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val baseUrl: String = "",
    val editingServer: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null,
) {
    val canSubmit: Boolean get() = email.isNotBlank() && password.isNotBlank() && !loading
}

class LoginViewModel(
    private val authRepository: AuthRepository,
    private val settings: SettingsStore,
) : ViewModel() {
    private val _state = MutableStateFlow(LoginUiState(baseUrl = settings.current.baseUrl))
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onEmailChange(value: String) = _state.update { it.copy(email = value, error = null) }

    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, error = null) }

    fun onBaseUrlChange(value: String) = _state.update { it.copy(baseUrl = value) }

    fun toggleServerEditor() = _state.update { it.copy(editingServer = !it.editingServer) }

    fun submit() {
        val current = _state.value
        if (!current.canSubmit) return

        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            settings.setBaseUrl(current.baseUrl)

            when (val result = authRepository.login(current.email, current.password)) {
                is ApiResult.Success -> _state.update { it.copy(loading = false, password = "") }
                is ApiResult.Failure -> _state.update { it.copy(loading = false, error = result.message) }
            }
        }
    }
}
