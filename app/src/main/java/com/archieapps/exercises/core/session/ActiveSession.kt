package com.archieapps.exercises.core.session

import com.archieapps.exercises.core.net.dto.WorkoutEventRequest
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
    val openExerciseIndex: Int? = null,
    val exercises: List<SessionExercise> = emptyList(),
    val phase: SessionPhase = SessionPhase.Idle,
    val events: List<SessionEvent> = emptyList(),
    val nextSequence: Int = 0,
) {
    val completedSets: Int get() = exercises.sumOf { exercise -> exercise.sets.count { it.done } }

    val plannedSets: Int get() = exercises.sumOf { it.sets.size }

    val totalVolumeKg: Double
        get() = exercises.sumOf { exercise ->
            exercise.sets.filter { it.done }.sumOf { (it.repetitions ?: 0) * (it.weightKg ?: 0.0) }
        }

    val hasWork: Boolean get() = completedSets > 0

    fun elapsedSeconds(nowMillis: Long): Long = max(0L, (nowMillis - startedAtEpochMillis) / 1000L)

    fun durationMinutes(nowMillis: Long): Int = max(1, Math.round(elapsedSeconds(nowMillis) / 60.0).toInt())

    val executingSet: Pair<Int, Int>?
        get() = (phase as? SessionPhase.Executing)?.let { it.exerciseIndex to it.setIndex }

    val restingSet: Pair<Int, Int>?
        get() = (phase as? SessionPhase.Resting)?.let { it.exerciseIndex to it.setIndex }

    fun openExercise(): SessionExercise? = openExerciseIndex?.let { exercises.getOrNull(it) }

    fun record(
        type: String,
        atMillis: Long,
        exerciseIndex: Int? = null,
        setIndex: Int? = null,
        meta: Map<String, String>? = null,
    ): ActiveSession {
        val exercise = exerciseIndex?.let { exercises.getOrNull(it) }

        return copy(
            events = events + SessionEvent(
                sequence = nextSequence,
                type = type,
                atEpochMillis = maxOf(atMillis, events.lastOrNull()?.atEpochMillis ?: atMillis),
                exerciseOrder = exerciseIndex,
                exerciseName = exercise?.name,
                setNumber = setIndex?.let { it + 1 },
                meta = meta,
            ),
            nextSequence = nextSequence + 1,
        )
    }

    fun setRunState(exerciseIndex: Int, setIndex: Int): SetRunState {
        val set = exercises.getOrNull(exerciseIndex)?.sets?.getOrNull(setIndex) ?: return SetRunState.Pending

        return when {
            executingSet == exerciseIndex to setIndex -> SetRunState.Running
            set.done -> SetRunState.Done
            else -> SetRunState.Pending
        }
    }

    fun nextPendingSet(exerciseIndex: Int): Int? =
        exercises.getOrNull(exerciseIndex)?.sets?.indexOfFirst { !it.done }?.takeIf { it >= 0 }

    fun canRemoveExercise(index: Int): Boolean {
        val anchored = executingSet ?: restingSet ?: return true

        return index > anchored.first
    }

    fun canRemoveSet(exerciseIndex: Int, setIndex: Int): Boolean {
        val anchored = executingSet ?: restingSet ?: return true

        return anchored.first != exerciseIndex || setIndex > anchored.second
    }

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
            events = eventRequests(zone),
        )
    }

    private fun eventRequests(zone: ZoneId): List<WorkoutEventRequest> {
        val exerciseOrders = mutableMapOf<Int, Int>()
        val setNumbers = mutableMapOf<Pair<Int, Int>, Int>()
        var keptExercises = 0

        exercises.forEachIndexed { exerciseIndex, exercise ->
            val done = exercise.sets.withIndex().filter { it.value.done }

            if (done.isEmpty()) return@forEachIndexed

            exerciseOrders[exerciseIndex] = keptExercises++
            done.forEachIndexed { position, (setIndex, _) ->
                setNumbers[exerciseIndex to setIndex] = position + 1
            }
        }

        return events.map { event ->
            val exerciseIndex = event.exerciseOrder
            val setIndex = event.setNumber?.let { it - 1 }
            val remappedSet = if (exerciseIndex != null && setIndex != null) {
                setNumbers[exerciseIndex to setIndex]
            } else {
                null
            }

            WorkoutEventRequest(
                sequence = event.sequence,
                type = event.type,
                occurredAt = Instant.ofEpochMilli(event.atEpochMillis).atZone(zone)
                    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                exerciseOrder = exerciseIndex?.let { exerciseOrders[it] } ?: exerciseIndex,
                exerciseName = event.exerciseName,
                setNumber = remappedSet ?: event.setNumber,
                meta = event.meta,
            )
        }
    }
}

enum class SetRunState { Pending, Running, Done }

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

    val doneSets: Int get() = sets.count { it.done }

    val started: Boolean get() = doneSets > 0

    val topWeightKg: Double? get() = sets.filter { it.done }.mapNotNull { it.weightKg }.maxOrNull()
}

@Serializable
data class SessionSet(
    val repetitions: Int? = null,
    val weightKg: Double? = null,
    val durationSeconds: Int? = null,
    val done: Boolean = false,
)
