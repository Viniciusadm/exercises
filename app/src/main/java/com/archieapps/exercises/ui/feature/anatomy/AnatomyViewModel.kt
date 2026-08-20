package com.archieapps.exercises.ui.feature.anatomy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archieapps.exercises.core.data.AnatomyRepository
import com.archieapps.exercises.core.net.ApiResult
import com.archieapps.exercises.core.net.dto.AnalysisRangeDto
import com.archieapps.exercises.core.net.dto.MuscleAttributionDto
import com.archieapps.exercises.core.net.dto.MuscleHighlightsDto
import com.archieapps.exercises.core.net.dto.MuscleRegionSummaryDto
import com.archieapps.exercises.core.net.dto.MuscleVolumeDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.archieapps.exercises.core.anatomy.BodyGeometry
import com.archieapps.exercises.core.anatomy.BodyGeometryLoader
import com.archieapps.exercises.core.anatomy.BodyView
import com.archieapps.exercises.core.net.dto.MuscleHeatmapDto
import com.archieapps.exercises.ui.component.MuscleRegionValue
import com.archieapps.exercises.core.net.dto.MuscleRecommendationDto

data class AnatomyUiState(
    val geometry: BodyGeometry? = null,
    val view: BodyView = BodyView.FRONT,
    val heatmap: MuscleHeatmapDto = MuscleHeatmapDto(),
    val selectedRegionId: String? = null,
    val range: AnalysisRangeDto = AnalysisRangeDto(),
    val muscles: List<MuscleVolumeDto> = emptyList(),
    val regions: List<MuscleRegionSummaryDto> = emptyList(),
    val highlights: MuscleHighlightsDto = MuscleHighlightsDto(),
    val attribution: MuscleAttributionDto = MuscleAttributionDto(),
    val recommendations: List<MuscleRecommendationDto> = emptyList(),
    val openedRecommendation: String? = null,
    val adopting: String? = null,
    val selectedRegion: String? = null,
    val syncedAt: Long? = null,
    val loading: Boolean = false,
    val message: String? = null,
) {
    val visibleMuscles: List<MuscleVolumeDto>
        get() = selectedRegion?.let { region -> muscles.filter { it.region == region } } ?: muscles

    val isEmpty: Boolean get() = muscles.isEmpty()

    val openedSuggestions: MuscleRecommendationDto?
        get() = openedRecommendation?.let { slug -> recommendations.firstOrNull { it.muscle.slug == slug } }

    val selectedMuscle: MuscleVolumeDto?
        get() = selectedRegionId?.let { region ->
            val slugs = heatmap.regions.firstOrNull { it.svgRegion == region }?.muscles?.map { it.slug }.orEmpty()

            muscles.filter { it.slug in slugs }.maxByOrNull { it.setsPerWeek }
        }

    val regionValues: List<MuscleRegionValue>
        get() = heatmap.regions.mapNotNull { region ->
            val top = region.muscles.maxByOrNull { it.value } ?: return@mapNotNull null

            MuscleRegionValue(
                regionId = region.svgRegion,
                muscleName = top.name,
                intensity = region.intensity.toFloat(),
                detail = "${trimZero(top.value)} ${shortUnit(heatmap.legend.unit)}",
            )
        }
}

private fun shortUnit(unit: String) = when (unit) {
    "kg" -> "kg"
    "x" -> "x"
    else -> "sér/sem"
}

private data class LocalState(
    val openedRecommendation: String? = null,
    val adopting: String? = null,
    val selectedRegion: String? = null,
    val view: BodyView = BodyView.FRONT,
    val selectedRegionId: String? = null,
    val geometry: BodyGeometry? = null,
    val loading: Boolean = false,
    val message: String? = null,
)

class AnatomyViewModel(
    private val anatomy: AnatomyRepository,
    private val geometryLoader: BodyGeometryLoader,
) : ViewModel() {
    private val local = MutableStateFlow(LocalState())

    val state: StateFlow<AnatomyUiState> = combine(anatomy.observe(), local) { cached, extra ->
        AnatomyUiState(
            geometry = extra.geometry,
            view = extra.view,
            heatmap = cached?.snapshot?.heatmap ?: MuscleHeatmapDto(),
            selectedRegionId = extra.selectedRegionId,
            range = cached?.snapshot?.analysis?.range ?: AnalysisRangeDto(),
            muscles = cached?.snapshot?.analysis?.muscles ?: emptyList(),
            regions = cached?.snapshot?.analysis?.regions ?: emptyList(),
            highlights = cached?.snapshot?.analysis?.highlights ?: MuscleHighlightsDto(),
            attribution = cached?.snapshot?.attribution ?: MuscleAttributionDto(),
            recommendations = cached?.snapshot?.recommendations ?: emptyList(),
            openedRecommendation = extra.openedRecommendation,
            adopting = extra.adopting,
            selectedRegion = extra.selectedRegion,
            syncedAt = cached?.syncedAt,
            loading = extra.loading,
            message = extra.message,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AnatomyUiState())

    init {
        refresh()

        viewModelScope.launch {
            local.value = local.value.copy(geometry = geometryLoader.load())
        }
    }

    fun selectView(view: BodyView) {
        local.value = local.value.copy(view = view, selectedRegionId = null)
    }

    fun selectRegionId(regionId: String?) {
        local.value = local.value.copy(
            selectedRegionId = if (local.value.selectedRegionId == regionId) null else regionId
        )
    }

    fun refresh() {
        if (local.value.loading) return

        local.value = local.value.copy(loading = true, message = null)

        viewModelScope.launch {
            val result = anatomy.refresh()

            local.value = local.value.copy(
                loading = false,
                message = (result as? ApiResult.Failure)?.message,
            )
        }
    }

    fun selectRegion(region: String?) {
        local.value = local.value.copy(
            selectedRegion = if (local.value.selectedRegion == region) null else region
        )
    }

    fun openSuggestions(slug: String?) {
        local.value = local.value.copy(
            openedRecommendation = if (local.value.openedRecommendation == slug) null else slug
        )
    }

    fun adopt(canonical: String) {
        if (local.value.adopting != null) return

        local.value = local.value.copy(adopting = canonical, message = null)

        viewModelScope.launch {
            val message = when (val result = anatomy.adopt(canonical)) {
                is ApiResult.Success -> "${result.value.name} entrou no seu catálogo."
                is ApiResult.Failure -> result.message
            }

            local.value = local.value.copy(adopting = null, message = message)
            anatomy.refresh()
        }
    }

    fun dismissMessage() {
        local.value = local.value.copy(message = null)
    }
}
