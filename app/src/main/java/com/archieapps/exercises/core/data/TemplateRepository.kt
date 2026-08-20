package com.archieapps.exercises.core.data

import com.archieapps.exercises.core.db.JsonCacheDao
import com.archieapps.exercises.core.db.JsonCacheEntity
import com.archieapps.exercises.core.net.ApiCaller
import com.archieapps.exercises.core.net.ApiResult
import com.archieapps.exercises.core.net.ExercisesApi
import com.archieapps.exercises.core.net.dto.NameRequest
import com.archieapps.exercises.core.net.dto.TemplateDto
import com.archieapps.exercises.core.net.dto.TemplateRequest
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

class TemplateRepository(
    private val api: ExercisesApi,
    private val cache: JsonCacheDao,
    private val caller: ApiCaller,
    private val json: Json,
) {
    suspend fun list(): ApiResult<List<TemplateDto>> {
        val result = caller.call { api.templates() }

        if (result is ApiResult.Success) {
            cache.put(
                JsonCacheEntity(
                    key = CACHE_KEY,
                    payload = json.encodeToString(ListSerializer(TemplateDto.serializer()), result.value),
                    updatedAt = System.currentTimeMillis(),
                )
            )
        }

        return result
    }

    suspend fun cached(): List<TemplateDto> = cache.find(CACHE_KEY)?.let { entity ->
        runCatching { json.decodeFromString(ListSerializer(TemplateDto.serializer()), entity.payload) }.getOrNull()
    } ?: emptyList()

    suspend fun create(request: TemplateRequest): ApiResult<TemplateDto> = caller.call { api.createTemplate(request) }

    suspend fun update(id: Int, request: TemplateRequest): ApiResult<TemplateDto> =
        caller.call { api.updateTemplate(id, request) }

    suspend fun delete(id: Int): ApiResult<Map<String, Int>> = caller.call { api.deleteTemplate(id) }

    suspend fun fromWorkout(workoutId: Int, name: String?): ApiResult<TemplateDto> =
        caller.call { api.templateFromWorkout(workoutId, NameRequest(name)) }

    private companion object {
        const val CACHE_KEY = "templates"
    }
}
