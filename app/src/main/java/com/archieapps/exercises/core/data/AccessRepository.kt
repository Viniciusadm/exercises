package com.archieapps.exercises.core.data

import com.archieapps.exercises.core.access.AccessCode
import com.archieapps.exercises.core.net.AccessCodeRequest
import com.archieapps.exercises.core.net.ApiCaller
import com.archieapps.exercises.core.net.ApiResult
import com.archieapps.exercises.core.net.ExercisesApi

class AccessRepository(
    private val api: ExercisesApi,
    private val caller: ApiCaller,
) {
    suspend fun validate(code: String): ApiResult<Unit> =
        when (val result = caller.envelope { api.validateAccessCode(AccessCodeRequest(code)) }) {
            is ApiResult.Success -> {
                AccessCode.remember(code)
                ApiResult.Success(Unit)
            }

            is ApiResult.Failure -> result
        }
}
