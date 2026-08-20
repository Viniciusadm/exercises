package com.archieapps.exercises.ui.feature.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archieapps.exercises.core.data.AnatomyRepository
import com.archieapps.exercises.core.data.ExerciseMuscleMap
import com.archieapps.exercises.core.data.ExerciseRepository
import com.archieapps.exercises.core.data.TemplateRepository
import com.archieapps.exercises.core.db.ExerciseEntity
import com.archieapps.exercises.core.net.ApiResult
import com.archieapps.exercises.core.net.dto.ExerciseRequest
import com.archieapps.exercises.core.net.dto.TemplateDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExerciseForm(
    val id: Int? = null,
    val name: String = "",
    val muscleGroup: String = "",
    val equipment: String = "",
    val executionType: String = "sets",
    val usesWeight: Boolean = true,
    val restSeconds: Int = 90,
    val notes: String = "",
) {
    val isValid: Boolean get() = name.isNotBlank()

    fun toRequest() = ExerciseRequest(
        name = name.trim(),
        executionType = executionType,
        usesWeight = usesWeight,
        muscleGroup = muscleGroup.trim().takeIf { it.isNotBlank() },
        equipment = equipment.trim().takeIf { it.isNotBlank() },
        restSeconds = restSeconds,
        notes = notes.trim().takeIf { it.isNotBlank() },
    )
}

data class CatalogUiState(
    val exercises: List<ExerciseEntity> = emptyList(),
    val muscleGroups: List<String> = emptyList(),
    val templates: List<TemplateDto> = emptyList(),
    val query: String = "",
    val muscleFilter: String? = null,
    val showTemplates: Boolean = false,
    val form: ExerciseForm? = null,
    val saving: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false,
    val muscleMap: ExerciseMuscleMap = ExerciseMuscleMap(),
)

class CatalogViewModel(
    private val exercises: ExerciseRepository,
    private val templates: TemplateRepository,
    private val anatomy: AnatomyRepository,
) : ViewModel() {
    private val local = MutableStateFlow(LocalState())

    val state: StateFlow<CatalogUiState> = combine(
        exercises.catalog,
        anatomy.observeExerciseMuscles(),
        local,
    ) { catalog, muscleMap, extra ->
        val filtered = catalog
            .filter { exercise ->
                extra.muscleFilter == null || exercise.muscleGroup == extra.muscleFilter
            }
            .filter { exercise ->
                extra.query.isBlank() ||
                    exercise.name.contains(extra.query, ignoreCase = true) ||
                    exercise.equipment?.contains(extra.query, ignoreCase = true) == true
            }

        CatalogUiState(
            exercises = filtered,
            muscleGroups = catalog.mapNotNull { it.muscleGroup }.distinct().sorted(),
            templates = extra.templates,
            query = extra.query,
            muscleFilter = extra.muscleFilter,
            showTemplates = extra.showTemplates,
            form = extra.form,
            saving = extra.saving,
            message = extra.message,
            isError = extra.isError,
            muscleMap = muscleMap,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CatalogUiState())

    init {
        viewModelScope.launch {
            exercises.sync()
            local.update { it.copy(templates = templates.cached()) }

            (templates.list() as? ApiResult.Success)?.let { result ->
                local.update { it.copy(templates = result.value) }
            }
        }
    }

    fun onQueryChange(value: String) = local.update { it.copy(query = value) }

    fun onMuscleFilter(value: String?) = local.update {
        it.copy(muscleFilter = if (it.muscleFilter == value) null else value)
    }

    fun showTemplates(show: Boolean) = local.update { it.copy(showTemplates = show) }

    fun startCreate() = local.update { it.copy(form = ExerciseForm()) }

    fun startEdit(exercise: ExerciseEntity) = local.update {
        it.copy(
            form = ExerciseForm(
                id = exercise.id,
                name = exercise.name,
                muscleGroup = exercise.muscleGroup.orEmpty(),
                equipment = exercise.equipment.orEmpty(),
                executionType = exercise.executionType,
                usesWeight = exercise.usesWeight,
                restSeconds = exercise.restSeconds ?: 90,
                notes = exercise.notes.orEmpty(),
            )
        )
    }

    fun updateForm(transform: (ExerciseForm) -> ExerciseForm) = local.update {
        it.copy(form = it.form?.let(transform))
    }

    fun cancelForm() = local.update { it.copy(form = null) }

    fun dismissMessage() = local.update { it.copy(message = null, isError = false) }

    fun submitForm() {
        val form = local.value.form ?: return
        if (!form.isValid) return

        viewModelScope.launch {
            local.update { it.copy(saving = true) }

            val result = if (form.id == null) {
                exercises.create(form.toRequest())
            } else {
                exercises.update(form.id, form.toRequest())
            }

            local.update {
                when (result) {
                    is ApiResult.Success -> it.copy(
                        saving = false,
                        form = null,
                        message = "\"${result.value.name}\" salvo.",
                        isError = false,
                    )

                    is ApiResult.Failure -> it.copy(saving = false, message = result.message, isError = true)
                }
            }
        }
    }

    fun deleteTemplate(template: TemplateDto) {
        viewModelScope.launch {
            when (val result = templates.delete(template.id)) {
                is ApiResult.Success -> {
                    val remaining = (templates.list() as? ApiResult.Success)?.value ?: emptyList()
                    local.update {
                        it.copy(templates = remaining, message = "Template removido.", isError = false)
                    }
                }

                is ApiResult.Failure -> local.update { it.copy(message = result.message, isError = true) }
            }
        }
    }

    private data class LocalState(
        val templates: List<TemplateDto> = emptyList(),
        val query: String = "",
        val muscleFilter: String? = null,
        val showTemplates: Boolean = false,
        val form: ExerciseForm? = null,
        val saving: Boolean = false,
        val message: String? = null,
        val isError: Boolean = false,
    )
}
