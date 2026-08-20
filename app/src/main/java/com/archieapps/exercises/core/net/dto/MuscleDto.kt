package com.archieapps.exercises.core.net.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MuscleDto(
    val id: Int,
    val slug: String,
    val name: String,
    @SerialName("anatomical_name") val anatomicalName: String? = null,
    val region: String = "",
    @SerialName("region_label") val regionLabel: String? = null,
    @SerialName("parent_id") val parentId: Int? = null,
    @SerialName("parent_slug") val parentSlug: String? = null,
    @SerialName("legacy_muscle_group") val legacyMuscleGroup: String? = null,
    @SerialName("svg_regions") val svgRegions: List<String> = emptyList(),
    @SerialName("exercises_count") val exercisesCount: Int = 0,
)

@Serializable
data class ExerciseMusclesDto(
    val primary: List<MuscleDto> = emptyList(),
    val secondary: List<MuscleDto> = emptyList(),
    val stabilizer: List<MuscleDto> = emptyList(),
) {
    val isEmpty: Boolean get() = primary.isEmpty() && secondary.isEmpty() && stabilizer.isEmpty()

    val all: List<MuscleDto> get() = primary + secondary + stabilizer
}

@Serializable
data class ExerciseMuscleMappingDto(
    val exercise: MappedExerciseDto,
    val muscles: ExerciseMusclesDto = ExerciseMusclesDto(),
    @SerialName("muscles_mapped") val musclesMapped: Boolean = false,
    @SerialName("muscles_unmapped_reason") val musclesUnmappedReason: String? = null,
)

@Serializable
data class MappedExerciseDto(
    val id: Int,
    val name: String,
    @SerialName("muscle_group") val muscleGroup: String? = null,
    @SerialName("execution_type") val executionType: String = "sets",
)

@Serializable
data class MuscleAnalysisDto(
    val range: AnalysisRangeDto = AnalysisRangeDto(),
    val granularity: String = "as_mapped",
    val muscles: List<MuscleVolumeDto> = emptyList(),
    val regions: List<MuscleRegionSummaryDto> = emptyList(),
    val highlights: MuscleHighlightsDto = MuscleHighlightsDto(),
)

@Serializable
data class AnalysisRangeDto(
    val from: String = "",
    val to: String = "",
    val weeks: Int = 1,
)

@Serializable
data class MuscleVolumeDto(
    @SerialName("muscle_id") val muscleId: Int,
    val slug: String,
    val name: String,
    @SerialName("anatomical_name") val anatomicalName: String? = null,
    val region: String = "",
    @SerialName("region_label") val regionLabel: String = "",
    @SerialName("parent_id") val parentId: Int? = null,
    @SerialName("parent_slug") val parentSlug: String? = null,
    @SerialName("svg_regions") val svgRegions: List<String> = emptyList(),
    val sessions: Int = 0,
    val entries: Int = 0,
    val sets: Int = 0,
    val reps: Int = 0,
    val seconds: Int = 0,
    @SerialName("bodyweight_sets") val bodyweightSets: Int = 0,
    @SerialName("volume_kg") val volumeKg: Double = 0.0,
    @SerialName("weighted_sets") val weightedSets: Double = 0.0,
    @SerialName("sets_per_week") val setsPerWeek: Double = 0.0,
    @SerialName("share_of_region") val shareOfRegion: Double = 0.0,
    @SerialName("last_trained_at") val lastTrainedAt: String? = null,
    @SerialName("coverage_vs_region") val coverageVsRegion: MuscleCoverageDto? = null,
    @SerialName("coverage_vs_siblings") val coverageVsSiblings: MuscleCoverageDto? = null,
    @SerialName("relative_status") val relativeStatus: String = "",
    @SerialName("absolute_status") val absoluteStatus: String = "",
    @SerialName("target_sets_per_week") val targetSetsPerWeek: Double = 0.0,
    @SerialName("by_activation") val byActivation: Map<String, MuscleActivationTotalsDto> = emptyMap(),
)

@Serializable
data class MuscleCoverageDto(
    val value: Double = 0.0,
    val pct: Int = 0,
    @SerialName("baseline_sets_per_week") val baselineSetsPerWeek: Double = 0.0,
    val peers: Int = 0,
)

@Serializable
data class MuscleActivationTotalsDto(
    val sets: Int = 0,
    @SerialName("volume_kg") val volumeKg: Double = 0.0,
)

@Serializable
data class MuscleRegionSummaryDto(
    val region: String = "",
    val label: String = "",
    val sets: Int = 0,
    @SerialName("volume_kg") val volumeKg: Double = 0.0,
    @SerialName("muscles_count") val musclesCount: Int = 0,
    val untrained: Int = 0,
    @SerialName("stabilizer_only") val stabilizerOnly: Int = 0,
    val undertrained: Int = 0,
    @SerialName("median_sets_per_week") val medianSetsPerWeek: Double? = null,
)

@Serializable
data class MuscleHighlightDto(
    val slug: String,
    val name: String,
    @SerialName("region_label") val regionLabel: String = "",
    @SerialName("sets_per_week") val setsPerWeek: Double = 0.0,
    val pct: Int? = null,
    val message: String = "",
)

@Serializable
data class MuscleHighlightsDto(
    val untrained: List<MuscleHighlightDto> = emptyList(),
    @SerialName("stabilizer_only") val stabilizerOnly: List<MuscleHighlightDto> = emptyList(),
    val undertrained: List<MuscleHighlightDto> = emptyList(),
    val emphasized: List<MuscleHighlightDto> = emptyList(),
)

@Serializable
data class MuscleAttributionDto(
    val attribution: String = "per_muscle",
    @SerialName("double_counted") val doubleCounted: Boolean = false,
    @SerialName("session_volume_kg") val sessionVolumeKg: Double = 0.0,
    @SerialName("attributed_volume_kg") val attributedVolumeKg: Double = 0.0,
    @SerialName("attribution_factor") val attributionFactor: Double? = null,
    val unmapped: MuscleUnmappedDto = MuscleUnmappedDto(),
)

@Serializable
data class MuscleUnmappedDto(
    val sets: Int = 0,
    @SerialName("volume_kg") val volumeKg: Double = 0.0,
    @SerialName("exercise_names") val exerciseNames: List<String> = emptyList(),
)

@Serializable
data class MuscleHeatmapDto(
    val range: AnalysisRangeDto = AnalysisRangeDto(),
    val metric: String = "sets",
    @SerialName("max_value") val maxValue: Double = 0.0,
    val legend: HeatmapLegendDto = HeatmapLegendDto(),
    val regions: List<HeatmapRegionDto> = emptyList(),
)

@Serializable
data class HeatmapLegendDto(
    val min: Double = 0.0,
    val max: Double = 0.0,
    val unit: String = "",
)

@Serializable
data class HeatmapRegionDto(
    @SerialName("svg_region") val svgRegion: String,
    val value: Double = 0.0,
    val intensity: Double = 0.0,
    val muscles: List<HeatmapMuscleDto> = emptyList(),
)

@Serializable
data class HeatmapMuscleDto(
    val slug: String,
    val name: String,
    val value: Double = 0.0,
    @SerialName("relative_status") val relativeStatus: String = "",
)

@Serializable
data class MuscleRecommendationDto(
    val muscle: RecommendedMuscleDto,
    @SerialName("relative_status") val relativeStatus: String = "",
    @SerialName("absolute_status") val absoluteStatus: String = "",
    @SerialName("sets_per_week") val setsPerWeek: Double = 0.0,
    @SerialName("target_sets_per_week") val targetSetsPerWeek: Double = 0.0,
    @SerialName("coverage_pct") val coveragePct: Int? = null,
    val message: String = "",
    @SerialName("from_your_history") val fromYourHistory: List<RecommendedExerciseDto> = emptyList(),
    @SerialName("new_options") val newOptions: List<RecommendedExerciseDto> = emptyList(),
) {
    val hasSuggestions: Boolean get() = fromYourHistory.isNotEmpty() || newOptions.isNotEmpty()
}

@Serializable
data class RecommendedMuscleDto(
    val id: Int,
    val slug: String,
    val name: String,
    val region: String = "",
    @SerialName("region_label") val regionLabel: String = "",
)

@Serializable
data class RecommendedExerciseDto(
    @SerialName("exercise_id") val exerciseId: Int? = null,
    val name: String,
    val equipment: String? = null,
    @SerialName("execution_type") val executionType: String = "sets",
    @SerialName("secondary_count") val secondaryCount: Int = 0,
    @SerialName("isolation_rank") val isolationRank: Int = 0,
    @SerialName("sessions_count") val sessionsCount: Int = 0,
    @SerialName("last_performed_at") val lastPerformedAt: String? = null,
    @SerialName("days_since") val daysSince: Int? = null,
) {
    val inCatalog: Boolean get() = exerciseId != null
}

@Serializable
data class AdoptExerciseRequest(val canonical: String)
