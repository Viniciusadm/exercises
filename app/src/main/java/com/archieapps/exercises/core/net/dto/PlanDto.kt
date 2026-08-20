package com.archieapps.exercises.core.net.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PlanOptionsDto(
    val objectives: List<PlanObjectiveDto> = emptyList(),
    @SerialName("days_per_week") val daysPerWeek: PlanRangeDto = PlanRangeDto(),
    @SerialName("minutes_per_day") val minutesPerDay: PlanRangeDto = PlanRangeDto(),
    @SerialName("default_objective") val defaultObjective: String = "hypertrophy",
    val equipment: List<String> = emptyList(),
    @SerialName("sets_per_exercise_range") val setsPerExerciseRange: List<Int> = emptyList(),
)

@Serializable
data class PlanObjectiveDto(
    val value: String,
    val label: String,
)

@Serializable
data class PlanRangeDto(
    val min: Int = 1,
    val max: Int = 6,
    val step: Int = 1,
    val default: Int = 1,
)

@Serializable
data class PlanProposalDto(
    @SerialName("plan_uuid") val planUuid: String = "",
    val source: String = "",
    val objective: String = "",
    @SerialName("days_per_week") val daysPerWeek: Int = 0,
    @SerialName("minutes_per_day") val minutesPerDay: Int = 0,
    @SerialName("sets_budget_per_day") val setsBudgetPerDay: Int = 0,
    val window: PlanWindowDto = PlanWindowDto(),
    val summary: String? = null,
    val days: List<PlanDayDto> = emptyList(),
)

@Serializable
data class PlanWindowDto(
    val from: String = "",
    val to: String = "",
    val weeks: Int = 1,
)

@Serializable
data class PlanDayDto(
    val day: Int = 1,
    val focus: String = "",
    val name: String = "",
    @SerialName("total_sets") val totalSets: Int = 0,
    @SerialName("estimated_minutes") val estimatedMinutes: Int = 0,
    val exercises: List<PlanExerciseDto> = emptyList(),
)

@Serializable
data class PlanExerciseDto(
    val ref: String,
    @SerialName("exercise_id") val exerciseId: Int? = null,
    val name: String = "",
    @SerialName("is_new") val isNew: Boolean = false,
    val equipment: String? = null,
    @SerialName("execution_type") val executionType: String = "sets",
    @SerialName("uses_weight") val usesWeight: Boolean = true,
    val compound: Boolean = false,
    val region: String? = null,
    val muscles: PlanMusclesDto = PlanMusclesDto(),
    @SerialName("target_sets") val targetSets: Int = 3,
    @SerialName("target_reps") val targetReps: Int? = null,
    @SerialName("target_weight_kg") val targetWeightKg: Double? = null,
    @SerialName("rest_seconds") val restSeconds: Int? = null,
    val notes: String? = null,
)

@Serializable
data class PlanMusclesDto(
    val primary: List<PlanMuscleDto> = emptyList(),
    val secondary: List<PlanMuscleDto> = emptyList(),
)

@Serializable
data class PlanMuscleDto(
    val slug: String,
    val name: String,
)

@Serializable
data class PlanMiscDto(
    val source: String = "",
    val model: String? = null,
    val uncoverable: List<PlanUncoverableDto> = emptyList(),
    val warnings: List<String> = emptyList(),
    @SerialName("new_exercises_count") val newExercisesCount: Int = 0,
    @SerialName("pool_size") val poolSize: Int = 0,
    @SerialName("deficits_considered") val deficitsConsidered: Int = 0,
)

@Serializable
data class PlanUncoverableDto(
    val slug: String,
    val name: String,
    val status: String = "",
)

@Serializable
data class PlanRequest(
    val objective: String,
    @SerialName("days_per_week") val daysPerWeek: Int,
    @SerialName("minutes_per_day") val minutesPerDay: Int,
    val equipment: List<String> = emptyList(),
)

@Serializable
data class PlanConfirmRequest(
    @SerialName("plan_uuid") val planUuid: String,
    val source: String,
    val days: List<PlanConfirmDayRequest>,
)

@Serializable
data class PlanConfirmDayRequest(
    val name: String,
    val exercises: List<PlanConfirmExerciseRequest>,
)

@Serializable
data class PlanConfirmExerciseRequest(
    val ref: String,
    @SerialName("target_sets") val targetSets: Int? = null,
    @SerialName("target_reps") val targetReps: Int? = null,
    @SerialName("target_weight_kg") val targetWeightKg: Double? = null,
    @SerialName("rest_seconds") val restSeconds: Int? = null,
    val notes: String? = null,
)

@Serializable
data class PlanConfirmMiscDto(
    val created: Boolean = false,
    val adopted: List<PlanAdoptedDto> = emptyList(),
    @SerialName("template_ids") val templateIds: List<Int> = emptyList(),
)

@Serializable
data class PlanAdoptedDto(
    val ref: String,
    @SerialName("exercise_id") val exerciseId: Int,
    val name: String,
)
