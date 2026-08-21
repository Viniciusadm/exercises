package com.archieapps.exercises.core.session

import com.archieapps.exercises.core.net.dto.TemplateDto
import com.archieapps.exercises.core.net.dto.WorkoutDto

object SessionBuilder {
    const val ESTIMATED_SET_SECONDS = 45

    fun fromTemplate(template: TemplateDto, defaultRestSeconds: Int): List<SessionExercise> =
        template.exercises.sortedBy { it.order }.map { item ->
            SessionExercise(
                exerciseId = item.exerciseId,
                name = item.name,
                executionType = item.executionType,
                usesWeight = item.usesWeight,
                muscleGroup = item.muscleGroup,
                equipment = item.equipment,
                restSeconds = item.restSeconds ?: defaultRestSeconds,
                targetReps = item.targetReps,
                notes = item.notes,
                sets = List((item.targetSets ?: 3).coerceIn(1, 12)) {
                    SessionSet(repetitions = item.targetReps, weightKg = item.targetWeightKg)
                },
            )
        }

    fun fromWorkout(workout: WorkoutDto, defaultRestSeconds: Int): List<SessionExercise> =
        workout.exercises.sortedBy { it.order }.map { item ->
            SessionExercise(
                exerciseId = item.exerciseId,
                name = item.name,
                executionType = item.executionType,
                usesWeight = item.usesWeight,
                muscleGroup = item.muscleGroup,
                restSeconds = defaultRestSeconds,
                sets = item.sets.map { set ->
                    SessionSet(
                        repetitions = set.repetitions,
                        weightKg = set.weightKg,
                        durationSeconds = set.durationSeconds,
                    )
                }.ifEmpty { listOf(SessionSet()) },
            )
        }

    fun plannedSets(exercises: List<SessionExercise>): Int = exercises.sumOf { it.sets.size }

    fun plannedVolumeKg(exercises: List<SessionExercise>): Double = exercises.sumOf { exercise ->
        exercise.sets.sumOf { (it.repetitions ?: 0) * (it.weightKg ?: 0.0) }
    }

    fun estimatedSeconds(exercises: List<SessionExercise>, defaultRestSeconds: Int): Long =
        exercises.sumOf { exercise ->
            val rest = exercise.restSeconds ?: defaultRestSeconds
            val perSet = (exercise.sets.firstOrNull()?.durationSeconds ?: ESTIMATED_SET_SECONDS) + rest

            exercise.sets.size.toLong() * perSet
        }
}
