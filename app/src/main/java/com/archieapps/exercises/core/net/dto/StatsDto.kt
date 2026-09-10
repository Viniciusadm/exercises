package com.archieapps.exercises.core.net.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StatsDto(
    val range: RangeDto? = null,
    val totals: TotalsDto = TotalsDto(),
    val averages: AveragesDto = AveragesDto(),
    @SerialName("last_workout") val lastWorkout: LastWorkoutDto? = null,
    @SerialName("days_since_last") val daysSinceLast: Int? = null,
    val streak: StreakDto = StreakDto(),
    @SerialName("by_weekday") val byWeekday: List<WeekdayDto> = emptyList(),
    @SerialName("by_muscle_group") val byMuscleGroup: List<MuscleGroupDto> = emptyList(),
    val monthly: List<MonthlyDto> = emptyList(),
    @SerialName("top_exercises") val topExercises: List<TopExerciseDto> = emptyList(),
    @SerialName("recent_records") val recentRecords: List<RecordDto> = emptyList(),
)

@Serializable
data class RangeDto(val from: String, val to: String)

@Serializable
data class TotalsDto(
    val sessions: Int = 0,
    val minutes: Int = 0,
    val exercises: Int = 0,
    val sets: Int = 0,
    val reps: Int = 0,
    @SerialName("volume_kg") val volumeKg: Double = 0.0,
    @SerialName("distance_km") val distanceKm: Double = 0.0,
    @SerialName("timed_seconds") val timedSeconds: Int = 0,
    @SerialName("avg_speed_kmh") val avgSpeedKmh: Double? = null,
)

@Serializable
data class AveragesDto(
    @SerialName("minutes_per_session") val minutesPerSession: Double = 0.0,
    @SerialName("volume_per_session") val volumePerSession: Double = 0.0,
    @SerialName("sessions_per_week") val sessionsPerWeek: Double = 0.0,
    @SerialName("distance_per_session") val distancePerSession: Double = 0.0,
)

@Serializable
data class LastWorkoutDto(
    val id: Int,
    val name: String,
    val date: String,
    @SerialName("workout_type") val workoutType: String? = null,
    val minutes: Int = 0,
    @SerialName("volume_kg") val volumeKg: Double = 0.0,
    @SerialName("exercise_count") val exerciseCount: Int = 0,
    @SerialName("muscle_groups") val muscleGroups: List<String> = emptyList(),
)

@Serializable
data class StreakDto(
    val days: Int = 0,
    val weeks: Int = 0,
    @SerialName("best_weeks") val bestWeeks: Int = 0,
)

@Serializable
data class WeekdayDto(
    val weekday: Int,
    val label: String,
    val sessions: Int = 0,
    @SerialName("volume_kg") val volumeKg: Double = 0.0,
    @SerialName("distance_km") val distanceKm: Double = 0.0,
)

@Serializable
data class MuscleGroupDto(
    @SerialName("muscle_group") val muscleGroup: String? = null,
    val entries: Int = 0,
    val sets: Int = 0,
    @SerialName("volume_kg") val volumeKg: Double = 0.0,
    val share: Double = 0.0,
)

@Serializable
data class MonthlyDto(
    val month: String,
    val sessions: Int = 0,
    val minutes: Int = 0,
    @SerialName("volume_kg") val volumeKg: Double = 0.0,
    @SerialName("distance_km") val distanceKm: Double = 0.0,
)

@Serializable
data class TopExerciseDto(
    @SerialName("exercise_id") val exerciseId: Int? = null,
    val name: String,
    val sessions: Int = 0,
    val sets: Int = 0,
    @SerialName("best_weight_kg") val bestWeightKg: Double? = null,
    @SerialName("volume_kg") val volumeKg: Double = 0.0,
)

@Serializable
data class RecordDto(
    @SerialName("exercise_id") val exerciseId: Int? = null,
    val name: String,
    val type: String,
    val value: Double,
    val previous: Double? = null,
    val date: String,
    @SerialName("workout_id") val workoutId: Int? = null,
)

@Serializable
data class CalendarDayDto(
    val date: String,
    val sessions: Int = 0,
    val minutes: Int = 0,
    val sets: Int = 0,
    @SerialName("volume_kg") val volumeKg: Double = 0.0,
    @SerialName("distance_km") val distanceKm: Double = 0.0,
)
