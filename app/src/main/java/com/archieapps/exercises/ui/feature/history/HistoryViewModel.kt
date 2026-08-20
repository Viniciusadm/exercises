package com.archieapps.exercises.ui.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archieapps.exercises.core.data.TemplateRepository
import com.archieapps.exercises.core.data.WorkoutRepository
import com.archieapps.exercises.core.net.ApiResult
import com.archieapps.exercises.core.net.dto.WorkoutDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HistoryUiState(
    val workouts: List<WorkoutDto> = emptyList(),
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val endReached: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val expandedId: Int? = null,
)

class HistoryViewModel(
    private val workouts: WorkoutRepository,
    private val templates: TemplateRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(HistoryUiState(loading = true))
    val state: StateFlow<HistoryUiState> = _state.asStateFlow()

    private var page = 1

    init {
        viewModelScope.launch {
            val cached = workouts.cachedFirstPage()
            if (cached.isNotEmpty()) _state.update { it.copy(workouts = cached) }
            refresh()
        }
    }

    fun refresh() {
        page = 1

        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }

            when (val result = workouts.page(page = 1)) {
                is ApiResult.Success -> _state.update {
                    it.copy(workouts = result.value, loading = false, endReached = result.value.isEmpty())
                }

                is ApiResult.Failure -> _state.update { it.copy(loading = false, error = result.message) }
            }
        }
    }

    fun loadMore() {
        val current = _state.value
        if (current.loading || current.loadingMore || current.endReached) return

        viewModelScope.launch {
            _state.update { it.copy(loadingMore = true) }

            when (val result = workouts.page(page = page + 1)) {
                is ApiResult.Success -> {
                    page++
                    _state.update {
                        it.copy(
                            workouts = it.workouts + result.value,
                            loadingMore = false,
                            endReached = result.value.isEmpty(),
                        )
                    }
                }

                is ApiResult.Failure -> _state.update { it.copy(loadingMore = false, error = result.message) }
            }
        }
    }

    fun toggleExpanded(id: Int) = _state.update { it.copy(expandedId = if (it.expandedId == id) null else id) }

    fun createTemplate(workout: WorkoutDto) {
        viewModelScope.launch {
            _state.update {
                when (val result = templates.fromWorkout(workout.id, null)) {
                    is ApiResult.Success -> it.copy(message = "Template \"${result.value.name}\" criado.")
                    is ApiResult.Failure -> it.copy(message = result.message)
                }
            }
        }
    }

    fun delete(workout: WorkoutDto) {
        viewModelScope.launch {
            when (val result = workouts.delete(workout.id)) {
                is ApiResult.Success -> {
                    _state.update { current ->
                        current.copy(
                            workouts = current.workouts.filterNot { it.id == workout.id },
                            expandedId = null,
                            message = "Treino excluído.",
                        )
                    }
                    workouts.page(page = 1)
                }

                is ApiResult.Failure -> _state.update { it.copy(message = result.message, error = result.message) }
            }
        }
    }

    fun dismissMessage() = _state.update { it.copy(message = null, error = null) }
}
