package com.archieapps.exercises.core.data

import com.archieapps.exercises.core.db.JsonCacheDao
import com.archieapps.exercises.core.db.JsonCacheEntity
import com.archieapps.exercises.core.net.ApiCaller
import com.archieapps.exercises.core.net.ApiResult
import com.archieapps.exercises.core.net.ExercisesApi
import com.archieapps.exercises.core.net.dto.ExerciseHistoryDto
import com.archieapps.exercises.core.net.dto.WorkoutDto
import com.archieapps.exercises.core.net.dto.WorkoutRequest
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

class WorkoutRepository(
    private val api: ExercisesApi,
    private val cache: JsonCacheDao,
    private val caller: ApiCaller,
    private val json: Json,
) {
    suspend fun page(page: Int, perPage: Int = 20): ApiResult<List<WorkoutDto>> {
        val result = caller.call { api.workouts(page = page, perPage = perPage, includeExercises = 1) }

        if (result is ApiResult.Success && page == 1) {
            store(HISTORY_KEY, json.encodeToString(ListSerializer(WorkoutDto.serializer()), result.value))
        }

        return result
    }

    suspend fun cachedFirstPage(): List<WorkoutDto> = read(HISTORY_KEY) {
        json.decodeFromString(ListSerializer(WorkoutDto.serializer()), it)
    } ?: emptyList()

    suspend fun detail(id: Int): ApiResult<WorkoutDto> = caller.call { api.workout(id) }

    suspend fun update(id: Int, request: WorkoutRequest): ApiResult<WorkoutDto> =
        caller.call { api.updateWorkout(id, request) }

    suspend fun delete(id: Int): ApiResult<Map<String, Int>> = caller.call { api.deleteWorkout(id) }

    suspend fun history(exerciseId: Int): ApiResult<ExerciseHistoryDto> {
        val result = caller.call { api.exerciseHistory(exerciseId) }

        if (result is ApiResult.Success) {
            store(historyKey(exerciseId), json.encodeToString(ExerciseHistoryDto.serializer(), result.value))
        }

        return result
    }

    suspend fun cachedHistory(exerciseId: Int): ExerciseHistoryDto? = read(historyKey(exerciseId)) {
        json.decodeFromString(ExerciseHistoryDto.serializer(), it)
    }

    private suspend fun store(key: String, payload: String) {
        cache.put(JsonCacheEntity(key = key, payload = payload, updatedAt = System.currentTimeMillis()))
    }

    private suspend fun <T> read(key: String, decode: (String) -> T): T? =
        cache.find(key)?.let { runCatching { decode(it.payload) }.getOrNull() }

    private fun historyKey(exerciseId: Int) = "exercise_history:$exerciseId"

    private companion object {
        const val HISTORY_KEY = "workout_history"
    }
}
