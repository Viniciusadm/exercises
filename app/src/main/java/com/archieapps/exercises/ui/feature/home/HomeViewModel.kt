package com.archieapps.exercises.ui.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archieapps.exercises.core.data.AnatomyRepository
import com.archieapps.exercises.core.data.AuthRepository
import com.archieapps.exercises.core.data.CachedHome
import com.archieapps.exercises.core.data.HomeRepository
import com.archieapps.exercises.core.net.ApiResult
import com.archieapps.exercises.core.net.FailureKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.archieapps.exercises.core.net.dto.MuscleRecommendationDto
import com.archieapps.exercises.core.anatomy.BodyGeometry
import com.archieapps.exercises.core.anatomy.BodyGeometryLoader
import com.archieapps.exercises.ui.component.MuscleRegionValue

data class HomeUiState(
    val cached: CachedHome? = null,
    val refreshing: Boolean = false,
    val error: String? = null,
    val offline: Boolean = false,
    val neglected: List<MuscleRecommendationDto> = emptyList(),
    val geometry: BodyGeometry? = null,
    val muscleRegions: List<MuscleRegionValue> = emptyList(),
) {
    val showSkeleton: Boolean get() = cached == null && refreshing
    val showEmpty: Boolean get() = cached == null && !refreshing
}

class HomeViewModel(
    private val homeRepository: HomeRepository,
    private val authRepository: AuthRepository,
    private val anatomyRepository: AnatomyRepository,
    private val geometryLoader: BodyGeometryLoader,
) : ViewModel() {
    private val refreshing = MutableStateFlow(true)
    private val failure = MutableStateFlow<ApiResult.Failure?>(null)
    private val geometry = MutableStateFlow<BodyGeometry?>(null)

    val state: StateFlow<HomeUiState> = combine(
        homeRepository.observe(),
        anatomyRepository.observe(),
        geometry,
        refreshing,
        failure,
    ) { cached, anatomy, body, isRefreshing, fail ->
        val heatmap = anatomy?.snapshot?.heatmap

        HomeUiState(
            cached = cached,
            refreshing = isRefreshing,
            error = fail?.message,
            offline = fail?.kind == FailureKind.OFFLINE,
            neglected = anatomy?.snapshot?.recommendations.orEmpty().filter { it.hasSuggestions },
            geometry = body,
            muscleRegions = heatmap?.regions.orEmpty().mapNotNull { region ->
                val top = region.muscles.maxByOrNull { it.value } ?: return@mapNotNull null

                MuscleRegionValue(
                    regionId = region.svgRegion,
                    muscleName = top.name,
                    intensity = region.intensity.toFloat(),
                )
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState(refreshing = true))

    init {
        refresh()

        viewModelScope.launch { geometry.value = geometryLoader.load() }
    }

    fun refresh() {
        viewModelScope.launch {
            refreshing.value = true

            when (val result = homeRepository.refresh()) {
                is ApiResult.Success -> failure.value = null
                is ApiResult.Failure -> {
                    failure.value = result
                    if (result.kind == FailureKind.UNAUTHORIZED) authRepository.logout()
                }
            }

            refreshing.value = false
        }
    }
}
