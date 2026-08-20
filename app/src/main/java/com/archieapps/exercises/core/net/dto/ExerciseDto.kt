package com.archieapps.exercises.core.net.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExerciseDto(
    val id: Int,
    val name: String,
    @SerialName("execution_type") val executionType: String = "sets",
    @SerialName("uses_weight") val usesWeight: Boolean = false,
    @SerialName("muscle_group") val muscleGroup: String? = null,
    val difficulty: String? = null,
    val equipment: String? = null,
    @SerialName("rest_seconds") val restSeconds: Int? = null,
    @SerialName("weight_options") val weightOptions: List<Double> = emptyList(),
    val notes: String? = null,
    @SerialName("last_performed_at") val lastPerformedAt: String? = null,
    @SerialName("sessions_count") val sessionsCount: Int = 0,
    @SerialName("best_weight_kg") val bestWeightKg: Double? = null,
    @SerialName("total_volume_kg") val totalVolumeKg: Double = 0.0,
    val muscles: ExerciseMusclesDto = ExerciseMusclesDto(),
    @SerialName("muscles_mapped") val musclesMapped: Boolean = false,
    @SerialName("muscles_unmapped_reason") val musclesUnmappedReason: String? = null,
)

@Serializable
data class CatalogMetaDto(
    @SerialName("muscle_groups") val muscleGroups: List<OptionDto> = emptyList(),
    val equipments: List<OptionDto> = emptyList(),
    @SerialName("execution_types") val executionTypes: List<String> = emptyList(),
    val difficulties: List<String> = emptyList(),
    @SerialName("effort_difficulties") val effortDifficulties: List<String> = emptyList(),
    @SerialName("workout_types") val workoutTypes: List<String> = emptyList(),
    val muscles: List<MuscleDto> = emptyList(),
    @SerialName("muscle_regions") val muscleRegions: List<String> = emptyList(),
    @SerialName("muscle_region_labels") val muscleRegionLabels: Map<String, String> = emptyMap(),
    @SerialName("activation_types") val activationTypes: List<String> = emptyList(),
    @SerialName("unmapped_exercises_count") val unmappedExercisesCount: Int = 0,
)

@Serializable
data class OptionDto(val value: String, val total: Int = 0)
