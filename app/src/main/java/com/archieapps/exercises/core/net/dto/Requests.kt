package com.archieapps.exercises.core.net.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WorkoutRequest(
    @SerialName("client_uuid") val clientUuid: String,
    val name: String,
    val date: String,
    @SerialName("started_at") val startedAt: String? = null,
    @SerialName("finished_at") val finishedAt: String? = null,
    @SerialName("duration_minutes") val durationMinutes: Int? = null,
    @SerialName("workout_type") val workoutType: String,
    @SerialName("workout_template_id") val templateId: Int? = null,
    val notes: String? = null,
    val exercises: List<WorkoutExerciseRequest>,
    val events: List<WorkoutEventRequest> = emptyList(),
)

@Serializable
data class WorkoutEventRequest(
    val sequence: Int,
    val type: String,
    @SerialName("occurred_at") val occurredAt: String,
    @SerialName("exercise_order") val exerciseOrder: Int? = null,
    @SerialName("exercise_name") val exerciseName: String? = null,
    @SerialName("set_number") val setNumber: Int? = null,
    val meta: Map<String, String>? = null,
)

@Serializable
data class WorkoutExerciseRequest(
    @SerialName("exercise_id") val exerciseId: Int? = null,
    val name: String,
    @SerialName("execution_type") val executionType: String,
    @SerialName("uses_weight") val usesWeight: Boolean,
    @SerialName("tracks_distance") val tracksDistance: Boolean = false,
    @SerialName("muscle_group") val muscleGroup: String? = null,
    val difficulty: String? = null,
    val notes: String? = null,
    val sets: List<WorkoutSetRequest>,
)

@Serializable
data class WorkoutSetRequest(
    @SerialName("set_number") val setNumber: Int,
    val repetitions: Int? = null,
    @SerialName("duration_seconds") val durationSeconds: Int? = null,
    @SerialName("distance_km") val distanceKm: Double? = null,
    @SerialName("weight_kg") val weightKg: Double? = null,
)

@Serializable
data class SegmentAdjustmentRequest(
    val sequence: Int,
    @SerialName("duration_seconds") val durationSeconds: Int,
)

@Serializable
data class ExerciseRequest(
    val name: String,
    @SerialName("execution_type") val executionType: String,
    @SerialName("uses_weight") val usesWeight: Boolean,
    @SerialName("tracks_distance") val tracksDistance: Boolean = false,
    @SerialName("muscle_group") val muscleGroup: String? = null,
    val difficulty: String? = null,
    val equipment: String? = null,
    @SerialName("rest_seconds") val restSeconds: Int? = null,
    @SerialName("weight_options") val weightOptions: List<Double> = emptyList(),
    val notes: String? = null,
)

@Serializable
data class TemplateRequest(
    val name: String,
    val exercises: List<TemplateExerciseRequest>,
)

@Serializable
data class TemplateExerciseRequest(
    @SerialName("exercise_id") val exerciseId: Int,
    @SerialName("target_sets") val targetSets: Int? = null,
    @SerialName("target_reps") val targetReps: Int? = null,
    @SerialName("target_seconds") val targetSeconds: Int? = null,
    @SerialName("target_weight_kg") val targetWeightKg: Double? = null,
    @SerialName("target_distance_km") val targetDistanceKm: Double? = null,
    @SerialName("rest_seconds") val restSeconds: Int? = null,
    val notes: String? = null,
)

@Serializable
data class NameRequest(val name: String? = null)

@Serializable
data class ExerciseMusclesRequest(
    val primary: List<Int> = emptyList(),
    val secondary: List<Int> = emptyList(),
    val stabilizer: List<Int> = emptyList(),
)
