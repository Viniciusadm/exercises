package com.archieapps.exercises.ui.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archieapps.exercises.core.data.AuthRepository
import com.archieapps.exercises.core.net.ApiResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val MIN_PASSWORD_LENGTH = 8

data class RegisterUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmation: String = "",
    val loading: Boolean = false,
    val error: String? = null,
) {
    val passwordTooShort: Boolean get() = password.isNotEmpty() && password.length < MIN_PASSWORD_LENGTH

    val confirmationMismatch: Boolean get() = confirmation.isNotEmpty() && confirmation != password

    val canSubmit: Boolean
        get() = name.isNotBlank() &&
            email.isNotBlank() &&
            password.length >= MIN_PASSWORD_LENGTH &&
            confirmation == password &&
            !loading
}

class RegisterViewModel(private val authRepository: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(RegisterUiState())
    val state: StateFlow<RegisterUiState> = _state.asStateFlow()

    fun onNameChange(value: String) = _state.update { it.copy(name = value, error = null) }

    fun onEmailChange(value: String) = _state.update { it.copy(email = value, error = null) }

    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, error = null) }

    fun onConfirmationChange(value: String) = _state.update { it.copy(confirmation = value, error = null) }

    fun submit() {
        val current = _state.value
        if (!current.canSubmit) return

        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }

            val result = authRepository.register(current.name, current.email, current.password, current.confirmation)

            when (result) {
                is ApiResult.Success -> _state.update { it.copy(loading = false, password = "", confirmation = "") }
                is ApiResult.Failure -> _state.update { it.copy(loading = false, error = result.message) }
            }
        }
    }
}
