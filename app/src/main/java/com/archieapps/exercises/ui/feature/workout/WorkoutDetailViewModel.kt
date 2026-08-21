package com.archieapps.exercises.ui.feature.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archieapps.exercises.core.data.WorkoutRepository
import com.archieapps.exercises.core.net.ApiResult
import com.archieapps.exercises.core.net.dto.WorkoutDto
import com.archieapps.exercises.core.net.dto.WorkoutTimelineDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WorkoutDetailUiState(
    val workout: WorkoutDto? = null,
    val timeline: WorkoutTimelineDto? = null,
    val loading: Boolean = true,
    val adjusting: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false,
) {
    val hasSegregatedMetrics: Boolean get() = (workout?.metricsVersion ?: 1) >= 2
}

class WorkoutDetailViewModel(
    private val workoutId: Int,
    private val workouts: WorkoutRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(WorkoutDetailUiState())
    val state: StateFlow<WorkoutDetailUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun dismissMessage() = _state.update { it.copy(message = null) }

    fun reload() = load()

    fun adjustSegment(sequence: Int, durationSeconds: Int) {
        viewModelScope.launch {
            _state.update { it.copy(adjusting = true) }

            when (val result = workouts.adjustSegment(workoutId, sequence, durationSeconds)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(workout = result.value, adjusting = false) }
                    loadTimeline()
                }

                is ApiResult.Failure -> _state.update {
                    it.copy(adjusting = false, message = result.message, isError = true)
                }
            }
        }
    }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }

            when (val result = workouts.detail(workoutId)) {
                is ApiResult.Success -> _state.update {
                    it.copy(workout = result.value, loading = false, isError = false)
                }

                is ApiResult.Failure -> _state.update {
                    it.copy(loading = false, message = result.message, isError = true)
                }
            }

            loadTimeline()
        }
    }

    private suspend fun loadTimeline() {
        (workouts.timeline(workoutId) as? ApiResult.Success)?.let { result ->
            _state.update { it.copy(timeline = result.value) }
        }
    }
}
