package com.archieapps.exercises.core.data

import com.archieapps.exercises.core.net.ApiCaller
import com.archieapps.exercises.core.net.ApiResult
import com.archieapps.exercises.core.net.ExercisesApi
import com.archieapps.exercises.core.net.LoginRequest
import com.archieapps.exercises.core.net.RegisterRequest
import com.archieapps.exercises.core.net.Session
import com.archieapps.exercises.core.settings.SettingsStore

class AuthRepository(
    private val api: ExercisesApi,
    private val caller: ApiCaller,
    private val settings: SettingsStore,
    private val clearLocalData: suspend () -> Unit,
) {
    suspend fun login(email: String, password: String): ApiResult<Unit> =
        startSession(caller.call { api.login(LoginRequest(email.trim(), password)) })

    suspend fun register(name: String, email: String, password: String, confirmation: String): ApiResult<Unit> =
        startSession(
            caller.call {
                api.register(RegisterRequest(name.trim(), email.trim(), password, confirmation))
            }
        )

    suspend fun logout() = settings.clearSession()

    private suspend fun startSession(result: ApiResult<Session>): ApiResult<Unit> = when (result) {
        is ApiResult.Success -> {
            val userId = result.value.user?.id
            val previousUserId = settings.current.userId

            if (previousUserId != null && previousUserId != userId) clearLocalData()

            settings.setSession(result.value.token, result.value.user?.name, userId)
            ApiResult.Success(Unit)
        }

        is ApiResult.Failure -> result
    }
}
