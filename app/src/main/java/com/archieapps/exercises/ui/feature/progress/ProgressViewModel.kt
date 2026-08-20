package com.archieapps.exercises.ui.feature.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archieapps.exercises.core.data.ExerciseRepository
import com.archieapps.exercises.core.data.WorkoutRepository
import com.archieapps.exercises.core.db.ExerciseEntity
import com.archieapps.exercises.core.net.ApiResult
import com.archieapps.exercises.core.net.dto.ExerciseHistoryDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProgressUiState(
    val catalog: List<ExerciseEntity> = emptyList(),
    val selectedId: Int? = null,
    val history: ExerciseHistoryDto? = null,
    val loading: Boolean = false,
    val error: String? = null,
)

class ProgressViewModel(
    private val exercises: ExerciseRepository,
    private val workouts: WorkoutRepository,
) : ViewModel() {
    private val selection = MutableStateFlow(Selection())

    val state: StateFlow<ProgressUiState> = combine(exercises.catalog, selection) { catalog, current ->
        ProgressUiState(
            catalog = catalog.sortedByDescending { it.totalVolumeKg },
            selectedId = current.exerciseId,
            history = current.history,
            loading = current.loading,
            error = current.error,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressUiState())

    init {
        viewModelScope.launch {
            exercises.sync()
            val candidate = exercises.catalog.first().maxByOrNull { it.totalVolumeKg }

            if (selection.value.exerciseId == null && candidate != null) select(candidate.id)
        }
    }

    fun select(exerciseId: Int) {
        if (selection.value.exerciseId == exerciseId && selection.value.history != null) return

        viewModelScope.launch {
            selection.update { it.copy(exerciseId = exerciseId, loading = true, error = null) }

            workouts.cachedHistory(exerciseId)?.let { cached ->
                selection.update { it.copy(history = cached) }
            }

            when (val result = workouts.history(exerciseId)) {
                is ApiResult.Success -> selection.update {
                    it.copy(history = result.value, loading = false, error = null)
                }

                is ApiResult.Failure -> selection.update { it.copy(loading = false, error = result.message) }
            }
        }
    }

    private data class Selection(
        val exerciseId: Int? = null,
        val history: ExerciseHistoryDto? = null,
        val loading: Boolean = false,
        val error: String? = null,
    )
}
