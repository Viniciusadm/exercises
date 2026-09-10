package com.archieapps.exercises.core.net.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WorkoutDto(
    val id: Int,
    @SerialName("workout_template_id") val templateId: Int? = null,
    @SerialName("client_uuid") val clientUuid: String? = null,
    val name: String,
    val date: String,
    @SerialName("started_at") val startedAt: String? = null,
    @SerialName("finished_at") val finishedAt: String? = null,
    @SerialName("duration_minutes") val durationMinutes: Int = 0,
    @SerialName("metrics_version") val metricsVersion: Int = 1,
    val time: WorkoutTimeDto? = null,
    @SerialName("workout_type") val workoutType: String? = null,
    val notes: String? = null,
    @SerialName("exercise_count") val exerciseCount: Int = 0,
    @SerialName("sets_count") val setsCount: Int = 0,
    @SerialName("total_reps") val totalReps: Int = 0,
    @SerialName("total_volume_kg") val totalVolumeKg: Double = 0.0,
    @SerialName("total_distance_km") val totalDistanceKm: Double = 0.0,
    @SerialName("avg_speed_kmh") val avgSpeedKmh: Double? = null,
    @SerialName("muscle_groups") val muscleGroups: List<String> = emptyList(),
    val exercises: List<WorkoutExerciseDto> = emptyList(),
)

@Serializable
data class WorkoutTimeDto(
    @SerialName("total_seconds") val totalSeconds: Int = 0,
    @SerialName("execution_seconds") val executionSeconds: Int = 0,
    @SerialName("rest_seconds") val restSeconds: Int = 0,
    @SerialName("idle_seconds") val idleSeconds: Int = 0,
    @SerialName("execution_share") val executionShare: Float = 0f,
    @SerialName("rest_share") val restShare: Float = 0f,
    @SerialName("idle_share") val idleShare: Float = 0f,
    @SerialName("sets_count") val setsCount: Int = 0,
    @SerialName("rest_periods_count") val restPeriodsCount: Int = 0,
    @SerialName("avg_set_seconds") val avgSetSeconds: Int? = null,
    @SerialName("avg_rest_seconds") val avgRestSeconds: Int? = null,
)

@Serializable
data class WorkoutTimelineDto(
    val id: Int,
    @SerialName("metrics_version") val metricsVersion: Int = 1,
    @SerialName("started_at") val startedAt: String? = null,
    @SerialName("finished_at") val finishedAt: String? = null,
    val metrics: WorkoutTimeDto? = null,
    val segments: List<TimelineSegmentDto> = emptyList(),
)

@Serializable
data class TimelineSegmentDto(
    val sequence: Int,
    val phase: String,
    val label: String,
    val at: String,
    @SerialName("duration_seconds") val durationSeconds: Int = 0,
    @SerialName("exercise_name") val exerciseName: String? = null,
    @SerialName("set_number") val setNumber: Int? = null,
    val editable: Boolean = false,
    val adjusted: Boolean = false,
)

@Serializable
data class WorkoutExerciseDto(
    val id: Int? = null,
    @SerialName("exercise_id") val exerciseId: Int? = null,
    val name: String,
    @SerialName("execution_type") val executionType: String = "sets",
    @SerialName("uses_weight") val usesWeight: Boolean = false,
    @SerialName("tracks_distance") val tracksDistance: Boolean = false,
    @SerialName("muscle_group") val muscleGroup: String? = null,
    val difficulty: String? = null,
    val notes: String? = null,
    val order: Int = 0,
    @SerialName("total_reps") val totalReps: Int = 0,
    @SerialName("total_volume_kg") val totalVolumeKg: Double = 0.0,
    @SerialName("total_distance_km") val totalDistanceKm: Double = 0.0,
    @SerialName("total_seconds") val totalSeconds: Int = 0,
    @SerialName("avg_speed_kmh") val avgSpeedKmh: Double? = null,
    val sets: List<WorkoutSetDto> = emptyList(),
)

@Serializable
data class WorkoutSetDto(
    val id: Int? = null,
    @SerialName("set_number") val setNumber: Int,
    val repetitions: Int? = null,
    @SerialName("duration_seconds") val durationSeconds: Int? = null,
    @SerialName("distance_km") val distanceKm: Double? = null,
    @SerialName("weight_kg") val weightKg: Double? = null,
    @SerialName("speed_kmh") val speedKmh: Double? = null,
)

@Serializable
data class ExerciseHistoryDto(
    val exercise: HistoryExerciseDto,
    val records: HistoryRecordsDto = HistoryRecordsDto(),
    val trend: TrendDto = TrendDto(),
    val sessions: List<ExerciseSessionDto> = emptyList(),
)

@Serializable
data class HistoryExerciseDto(
    val id: Int,
    val name: String,
    @SerialName("execution_type") val executionType: String = "sets",
    @SerialName("uses_weight") val usesWeight: Boolean = false,
    @SerialName("tracks_distance") val tracksDistance: Boolean = false,
    @SerialName("muscle_group") val muscleGroup: String? = null,
    val equipment: String? = null,
    @SerialName("rest_seconds") val restSeconds: Int? = null,
)

@Serializable
data class HistoryRecordsDto(
    @SerialName("sessions_count") val sessionsCount: Int = 0,
    @SerialName("first_performed_at") val firstPerformedAt: String? = null,
    @SerialName("last_performed_at") val lastPerformedAt: String? = null,
    @SerialName("total_volume_kg") val totalVolumeKg: Double = 0.0,
    @SerialName("best_weight") val bestWeight: RecordMarkDto? = null,
    @SerialName("best_reps") val bestReps: RecordMarkDto? = null,
    @SerialName("best_estimated_1rm") val bestEstimated1rm: RecordMarkDto? = null,
    @SerialName("best_session_volume") val bestSessionVolume: RecordMarkDto? = null,
    @SerialName("total_distance_km") val totalDistanceKm: Double = 0.0,
    @SerialName("best_distance") val bestDistance: RecordMarkDto? = null,
    @SerialName("best_speed") val bestSpeed: RecordMarkDto? = null,
)

@Serializable
data class RecordMarkDto(
    val value: Double,
    val repetitions: Int? = null,
    @SerialName("weight_kg") val weightKg: Double? = null,
    @SerialName("distance_km") val distanceKm: Double? = null,
    val seconds: Int? = null,
    val date: String? = null,
    @SerialName("workout_id") val workoutId: Int? = null,
)

@Serializable
data class TrendDto(
    val direction: String = "flat",
    @SerialName("delta_kg") val deltaKg: Double = 0.0,
)

@Serializable
data class ExerciseSessionDto(
    @SerialName("workout_id") val workoutId: Int? = null,
    val date: String? = null,
    val difficulty: String? = null,
    val notes: String? = null,
    @SerialName("total_reps") val totalReps: Int = 0,
    @SerialName("total_volume_kg") val totalVolumeKg: Double = 0.0,
    @SerialName("total_distance_km") val totalDistanceKm: Double = 0.0,
    @SerialName("total_seconds") val totalSeconds: Int = 0,
    @SerialName("avg_speed_kmh") val avgSpeedKmh: Double? = null,
    @SerialName("estimated_1rm") val estimated1rm: Double? = null,
    @SerialName("top_set") val topSet: WorkoutSetDto? = null,
    val sets: List<WorkoutSetDto> = emptyList(),
)

@Serializable
data class LatestExerciseDto(
    @SerialName("exercise_id") val exerciseId: Int? = null,
    val name: String,
    val date: String? = null,
    @SerialName("execution_type") val executionType: String = "sets",
    @SerialName("uses_weight") val usesWeight: Boolean = false,
    @SerialName("tracks_distance") val tracksDistance: Boolean = false,
    val difficulty: String? = null,
    @SerialName("rest_seconds") val restSeconds: Int? = null,
    val sets: List<WorkoutSetDto> = emptyList(),
    val sessions: List<ExerciseSessionDto> = emptyList(),
    val suggestion: SuggestionDto? = null,
)

@Serializable
data class SuggestionDto(
    @SerialName("weight_kg") val weightKg: Double,
    val reason: String,
)

@Serializable
data class TemplateDto(
    val id: Int,
    val name: String,
    val exercises: List<TemplateExerciseDto> = emptyList(),
)

@Serializable
data class TemplateExerciseDto(
    val id: Int? = null,
    @SerialName("exercise_id") val exerciseId: Int,
    val name: String = "",
    @SerialName("execution_type") val executionType: String = "sets",
    @SerialName("uses_weight") val usesWeight: Boolean = false,
    @SerialName("tracks_distance") val tracksDistance: Boolean = false,
    @SerialName("muscle_group") val muscleGroup: String? = null,
    val equipment: String? = null,
    val order: Int = 0,
    @SerialName("target_sets") val targetSets: Int? = null,
    @SerialName("target_reps") val targetReps: Int? = null,
    @SerialName("target_seconds") val targetSeconds: Int? = null,
    @SerialName("target_weight_kg") val targetWeightKg: Double? = null,
    @SerialName("target_distance_km") val targetDistanceKm: Double? = null,
    @SerialName("rest_seconds") val restSeconds: Int? = null,
    val notes: String? = null,
)
