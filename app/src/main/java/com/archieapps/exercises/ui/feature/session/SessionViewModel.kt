package com.archieapps.exercises.ui.feature.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archieapps.exercises.core.data.ExerciseRepository
import com.archieapps.exercises.core.data.FinishOutcome
import com.archieapps.exercises.core.data.SessionRepository
import com.archieapps.exercises.core.data.TemplateRepository
import com.archieapps.exercises.core.data.WorkoutRepository
import com.archieapps.exercises.core.db.ExerciseEntity
import com.archieapps.exercises.core.format.Format
import com.archieapps.exercises.core.net.ApiResult
import com.archieapps.exercises.core.rest.RestTimerBus
import com.archieapps.exercises.core.rest.RestTimerController
import com.archieapps.exercises.core.net.dto.ExerciseSessionDto
import com.archieapps.exercises.core.net.dto.TemplateDto
import com.archieapps.exercises.core.net.dto.WorkoutDto
import com.archieapps.exercises.core.session.ActiveSession
import com.archieapps.exercises.core.session.SessionExercise
import com.archieapps.exercises.core.session.SessionSet
import com.archieapps.exercises.core.settings.SettingsStore
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RestState(
    val exerciseName: String,
    val totalSeconds: Int,
    val remainingSeconds: Int,
) {
    val progress: Float get() = if (totalSeconds <= 0) 0f else remainingSeconds.toFloat() / totalSeconds
}

data class SessionUiState(
    val draft: ActiveSession? = null,
    val templates: List<TemplateDto> = emptyList(),
    val lastWorkout: WorkoutDto? = null,
    val recentWorkouts: List<WorkoutDto> = emptyList(),
    val catalog: List<ExerciseEntity> = emptyList(),
    val nowMillis: Long = System.currentTimeMillis(),
    val rest: RestState? = null,
    val preparing: Boolean = false,
    val saving: Boolean = false,
    val message: String? = null,
    val outcome: FinishOutcome? = null,
    val keepScreenOn: Boolean = true,
)

class SessionViewModel(
    private val sessions: SessionRepository,
    private val workouts: WorkoutRepository,
    private val templates: TemplateRepository,
    private val exercises: ExerciseRepository,
    private val settings: SettingsStore,
    private val restTimer: RestTimerController,
) : ViewModel() {
    private val transient = MutableStateFlow(TransientState())

    val state: StateFlow<SessionUiState> = combine(
        sessions.observeDraft(),
        exercises.catalog,
        transient,
        RestTimerBus.state,
    ) { draft, catalog, extra, rest ->
        SessionUiState(
            draft = draft,
            templates = extra.templates,
            lastWorkout = extra.lastWorkout,
            recentWorkouts = extra.recentWorkouts,
            catalog = catalog,
            nowMillis = extra.nowMillis,
            rest = rest?.let {
                RestState(
                    exerciseName = it.exerciseName,
                    totalSeconds = it.totalSeconds,
                    remainingSeconds = it.remainingSeconds(extra.nowMillis),
                )
            },
            preparing = extra.preparing,
            saving = extra.saving,
            message = extra.message,
            outcome = extra.outcome,
            keepScreenOn = settings.current.keepScreenOnDuringWorkout,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SessionUiState())

    init {
        loadStarters()
        startClock()
    }

    fun dismissMessage() = transient.update { it.copy(message = null) }

    fun startEmpty() {
        viewModelScope.launch { sessions.saveDraft(newDraft(name = "Treino", exercises = emptyList())) }
    }

    fun startFromTemplate(template: TemplateDto) {
        viewModelScope.launch {
            transient.update { it.copy(preparing = true) }

            val skeleton = template.exercises.sortedBy { it.order }.map { item ->
                SessionExercise(
                    exerciseId = item.exerciseId,
                    name = item.name,
                    executionType = item.executionType,
                    usesWeight = item.usesWeight,
                    muscleGroup = item.muscleGroup,
                    equipment = item.equipment,
                    restSeconds = item.restSeconds ?: settings.current.defaultRestSeconds,
                    targetReps = item.targetReps,
                    notes = item.notes,
                    sets = List((item.targetSets ?: 3).coerceIn(1, 12)) {
                        SessionSet(repetitions = item.targetReps, weightKg = item.targetWeightKg)
                    },
                )
            }

            sessions.saveDraft(newDraft(template.name, skeleton, template.id))
            enrich()
            transient.update { it.copy(preparing = false) }
        }
    }

    fun repeatLastWorkout() {
        val last = transient.value.lastWorkout

        if (last == null) {
            transient.update { it.copy(message = "Nenhum treino anterior para repetir.") }

            return
        }

        repeatWorkout(last)
    }

    fun repeatWorkout(workout: WorkoutDto) {
        viewModelScope.launch {
            transient.update { it.copy(preparing = true) }
            val last = workout

            val skeleton = last.exercises.sortedBy { it.order }.map { item ->
                SessionExercise(
                    exerciseId = item.exerciseId,
                    name = item.name,
                    executionType = item.executionType,
                    usesWeight = item.usesWeight,
                    muscleGroup = item.muscleGroup,
                    restSeconds = settings.current.defaultRestSeconds,
                    sets = item.sets.map { set ->
                        SessionSet(
                            repetitions = set.repetitions,
                            weightKg = set.weightKg,
                            durationSeconds = set.durationSeconds,
                        )
                    }.ifEmpty { listOf(SessionSet()) },
                )
            }

            sessions.saveDraft(newDraft(last.name, skeleton, last.templateId))
            enrich()
            transient.update { it.copy(preparing = false) }
        }
    }

    fun addExercise(exercise: ExerciseEntity) {
        mutate { draft ->
            draft.copy(
                exercises = draft.exercises + SessionExercise(
                    exerciseId = exercise.id,
                    name = exercise.name,
                    executionType = exercise.executionType,
                    usesWeight = exercise.usesWeight,
                    muscleGroup = exercise.muscleGroup,
                    equipment = exercise.equipment,
                    restSeconds = exercise.restSeconds ?: settings.current.defaultRestSeconds,
                    bestWeightKg = exercise.bestWeightKg,
                    sets = listOf(SessionSet(repetitions = 12, weightKg = exercise.bestWeightKg)),
                ),
                currentIndex = draft.exercises.size,
            )
        }

        viewModelScope.launch { enrich(onlyIndex = state.value.draft?.exercises?.lastIndex) }
    }

    fun removeExercise(index: Int) {
        mutate { draft ->
            val remaining = draft.exercises.filterIndexed { position, _ -> position != index }

            draft.copy(
                exercises = remaining,
                currentIndex = draft.currentIndex.coerceAtMost((remaining.size - 1).coerceAtLeast(0)),
            )
        }
    }

    fun selectExercise(index: Int) = mutate { it.copy(currentIndex = index) }

    fun addSet(exerciseIndex: Int) = mutate { draft ->
        draft.updateExercise(exerciseIndex) { exercise ->
            val reference = exercise.sets.lastOrNull()

            exercise.copy(
                sets = exercise.sets + SessionSet(
                    repetitions = reference?.repetitions ?: exercise.targetReps ?: 12,
                    weightKg = reference?.weightKg,
                    durationSeconds = reference?.durationSeconds,
                ),
            )
        }
    }

    fun removeSet(exerciseIndex: Int, setIndex: Int) = mutate { draft ->
        draft.updateExercise(exerciseIndex) { exercise ->
            exercise.copy(sets = exercise.sets.filterIndexed { position, _ -> position != setIndex })
        }
    }

    fun changeReps(exerciseIndex: Int, setIndex: Int, delta: Int) = updateSet(exerciseIndex, setIndex) { set ->
        set.copy(repetitions = ((set.repetitions ?: 0) + delta).coerceIn(0, 999))
    }

    fun changeWeight(exerciseIndex: Int, setIndex: Int, delta: Double) = updateSet(exerciseIndex, setIndex) { set ->
        set.copy(weightKg = ((set.weightKg ?: 0.0) + delta).coerceIn(0.0, 999.0))
    }

    fun changeDuration(exerciseIndex: Int, setIndex: Int, delta: Int) = updateSet(exerciseIndex, setIndex) { set ->
        set.copy(durationSeconds = ((set.durationSeconds ?: 0) + delta).coerceIn(0, 7200))
    }

    fun toggleSetDone(exerciseIndex: Int, setIndex: Int) {
        val draft = state.value.draft ?: return
        val exercise = draft.exercises.getOrNull(exerciseIndex) ?: return
        val wasDone = exercise.sets.getOrNull(setIndex)?.done ?: return

        updateSet(exerciseIndex, setIndex) { it.copy(done = !wasDone) }

        if (!wasDone) {
            restTimer.start(exercise.name, exercise.restSeconds ?: settings.current.defaultRestSeconds)
        }
    }

    fun addRest(seconds: Int) = restTimer.add(seconds)

    fun skipRest() = restTimer.skip()

    fun discard() {
        viewModelScope.launch {
            skipRest()
            sessions.discardDraft()
        }
    }

    fun finish() {
        val draft = state.value.draft ?: return

        viewModelScope.launch {
            transient.update { it.copy(saving = true) }
            val outcome = sessions.finish(draft, System.currentTimeMillis())

            transient.update {
                when (outcome) {
                    is FinishOutcome.Rejected -> it.copy(saving = false, message = outcome.reason)
                    else -> it.copy(saving = false, outcome = outcome)
                }
            }

            skipRest()
        }
    }

    fun acknowledgeOutcome() = transient.update { it.copy(outcome = null) }

    private fun startClock() {
        viewModelScope.launch {
            while (true) {
                transient.update { it.copy(nowMillis = System.currentTimeMillis()) }
                delay(1_000)
            }
        }
    }

    private fun loadStarters() {
        viewModelScope.launch {
            val cachedTemplates = templates.cached()
            if (cachedTemplates.isNotEmpty()) transient.update { it.copy(templates = cachedTemplates) }

            val cachedWorkouts = workouts.cachedFirstPage()
            if (cachedWorkouts.isNotEmpty()) {
                transient.update { it.copy(lastWorkout = cachedWorkouts.first(), recentWorkouts = cachedWorkouts) }
            }

            (templates.list() as? ApiResult.Success)?.let { result ->
                transient.update { it.copy(templates = result.value) }
            }

            (workouts.page(page = 1, perPage = 8) as? ApiResult.Success)?.value?.let { page ->
                transient.update { it.copy(lastWorkout = page.firstOrNull(), recentWorkouts = page) }
            }
        }
    }

    private suspend fun enrich(onlyIndex: Int? = null) {
        val draft = sessions.observeDraft().first() ?: return

        draft.exercises.forEachIndexed { index, exercise ->
            if (onlyIndex != null && index != onlyIndex) return@forEachIndexed

            val latest = sessions.latest(exercise.exerciseId, exercise.name) ?: return@forEachIndexed
            val previous = latest.sessions.firstOrNull()

            mutate { current ->
                current.updateExercise(index) { target ->
                    val weight = target.sets.firstOrNull()?.weightKg
                        ?: latest.suggestion?.weightKg
                        ?: previous?.topSet?.weightKg
                    val reps = target.targetReps ?: previous?.topSet?.repetitions

                    target.copy(
                        restSeconds = target.restSeconds ?: latest.restSeconds,
                        previousSummary = previous?.let(::summarize),
                        suggestion = latest.suggestion?.let { "${Format.weight(it.weightKg)} · ${it.reason}" },
                        bestWeightKg = latest.sessions.mapNotNull { it.topSet?.weightKg }.maxOrNull(),
                        sets = target.sets.map { set ->
                            set.copy(
                                weightKg = set.weightKg ?: weight,
                                repetitions = set.repetitions ?: reps,
                            )
                        }.ifEmpty {
                            List(previous?.sets?.size ?: 3) { SessionSet(repetitions = reps, weightKg = weight) }
                        },
                    )
                }
            }
        }
    }

    private fun summarize(session: ExerciseSessionDto): String {
        val weight = session.topSet?.weightKg
        val reps = session.topSet?.repetitions
        val date = Format.parseDate(session.date)?.let(Format::dayMonth)
        val core = listOfNotNull(
            session.sets.size.takeIf { it > 0 }?.let { "${it}×${reps ?: 0}" },
            weight?.let(Format::weight),
        ).joinToString(" · ")

        return listOfNotNull(core.takeIf { it.isNotBlank() }, date).joinToString(" — ")
    }

    private fun updateSet(exerciseIndex: Int, setIndex: Int, transform: (SessionSet) -> SessionSet) =
        mutate { draft ->
            draft.updateExercise(exerciseIndex) { exercise ->
                exercise.copy(
                    sets = exercise.sets.mapIndexed { position, set ->
                        if (position == setIndex) transform(set) else set
                    },
                )
            }
        }

    private fun mutate(transform: (ActiveSession) -> ActiveSession) {
        viewModelScope.launch {
            val draft = sessions.observeDraft().first() ?: return@launch
            sessions.saveDraft(transform(draft))
        }
    }

    private fun newDraft(name: String, exercises: List<SessionExercise>, templateId: Int? = null) = ActiveSession(
        clientUuid = UUID.randomUUID().toString(),
        name = name,
        workoutType = "Musculação",
        templateId = templateId,
        startedAtEpochMillis = System.currentTimeMillis(),
        exercises = exercises,
    )

    private data class TransientState(
        val templates: List<TemplateDto> = emptyList(),
        val lastWorkout: WorkoutDto? = null,
        val recentWorkouts: List<WorkoutDto> = emptyList(),
        val nowMillis: Long = System.currentTimeMillis(),
        val preparing: Boolean = false,
        val saving: Boolean = false,
        val message: String? = null,
        val outcome: FinishOutcome? = null,
    )
}
