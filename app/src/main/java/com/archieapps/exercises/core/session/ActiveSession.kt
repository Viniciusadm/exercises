package com.archieapps.exercises.core.session

import com.archieapps.exercises.core.net.dto.WorkoutExerciseRequest
import com.archieapps.exercises.core.net.dto.WorkoutRequest
import com.archieapps.exercises.core.net.dto.WorkoutSetRequest
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.max
import kotlinx.serialization.Serializable

@Serializable
data class ActiveSession(
    val clientUuid: String,
    val name: String,
    val workoutType: String = "Musculação",
    val templateId: Int? = null,
    val startedAtEpochMillis: Long,
    val notes: String? = null,
    val currentIndex: Int = 0,
    val exercises: List<SessionExercise> = emptyList(),
) {
    val completedSets: Int get() = exercises.sumOf { exercise -> exercise.sets.count { it.done } }

    val totalVolumeKg: Double
        get() = exercises.sumOf { exercise ->
            exercise.sets.filter { it.done }.sumOf { (it.repetitions ?: 0) * (it.weightKg ?: 0.0) }
        }

    val hasWork: Boolean get() = completedSets > 0

    fun elapsedSeconds(nowMillis: Long): Long = max(0L, (nowMillis - startedAtEpochMillis) / 1000L)

    fun durationMinutes(nowMillis: Long): Int = max(1, Math.round(elapsedSeconds(nowMillis) / 60.0).toInt())

    fun current(): SessionExercise? = exercises.getOrNull(currentIndex)

    fun updateExercise(index: Int, transform: (SessionExercise) -> SessionExercise): ActiveSession =
        copy(exercises = exercises.mapIndexed { position, exercise ->
            if (position == index) transform(exercise) else exercise
        })

    fun toRequest(zone: ZoneId, finishedAtMillis: Long): WorkoutRequest {
        val started = Instant.ofEpochMilli(startedAtEpochMillis).atZone(zone)
        val finished = Instant.ofEpochMilli(finishedAtMillis).atZone(zone)

        return WorkoutRequest(
            clientUuid = clientUuid,
            name = name.ifBlank { "Treino" },
            date = started.toLocalDate().toString(),
            startedAt = started.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
            finishedAt = finished.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
            durationMinutes = durationMinutes(finishedAtMillis),
            workoutType = workoutType,
            templateId = templateId,
            notes = notes?.takeIf { it.isNotBlank() },
            exercises = exercises.mapNotNull { exercise ->
                val done = exercise.sets.filter { it.done }
                if (done.isEmpty()) {
                    null
                } else {
                    WorkoutExerciseRequest(
                        exerciseId = exercise.exerciseId,
                        name = exercise.name,
                        executionType = exercise.executionType,
                        usesWeight = exercise.usesWeight,
                        muscleGroup = exercise.muscleGroup,
                        difficulty = exercise.difficulty,
                        notes = exercise.notes?.takeIf { it.isNotBlank() },
                        sets = done.mapIndexed { index, set ->
                            WorkoutSetRequest(
                                setNumber = index + 1,
                                repetitions = set.repetitions,
                                durationSeconds = set.durationSeconds,
                                weightKg = set.weightKg,
                            )
                        },
                    )
                }
            },
        )
    }
}

@Serializable
data class SessionExercise(
    val exerciseId: Int? = null,
    val name: String,
    val executionType: String = "sets",
    val usesWeight: Boolean = true,
    val muscleGroup: String? = null,
    val equipment: String? = null,
    val restSeconds: Int? = null,
    val difficulty: String? = null,
    val notes: String? = null,
    val targetReps: Int? = null,
    val sets: List<SessionSet> = emptyList(),
    val previousSummary: String? = null,
    val suggestion: String? = null,
    val bestWeightKg: Double? = null,
) {
    val volumeKg: Double
        get() = sets.filter { it.done }.sumOf { (it.repetitions ?: 0) * (it.weightKg ?: 0.0) }

    val isComplete: Boolean get() = sets.isNotEmpty() && sets.all { it.done }

    val topWeightKg: Double? get() = sets.filter { it.done }.mapNotNull { it.weightKg }.maxOrNull()
}

@Serializable
data class SessionSet(
    val repetitions: Int? = null,
    val weightKg: Double? = null,
    val durationSeconds: Int? = null,
    val done: Boolean = false,
)
