package com.archieapps.exercises.core.data

import com.archieapps.exercises.core.db.JsonCacheDao
import com.archieapps.exercises.core.db.JsonCacheEntity
import com.archieapps.exercises.core.net.ApiCaller
import com.archieapps.exercises.core.net.ApiResult
import com.archieapps.exercises.core.net.ExercisesApi
import com.archieapps.exercises.core.net.dto.ExerciseDto
import com.archieapps.exercises.core.net.dto.ExerciseMuscleMappingDto
import com.archieapps.exercises.core.net.dto.ExerciseMusclesDto
import com.archieapps.exercises.core.net.dto.ExerciseMusclesRequest
import com.archieapps.exercises.core.net.dto.MuscleAnalysisDto
import com.archieapps.exercises.core.net.dto.MuscleAttributionDto
import com.archieapps.exercises.core.net.dto.AdoptExerciseRequest
import com.archieapps.exercises.core.net.dto.MuscleHeatmapDto
import com.archieapps.exercises.core.net.dto.MuscleRecommendationDto
import com.archieapps.exercises.core.net.dto.MuscleDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement

@Serializable
data class ExerciseMuscleMap(
    val byExercise: Map<Int, ExerciseMusclesDto> = emptyMap(),
    val unmappedReasons: Map<Int, String> = emptyMap(),
) {
    fun of(exerciseId: Int?): ExerciseMusclesDto =
        exerciseId?.let { byExercise[it] } ?: ExerciseMusclesDto()

    fun reasonFor(exerciseId: Int?): String? = exerciseId?.let { unmappedReasons[it] }
}

@Serializable
data class AnatomySnapshot(
    val analysis: MuscleAnalysisDto = MuscleAnalysisDto(),
    val attribution: MuscleAttributionDto = MuscleAttributionDto(),
    val heatmap: MuscleHeatmapDto = MuscleHeatmapDto(),
    val recommendations: List<MuscleRecommendationDto> = emptyList(),
    val muscles: List<MuscleDto> = emptyList(),
)

data class CachedAnatomy(val snapshot: AnatomySnapshot, val syncedAt: Long)

class AnatomyRepository(
    private val api: ExercisesApi,
    private val cache: JsonCacheDao,
    private val caller: ApiCaller,
    private val json: Json,
) {
    fun observeExerciseMuscles(): Flow<ExerciseMuscleMap> = cache.observe(MAP_KEY).map { entity ->
        entity?.let {
            runCatching { json.decodeFromString<ExerciseMuscleMap>(it.payload) }.getOrNull()
        } ?: ExerciseMuscleMap()
    }

    fun observe(): Flow<CachedAnatomy?> = cache.observe(CATALOG_KEY).map { entity ->
        entity?.let {
            runCatching { CachedAnatomy(json.decodeFromString(it.payload), it.updatedAt) }.getOrNull()
        }
    }

    suspend fun putExerciseMuscles(exercises: List<ExerciseDto>) {
        val map = ExerciseMuscleMap(
            byExercise = exercises
                .filter { !it.muscles.isEmpty }
                .associate { it.id to it.muscles },
            unmappedReasons = exercises
                .mapNotNull { exercise -> exercise.musclesUnmappedReason?.let { exercise.id to it } }
                .toMap(),
        )

        write(MAP_KEY, json.encodeToString(map))
    }

    fun observeMuscleCatalog(): Flow<List<MuscleDto>> = cache.observe(MUSCLES_KEY).map { entity ->
        entity?.let {
            runCatching { json.decodeFromString<List<MuscleDto>>(it.payload) }.getOrNull()
        }.orEmpty()
    }

    suspend fun cachedMuscleCatalog(): List<MuscleDto> = cache.find(MUSCLES_KEY)
        ?.let { runCatching { json.decodeFromString<List<MuscleDto>>(it.payload) }.getOrNull() }
        .orEmpty()

    suspend fun syncMuscleCatalog(): ApiResult<List<MuscleDto>> {
        val result = caller.call { api.muscles() }

        if (result is ApiResult.Success) write(MUSCLES_KEY, json.encodeToString(result.value))

        return result
    }

    suspend fun syncExerciseMuscles(
        exerciseId: Int,
        request: ExerciseMusclesRequest,
    ): ApiResult<ExerciseMuscleMappingDto> {
        val result = caller.call { api.syncExerciseMuscles(exerciseId, request) }

        if (result is ApiResult.Success) mergeExerciseMuscles(exerciseId, result.value)

        return result
    }

    private suspend fun cachedExerciseMuscles(): ExerciseMuscleMap = cache.find(MAP_KEY)
        ?.let { runCatching { json.decodeFromString<ExerciseMuscleMap>(it.payload) }.getOrNull() }
        ?: ExerciseMuscleMap()

    private suspend fun mergeExerciseMuscles(exerciseId: Int, mapping: ExerciseMuscleMappingDto) {
        val current = cachedExerciseMuscles()

        val byExercise = current.byExercise.toMutableMap().apply {
            if (mapping.muscles.isEmpty) remove(exerciseId) else put(exerciseId, mapping.muscles)
        }

        val reasons = current.unmappedReasons.toMutableMap().apply {
            val reason = mapping.musclesUnmappedReason
            if (reason == null) remove(exerciseId) else put(exerciseId, reason)
        }

        write(MAP_KEY, json.encodeToString(ExerciseMuscleMap(byExercise, reasons)))
    }

    suspend fun adopt(canonical: String): ApiResult<ExerciseDto> =
        caller.call { api.adoptExercise(AdoptExerciseRequest(canonical)) }

    suspend fun refresh(): ApiResult<Unit> {
        val catalog = syncMuscleCatalog()
        if (catalog is ApiResult.Failure) return catalog

        val envelope = caller.envelope { api.muscleAnalysis(granularity = "as_mapped") }
        if (envelope is ApiResult.Failure) return envelope

        val payload = (envelope as ApiResult.Success).value

        val heatmap = caller.call { api.muscleHeatmap() }
        if (heatmap is ApiResult.Failure) return heatmap

        val recommendations = caller.call { api.muscleRecommendations() }
        if (recommendations is ApiResult.Failure) return recommendations

        write(
            CATALOG_KEY,
            json.encodeToString(
                AnatomySnapshot(
                    analysis = payload.body ?: MuscleAnalysisDto(),
                    attribution = payload.misc
                        ?.let { runCatching { json.decodeFromJsonElement<MuscleAttributionDto>(it) }.getOrNull() }
                        ?: MuscleAttributionDto(),
                    heatmap = (heatmap as ApiResult.Success).value,
                    recommendations = (recommendations as ApiResult.Success).value,
                    muscles = (catalog as ApiResult.Success).value,
                )
            )
        )

        return ApiResult.Success(Unit)
    }

    private suspend fun write(key: String, payload: String) {
        cache.put(JsonCacheEntity(key = key, payload = payload, updatedAt = System.currentTimeMillis()))
    }

    private companion object {
        const val MAP_KEY = "exercise_muscles"
        const val CATALOG_KEY = "anatomy"
        const val MUSCLES_KEY = "muscle_catalog"
    }
}
