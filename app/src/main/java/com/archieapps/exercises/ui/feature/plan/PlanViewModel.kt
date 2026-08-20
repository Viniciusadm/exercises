package com.archieapps.exercises.ui.feature.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archieapps.exercises.core.data.AnatomyRepository
import com.archieapps.exercises.core.data.ExerciseRepository
import com.archieapps.exercises.core.data.PlanConfirmation
import com.archieapps.exercises.core.data.PlanDraft
import com.archieapps.exercises.core.data.PlanRepository
import com.archieapps.exercises.core.data.SessionRepository
import com.archieapps.exercises.core.data.TemplateRepository
import com.archieapps.exercises.core.db.ExerciseEntity
import com.archieapps.exercises.core.net.ApiResult
import com.archieapps.exercises.core.net.dto.PlanExerciseDto
import com.archieapps.exercises.core.net.dto.PlanMuscleDto
import com.archieapps.exercises.core.net.dto.PlanOptionsDto
import com.archieapps.exercises.core.net.dto.PlanProposalDto
import com.archieapps.exercises.core.net.dto.PlanRequest
import com.archieapps.exercises.core.settings.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PlanStep { FORM, GENERATING, REVIEW, DONE }

data class PlanUiState(
    val step: PlanStep = PlanStep.FORM,
    val options: PlanOptionsDto = PlanOptionsDto(),
    val objective: String = DEFAULT_OBJECTIVE,
    val daysPerWeek: Int = DEFAULT_DAYS,
    val minutesPerDay: Int = DEFAULT_MINUTES,
    val equipment: Set<String> = emptySet(),
    val equipmentOptions: List<String> = emptyList(),
    val draft: PlanDraft? = null,
    val confirmation: PlanConfirmation? = null,
    val swappingRef: String? = null,
    val swappingDay: Int = 0,
    val alternatives: List<PlanExerciseDto> = emptyList(),
    val sessionInProgress: Boolean = false,
    val confirming: Boolean = false,
    val message: String? = null,
) {
    val proposal: PlanProposalDto? get() = draft?.proposal

    val summary: String get() = proposal?.let { planSummary(it) } ?: ""

    val problems: List<String> get() = proposal?.let { planProblems(it) } ?: emptyList()

    val canConfirm: Boolean get() = proposal?.let { canConfirm(it) } == true && !confirming

    val newExercises: List<PlanExerciseDto> get() = proposal?.let { newCanonicals(it) } ?: emptyList()

    val warnings: List<String> get() = draft?.misc?.warnings ?: emptyList()

    val uncoverable: List<String> get() = draft?.misc?.uncoverable?.map { it.name } ?: emptyList()

    val byAi: Boolean get() = draft?.misc?.source == "ai"

    val createdTemplates: Int get() = confirmation?.templates?.size ?: 0

    val adoptedNames: List<String> get() = confirmation?.misc?.adopted?.map { it.name } ?: emptyList()

    val alreadyConfirmed: Boolean get() = confirmation?.misc?.created == false

    companion object {
        const val DEFAULT_OBJECTIVE = "hypertrophy"
        const val DEFAULT_DAYS = 3
        const val DEFAULT_MINUTES = 60
    }
}

private data class LocalState(
    val step: PlanStep = PlanStep.FORM,
    val options: PlanOptionsDto = PlanOptionsDto(),
    val objective: String? = null,
    val daysPerWeek: Int? = null,
    val minutesPerDay: Int? = null,
    val equipment: Set<String>? = null,
    val draft: PlanDraft? = null,
    val confirmation: PlanConfirmation? = null,
    val swappingRef: String? = null,
    val swappingDay: Int = 0,
    val confirming: Boolean = false,
    val message: String? = null,
)

class PlanViewModel(
    private val plans: PlanRepository,
    private val templates: TemplateRepository,
    private val exercises: ExerciseRepository,
    private val anatomy: AnatomyRepository,
    private val sessions: SessionRepository,
    private val settings: SettingsStore,
) : ViewModel() {
    private val local = MutableStateFlow(LocalState())

    val state: StateFlow<PlanUiState> = combine(
        local,
        exercises.catalog,
        anatomy.observeExerciseMuscles(),
        sessions.observeDraft(),
    ) { extra, catalog, muscleMap, session ->
        val objective = extra.objective ?: settings.current.planObjective
            ?: extra.options.defaultObjective.ifBlank { PlanUiState.DEFAULT_OBJECTIVE }
        val days = extra.daysPerWeek ?: settings.current.planDaysPerWeek ?: PlanUiState.DEFAULT_DAYS
        val minutes = extra.minutesPerDay ?: settings.current.planMinutesPerDay ?: PlanUiState.DEFAULT_MINUTES
        val equipment = extra.equipment ?: settings.current.planEquipment.toSet()

        PlanUiState(
            step = extra.step,
            options = extra.options,
            objective = objective,
            daysPerWeek = days,
            minutesPerDay = minutes,
            equipment = equipment,
            equipmentOptions = equipmentOptions(
                extra.options.equipment,
                catalog.mapNotNull { it.equipment },
            ),
            draft = extra.draft,
            confirmation = extra.confirmation,
            swappingRef = extra.swappingRef,
            swappingDay = extra.swappingDay,
            alternatives = alternatives(extra, catalog) { id -> primaryOf(muscleMap, id) },
            sessionInProgress = session != null,
            confirming = extra.confirming,
            message = extra.message,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlanUiState())

    init {
        viewModelScope.launch {
            plans.cachedOptions()?.let { cached -> local.update { it.copy(options = cached) } }

            (plans.options() as? ApiResult.Success)?.let { result ->
                local.update { it.copy(options = result.value) }
            }
        }

        viewModelScope.launch {
            plans.draft()?.let { draft ->
                local.update { it.copy(draft = draft, step = PlanStep.REVIEW) }
            }
        }
    }

    fun setObjective(value: String) = local.update { it.copy(objective = value) }

    fun setDaysPerWeek(value: Int) = local.update {
        it.copy(daysPerWeek = value.coerceIn(it.options.daysPerWeek.min, it.options.daysPerWeek.max))
    }

    fun setMinutesPerDay(value: Int) = local.update {
        it.copy(minutesPerDay = value.coerceIn(it.options.minutesPerDay.min, it.options.minutesPerDay.max))
    }

    fun toggleEquipment(value: String) = local.update { current ->
        val selected = current.equipment ?: settings.current.planEquipment.toSet()

        current.copy(equipment = if (value in selected) selected - value else selected + value)
    }

    fun clearEquipment() = local.update { it.copy(equipment = emptySet()) }

    fun generate() {
        val snapshot = state.value

        if (snapshot.step == PlanStep.GENERATING) return

        local.update { it.copy(step = PlanStep.GENERATING, message = null, confirmation = null) }

        viewModelScope.launch {
            settings.setPlanForm(
                snapshot.objective,
                snapshot.daysPerWeek,
                snapshot.minutesPerDay,
                snapshot.equipment.toList(),
            )

            val request = PlanRequest(
                objective = snapshot.objective,
                daysPerWeek = snapshot.daysPerWeek,
                minutesPerDay = snapshot.minutesPerDay,
                equipment = snapshot.equipment.toList(),
            )

            when (val result = plans.create(request)) {
                is ApiResult.Success -> local.update {
                    it.copy(step = PlanStep.REVIEW, draft = result.value, message = null)
                }

                is ApiResult.Failure -> local.update {
                    it.copy(step = PlanStep.FORM, message = result.message)
                }
            }
        }
    }

    fun backToForm() = local.update { it.copy(step = PlanStep.FORM, swappingRef = null) }

    fun discard() {
        local.update { it.copy(step = PlanStep.FORM, draft = null, swappingRef = null, message = null) }

        viewModelScope.launch { plans.clearDraft() }
    }

    fun setSets(dayNumber: Int, ref: String, sets: Int) =
        edit { withSets(it, dayNumber, ref, sets) }

    fun setReps(dayNumber: Int, ref: String, reps: Int?) =
        edit { withReps(it, dayNumber, ref, reps) }

    fun remove(dayNumber: Int, ref: String) =
        edit { withoutItem(it, dayNumber, ref) }

    fun openSwap(dayNumber: Int, ref: String) =
        local.update { it.copy(swappingDay = dayNumber, swappingRef = ref) }

    fun closeSwap() = local.update { it.copy(swappingRef = null) }

    fun applySwap(replacement: PlanExerciseDto) {
        val day = local.value.swappingDay
        val ref = local.value.swappingRef ?: return

        local.update { it.copy(swappingRef = null) }
        edit { withReplacement(it, day, ref, replacement) }
    }

    fun confirm() {
        val draft = local.value.draft ?: return

        if (local.value.confirming || !canConfirm(draft.proposal)) return

        local.update { it.copy(confirming = true, message = null) }

        viewModelScope.launch {
            when (val result = plans.confirm(toConfirmRequest(draft.proposal))) {
                is ApiResult.Success -> {
                    templates.list()

                    if (result.value.misc.adopted.isNotEmpty()) {
                        exercises.sync()
                        anatomy.refresh()
                    }

                    plans.clearDraft()

                    local.update {
                        it.copy(
                            step = PlanStep.DONE,
                            confirming = false,
                            confirmation = result.value,
                            draft = null,
                        )
                    }
                }

                is ApiResult.Failure -> local.update {
                    it.copy(confirming = false, message = result.message)
                }
            }
        }
    }

    fun reset() = local.update {
        LocalState(options = it.options, step = PlanStep.FORM)
    }

    fun dismissMessage() = local.update { it.copy(message = null) }

    private fun edit(transform: (PlanDraft) -> PlanDraft) {
        val current = local.value.draft ?: return
        val updated = transform(current)

        local.update { it.copy(draft = updated) }

        viewModelScope.launch { plans.saveDraft(updated) }
    }

    private fun alternatives(
        extra: LocalState,
        catalog: List<ExerciseEntity>,
        primaryOf: (Int) -> List<PlanMuscleDto>,
    ): List<PlanExerciseDto> {
        val ref = extra.swappingRef ?: return emptyList()
        val proposal = extra.draft?.proposal ?: return emptyList()
        val item = proposal.days
            .flatMap { it.exercises }
            .firstOrNull { it.ref == ref }
            ?: return emptyList()

        return alternativesFor(item, catalog, primaryOf, usedRefs(proposal))
    }

    private fun primaryOf(
        map: com.archieapps.exercises.core.data.ExerciseMuscleMap,
        exerciseId: Int,
    ): List<PlanMuscleDto> = map.of(exerciseId).primary.map { PlanMuscleDto(it.slug, it.name) }
}
