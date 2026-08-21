package com.archieapps.exercises.core.data

import com.archieapps.exercises.core.db.JsonCacheDao
import com.archieapps.exercises.core.db.JsonCacheEntity
import com.archieapps.exercises.core.db.PendingWorkoutDao
import com.archieapps.exercises.core.db.PendingWorkoutEntity
import com.archieapps.exercises.core.net.ApiCaller
import com.archieapps.exercises.core.net.ApiResult
import com.archieapps.exercises.core.net.ExercisesApi
import com.archieapps.exercises.core.net.FailureKind
import com.archieapps.exercises.core.net.ServerTime
import com.archieapps.exercises.core.net.dto.LatestExerciseDto
import com.archieapps.exercises.core.net.dto.WorkoutRequest
import com.archieapps.exercises.core.session.ActiveSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

sealed interface FinishOutcome {
    data class Saved(val workoutId: Int) : FinishOutcome

    data class Queued(val reason: String) : FinishOutcome

    data class Rejected(val reason: String) : FinishOutcome
}

class SessionRepository(
    private val api: ExercisesApi,
    private val cache: JsonCacheDao,
    private val pending: PendingWorkoutDao,
    private val caller: ApiCaller,
    private val json: Json,
) {
    fun observeDraft(): Flow<ActiveSession?> = cache.observe(DRAFT_KEY).map { entity ->
        entity?.let { runCatching { json.decodeFromString<ActiveSession>(it.payload) }.getOrNull() }
    }

    val pendingCount: Flow<Int> = pending.observeCount()

    suspend fun saveDraft(session: ActiveSession) {
        cache.put(
            JsonCacheEntity(
                key = DRAFT_KEY,
                payload = json.encodeToString(session),
                updatedAt = System.currentTimeMillis(),
            )
        )
    }

    suspend fun discardDraft() = cache.remove(DRAFT_KEY)

    suspend fun latest(exerciseId: Int?, name: String?): LatestExerciseDto? {
        val result = caller.call { api.latestExercise(exerciseId = exerciseId, name = name) }

        return (result as? ApiResult.Success)?.value
    }

    suspend fun finish(session: ActiveSession, finishedAtMillis: Long): FinishOutcome {
        if (!session.hasWork) return FinishOutcome.Rejected("Nenhuma série concluída neste treino.")

        val request = session.toRequest(ServerTime.zone, finishedAtMillis)
        val outcome = send(request)

        if (outcome !is FinishOutcome.Rejected) discardDraft()

        return outcome
    }

    suspend fun flushPending(): Int {
        var sent = 0

        pending.all().forEach { entity ->
            val request = runCatching { json.decodeFromString<WorkoutRequest>(entity.payload) }.getOrNull()

            if (request == null) {
                pending.delete(entity.clientUuid)

                return@forEach
            }

            when (val result = caller.call { api.createWorkout(request) }) {
                is ApiResult.Success -> {
                    pending.delete(entity.clientUuid)
                    sent++
                }

                is ApiResult.Failure -> pending.markError(entity.clientUuid, result.message)
            }
        }

        return sent
    }

    private suspend fun send(request: WorkoutRequest): FinishOutcome =
        when (val result = caller.call { api.createWorkout(request) }) {
            is ApiResult.Success -> FinishOutcome.Saved(result.value.id)

            is ApiResult.Failure -> if (result.kind == FailureKind.OFFLINE) {
                enqueue(request)
                FinishOutcome.Queued("Sem conexão. O treino ficou salvo no aparelho e sobe no próximo sync.")
            } else {
                enqueue(request)
                FinishOutcome.Queued("${result.message} O treino ficou salvo no aparelho para tentar de novo.")
            }
        }

    private suspend fun enqueue(request: WorkoutRequest) {
        pending.put(
            PendingWorkoutEntity(
                clientUuid = request.clientUuid,
                payload = json.encodeToString(request),
                createdAt = System.currentTimeMillis(),
            )
        )
    }

    private companion object {
        const val DRAFT_KEY = "active_session"
    }
}
