package com.archieapps.exercises.core.data

import com.archieapps.exercises.core.net.ApiCaller
import com.archieapps.exercises.core.net.ApiResult
import com.archieapps.exercises.core.net.ExercisesApi
import com.archieapps.exercises.core.net.LoginRequest
import com.archieapps.exercises.core.settings.SettingsStore

class AuthRepository(
    private val api: ExercisesApi,
    private val caller: ApiCaller,
    private val settings: SettingsStore,
) {
    suspend fun login(email: String, password: String): ApiResult<Unit> {
        val result = caller.call { api.login(LoginRequest(email.trim(), password)) }

        return when (result) {
            is ApiResult.Success -> {
                settings.setSession(result.value.token, result.value.user?.name)
                ApiResult.Success(Unit)
            }

            is ApiResult.Failure -> result
        }
    }

    suspend fun logout() = settings.clearSession()
}
