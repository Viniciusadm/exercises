package com.archieapps.exercises.ui.feature.templates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archieapps.exercises.core.data.ExerciseRepository
import com.archieapps.exercises.core.data.TemplateRepository
import com.archieapps.exercises.core.db.ExerciseEntity
import com.archieapps.exercises.core.net.ApiResult
import com.archieapps.exercises.core.net.dto.TemplateExerciseDto
import com.archieapps.exercises.core.net.dto.TemplateExerciseRequest
import com.archieapps.exercises.core.net.dto.TemplateRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TemplateItemForm(
    val exerciseId: Int,
    val name: String,
    val muscleGroup: String? = null,
    val equipment: String? = null,
    val executionType: String = "sets",
    val usesWeight: Boolean = true,
    val targetSets: Int? = 3,
    val targetReps: Int? = 12,
    val targetWeightKg: Double? = null,
    val restSeconds: Int? = null,
    val notes: String? = null,
) {
    fun toRequest() = TemplateExerciseRequest(
        exerciseId = exerciseId,
        targetSets = targetSets,
        targetReps = targetReps,
        targetWeightKg = targetWeightKg,
        restSeconds = restSeconds,
        notes = notes?.takeIf { it.isNotBlank() },
    )
}

data class TemplateEditorUiState(
    val templateId: Int? = null,
    val name: String = "",
    val items: List<TemplateItemForm> = emptyList(),
    val catalog: List<ExerciseEntity> = emptyList(),
    val loading: Boolean = false,
    val saving: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false,
    val closed: Boolean = false,
) {
    val canSave: Boolean get() = name.isNotBlank() && items.isNotEmpty() && !saving
    val isNew: Boolean get() = templateId == null
}

class TemplateEditorViewModel(
    private val templateId: Int?,
    private val templates: TemplateRepository,
    private val exercises: ExerciseRepository,
) : ViewModel() {
    private val form = MutableStateFlow(FormState(loading = templateId != null))

    val state: StateFlow<TemplateEditorUiState> = combine(exercises.catalog, form) { catalog, current ->
        TemplateEditorUiState(
            templateId = templateId,
            name = current.name,
            items = current.items,
            catalog = catalog,
            loading = current.loading,
            saving = current.saving,
            message = current.message,
            isError = current.isError,
            closed = current.closed,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TemplateEditorUiState(loading = true))

    init {
        viewModelScope.launch {
            exercises.sync()

            if (templateId == null) return@launch

            val cached = templates.cached().firstOrNull { it.id == templateId }
            cached?.let(::fill)

            val fresh = (templates.list() as? ApiResult.Success)?.value?.firstOrNull { it.id == templateId }
            if (fresh != null) fill(fresh) else form.update { it.copy(loading = false) }
        }
    }

    fun setName(value: String) = form.update { it.copy(name = value) }

    fun addExercise(exercise: ExerciseEntity) = form.update { current ->
        current.copy(
            items = current.items + TemplateItemForm(
                exerciseId = exercise.id,
                name = exercise.name,
                muscleGroup = exercise.muscleGroup,
                equipment = exercise.equipment,
                executionType = exercise.executionType,
                usesWeight = exercise.usesWeight,
                targetWeightKg = exercise.bestWeightKg,
                restSeconds = exercise.restSeconds,
            ),
            name = current.name.ifBlank { "Treino ${exercise.muscleGroup ?: ""}".trim() },
        )
    }

    fun removeItem(index: Int) = form.update { current ->
        current.copy(items = current.items.filterIndexed { position, _ -> position != index })
    }

    fun moveItem(index: Int, delta: Int) = form.update { current ->
        val target = index + delta
        if (target !in current.items.indices) return@update current

        val reordered = current.items.toMutableList()
        reordered.add(target, reordered.removeAt(index))

        current.copy(items = reordered)
    }

    fun updateItem(index: Int, transform: (TemplateItemForm) -> TemplateItemForm) = form.update { current ->
        current.copy(
            items = current.items.mapIndexed { position, item ->
                if (position == index) transform(item) else item
            }
        )
    }

    fun dismissMessage() = form.update { it.copy(message = null, isError = false) }

    fun save() {
        val current = form.value
        if (current.name.isBlank() || current.items.isEmpty()) return

        viewModelScope.launch {
            form.update { it.copy(saving = true) }

            val request = TemplateRequest(
                name = current.name.trim(),
                exercises = current.items.map { it.toRequest() },
            )

            val result = if (templateId == null) {
                templates.create(request)
            } else {
                templates.update(templateId, request)
            }

            when (result) {
                is ApiResult.Success -> {
                    templates.list()
                    form.update { it.copy(saving = false, closed = true) }
                }

                is ApiResult.Failure -> form.update {
                    it.copy(saving = false, message = result.message, isError = true)
                }
            }
        }
    }

    fun delete() {
        val id = templateId ?: return

        viewModelScope.launch {
            form.update { it.copy(saving = true) }

            when (val result = templates.delete(id)) {
                is ApiResult.Success -> {
                    templates.list()
                    form.update { it.copy(saving = false, closed = true) }
                }

                is ApiResult.Failure -> form.update {
                    it.copy(saving = false, message = result.message, isError = true)
                }
            }
        }
    }

    private fun fill(template: com.archieapps.exercises.core.net.dto.TemplateDto) = form.update { current ->
        current.copy(
            loading = false,
            name = template.name,
            items = template.exercises.sortedBy { it.order }.map(::toForm),
        )
    }

    private fun toForm(item: TemplateExerciseDto) = TemplateItemForm(
        exerciseId = item.exerciseId,
        name = item.name,
        muscleGroup = item.muscleGroup,
        equipment = item.equipment,
        executionType = item.executionType,
        usesWeight = item.usesWeight,
        targetSets = item.targetSets,
        targetReps = item.targetReps,
        targetWeightKg = item.targetWeightKg,
        restSeconds = item.restSeconds,
        notes = item.notes,
    )

    private data class FormState(
        val name: String = "",
        val items: List<TemplateItemForm> = emptyList(),
        val loading: Boolean = false,
        val saving: Boolean = false,
        val message: String? = null,
        val isError: Boolean = false,
        val closed: Boolean = false,
    )
}
