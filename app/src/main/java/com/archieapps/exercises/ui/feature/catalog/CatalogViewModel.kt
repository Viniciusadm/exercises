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
import com.archieapps.exercises.core.net.dto.MuscleDto
import com.archieapps.exercises.core.session.WeightLadder
import com.archieapps.exercises.ui.component.MuscleSelection
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
    val weightOptions: List<Double> = emptyList(),
    val notes: String = "",
    val muscles: MuscleSelection = MuscleSelection(),
) {
    val isValid: Boolean get() = name.isNotBlank()

    fun toRequest() = ExerciseRequest(
        name = name.trim(),
        executionType = executionType,
        usesWeight = usesWeight,
        muscleGroup = muscleGroup.trim().takeIf { it.isNotBlank() },
        equipment = equipment.trim().takeIf { it.isNotBlank() },
        restSeconds = restSeconds,
        weightOptions = WeightLadder.sanitize(weightOptions),
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
    val muscleCatalog: List<MuscleDto> = emptyList(),
    val formDirty: Boolean = false,
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
        anatomy.observeMuscleCatalog(),
        local,
    ) { catalog, muscleMap, muscleCatalog, extra ->
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
            muscleCatalog = muscleCatalog,
            formDirty = extra.form != null && extra.form != extra.pristineForm,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CatalogUiState())

    init {
        viewModelScope.launch {
            exercises.sync()
            if (anatomy.cachedMuscleCatalog().isEmpty()) anatomy.syncMuscleCatalog()
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

    fun startCreate() {
        val blank = ExerciseForm()
        local.update { it.copy(form = blank, pristineForm = blank) }
    }

    fun startEdit(exercise: ExerciseEntity) {
        val seeded = ExerciseForm(
            id = exercise.id,
            name = exercise.name,
            muscleGroup = exercise.muscleGroup.orEmpty(),
            equipment = exercise.equipment.orEmpty(),
            executionType = exercise.executionType,
            usesWeight = exercise.usesWeight,
            restSeconds = exercise.restSeconds ?: 90,
            weightOptions = exercise.weightOptions,
            notes = exercise.notes.orEmpty(),
            muscles = MuscleSelection.from(state.value.muscleMap.of(exercise.id)),
        )

        local.update { it.copy(form = seeded, pristineForm = seeded) }
    }

    fun retryMuscleCatalog() {
        viewModelScope.launch { anatomy.syncMuscleCatalog() }
    }

    fun updateForm(transform: (ExerciseForm) -> ExerciseForm) = local.update {
        it.copy(form = it.form?.let(transform))
    }

    fun cancelForm() = local.update { it.copy(form = null, pristineForm = null) }

    fun dismissMessage() = local.update { it.copy(message = null, isError = false) }

    fun submitForm() {
        val form = local.value.form ?: return
        val pristine = local.value.pristineForm
        if (!form.isValid) return

        viewModelScope.launch {
            local.update { it.copy(saving = true) }

            val result = if (form.id == null) {
                exercises.create(form.toRequest())
            } else {
                exercises.update(form.id, form.toRequest())
            }

            if (result is ApiResult.Failure) {
                local.update { it.copy(saving = false, message = result.message, isError = true) }
                return@launch
            }

            val saved = (result as ApiResult.Success).value

            val musclesChanged = form.muscles != (pristine?.muscles ?: MuscleSelection())
            val mapping = if (musclesChanged) {
                anatomy.syncExerciseMuscles(saved.id, form.muscles.toRequest())
            } else {
                null
            }

            val failedMapping = mapping as? ApiResult.Failure

            local.update {
                it.copy(
                    saving = false,
                    form = null,
                    pristineForm = null,
                    message = failedMapping
                        ?.let { failure ->
                            "\"${saved.name}\" salvo, mas o mapeamento de músculos falhou: ${failure.message}"
                        }
                        ?: "\"${saved.name}\" salvo.",
                    isError = failedMapping != null,
                )
            }
        }
    }

    fun deleteExercise() {
        val form = local.value.form ?: return
        val id = form.id ?: return

        viewModelScope.launch {
            local.update { it.copy(saving = true) }

            when (val result = exercises.delete(id)) {
                is ApiResult.Success -> local.update {
                    it.copy(
                        saving = false,
                        form = null,
                        pristineForm = null,
                        message = "\"${form.name.trim()}\" excluído.",
                        isError = false,
                    )
                }

                is ApiResult.Failure -> local.update {
                    it.copy(saving = false, message = result.message, isError = true)
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
        val pristineForm: ExerciseForm? = null,
        val saving: Boolean = false,
        val message: String? = null,
        val isError: Boolean = false,
    )
}
