package com.archieapps.exercises.core.data

import com.archieapps.exercises.core.db.ExerciseDao
import com.archieapps.exercises.core.db.ExerciseEntity
import com.archieapps.exercises.core.net.ApiCaller
import com.archieapps.exercises.core.net.ApiResult
import com.archieapps.exercises.core.net.ExercisesApi
import com.archieapps.exercises.core.net.dto.ExerciseDto
import com.archieapps.exercises.core.net.dto.ExerciseRequest
import kotlinx.coroutines.flow.Flow

class ExerciseRepository(
    private val api: ExercisesApi,
    private val dao: ExerciseDao,
    private val caller: ApiCaller,
    private val anatomy: AnatomyRepository? = null,
) {
    val catalog: Flow<List<ExerciseEntity>> = dao.observeAll()

    val count: Flow<Int> = dao.observeCount()

    suspend fun sync(): ApiResult<Int> {
        val result = caller.call { api.exercises() }

        return when (result) {
            is ApiResult.Success -> {
                val entities = result.value.map(::toEntity)
                dao.upsertAll(entities)
                dao.deleteMissing(entities.map { it.id })
                anatomy?.putExerciseMuscles(result.value)
                ApiResult.Success(entities.size)
            }

            is ApiResult.Failure -> result
        }
    }

    suspend fun create(request: ExerciseRequest): ApiResult<ExerciseDto> {
        val result = caller.call { api.createExercise(request) }

        if (result is ApiResult.Success) dao.upsertAll(listOf(toEntity(result.value)))

        return result
    }

    suspend fun update(id: Int, request: ExerciseRequest): ApiResult<ExerciseDto> {
        val result = caller.call { api.updateExercise(id, request) }

        if (result is ApiResult.Success) dao.upsertAll(listOf(toEntity(result.value)))

        return result
    }

    suspend fun find(id: Int) = dao.find(id)

    private fun toEntity(dto: ExerciseDto) = ExerciseEntity(
        id = dto.id,
        name = dto.name,
        executionType = dto.executionType,
        usesWeight = dto.usesWeight,
        muscleGroup = dto.muscleGroup,
        difficulty = dto.difficulty,
        equipment = dto.equipment,
        restSeconds = dto.restSeconds,
        notes = dto.notes,
        lastPerformedAt = dto.lastPerformedAt,
        sessionsCount = dto.sessionsCount,
        bestWeightKg = dto.bestWeightKg,
        totalVolumeKg = dto.totalVolumeKg,
    )
}
