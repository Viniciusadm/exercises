package com.archieapps.exercises.ui.feature.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archieapps.exercises.core.data.ExerciseRepository
import com.archieapps.exercises.core.data.FinishOutcome
import com.archieapps.exercises.core.data.AnatomyRepository
import com.archieapps.exercises.core.data.ExerciseMuscleMap
import com.archieapps.exercises.core.data.SessionRepository
import com.archieapps.exercises.core.data.TemplateRepository
import com.archieapps.exercises.core.data.WorkoutRepository
import com.archieapps.exercises.core.db.ExerciseEntity
import com.archieapps.exercises.core.format.Format
import com.archieapps.exercises.core.session.DistanceLadder
import com.archieapps.exercises.core.net.ApiResult
import com.archieapps.exercises.core.rest.FinishSetBus
import com.archieapps.exercises.core.rest.RestTimerBus
import com.archieapps.exercises.core.rest.RestTimerController
import com.archieapps.exercises.core.net.dto.ExerciseSessionDto
import com.archieapps.exercises.core.net.dto.TemplateDto
import com.archieapps.exercises.core.net.dto.WorkoutDto
import com.archieapps.exercises.core.session.ActiveSession
import com.archieapps.exercises.core.session.SessionBuilder
import com.archieapps.exercises.core.session.ExecutionType
import com.archieapps.exercises.core.session.SessionEventType
import com.archieapps.exercises.core.session.SessionExercise
import com.archieapps.exercises.core.session.SessionPhase
import com.archieapps.exercises.core.session.SessionSet
import com.archieapps.exercises.core.session.SessionTimeBreakdown
import com.archieapps.exercises.core.session.SetRunState
import com.archieapps.exercises.core.session.WeightLadder
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.archieapps.exercises.core.anatomy.BodyGeometry
import com.archieapps.exercises.core.anatomy.BodyGeometryLoader

data class RestState(
    val exerciseName: String,
    val totalSeconds: Int,
    val remainingSeconds: Int,
) {
    val progress: Float get() = if (totalSeconds <= 0) 0f else remainingSeconds.toFloat() / totalSeconds
}

data class ExecutionState(
    val exerciseName: String,
    val setNumber: Int,
    val elapsedSeconds: Long,
)

data class SessionUiState(
    val draft: ActiveSession? = null,
    val pendingStart: ActiveSession? = null,
    val finished: ActiveSession? = null,
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
    val defaultRestSeconds: Int = 90,
    val muscleMap: ExerciseMuscleMap = ExerciseMuscleMap(),
    val geometry: BodyGeometry? = null,
) {
    val breakdown: SessionTimeBreakdown
        get() = draft?.let { SessionTimeBreakdown.of(it, nowMillis) } ?: SessionTimeBreakdown()

    val phase: SessionPhase get() = draft?.phase ?: SessionPhase.Idle

    val execution: ExecutionState?
        get() {
            val executing = phase as? SessionPhase.Executing ?: return null
            val exercise = draft?.exercises?.getOrNull(executing.exerciseIndex) ?: return null

            return ExecutionState(
                exerciseName = exercise.name,
                setNumber = executing.setIndex + 1,
                elapsedSeconds = ((nowMillis - executing.startedAtMillis) / 1_000L).coerceAtLeast(0L),
            )
        }

    fun elapsedSecondsOf(exerciseIndex: Int, setIndex: Int): Long? {
        val executing = phase as? SessionPhase.Executing ?: return null
        if (executing.exerciseIndex != exerciseIndex || executing.setIndex != setIndex) return null

        return ((nowMillis - executing.startedAtMillis) / 1_000L).coerceAtLeast(0L)
    }

    fun weightOptionsFor(exerciseId: Int?): List<Double> =
        catalog.firstOrNull { it.id == exerciseId }?.weightOptions.orEmpty()

    fun setRunState(exerciseIndex: Int, setIndex: Int): SetRunState =
        draft?.setRunState(exerciseIndex, setIndex) ?: SetRunState.Pending
}

class SessionViewModel(
    private val sessions: SessionRepository,
    private val workouts: WorkoutRepository,
    private val templates: TemplateRepository,
    private val exercises: ExerciseRepository,
    private val anatomy: AnatomyRepository,
    private val geometryLoader: BodyGeometryLoader,
    private val settings: SettingsStore,
    private val restTimer: RestTimerController,
) : ViewModel() {
    private val transient = MutableStateFlow(TransientState())
    private val mutations = Mutex()

    val state: StateFlow<SessionUiState> = combine(
        sessions.observeDraft(),
        exercises.catalog,
        anatomy.observeExerciseMuscles(),
        transient,
        RestTimerBus.state,
    ) { draft, catalog, muscleMap, extra, rest ->
        SessionUiState(
            draft = draft,
            pendingStart = extra.pendingStart,
            finished = extra.finished,
            templates = extra.templates,
            lastWorkout = extra.lastWorkout,
            recentWorkouts = extra.recentWorkouts,
            catalog = catalog,
            nowMillis = extra.nowMillis,
            rest = rest?.takeUnless { it.countUp }?.let {
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
            defaultRestSeconds = settings.current.defaultRestSeconds,
            muscleMap = muscleMap,
            geometry = extra.geometry,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SessionUiState())

    init {
        loadStarters()

        viewModelScope.launch {
            transient.value = transient.value.copy(geometry = geometryLoader.load())
        }
        startClock()
        observeFinishRequests()
    }

    fun dismissMessage() = transient.update { it.copy(message = null) }

    fun startEmpty() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val draft = newDraft(name = "Treino", exercises = emptyList(), startedAtMillis = now)

            sessions.saveDraft(draft.record(SessionEventType.SESSION_STARTED, now))
        }
    }

    fun previewTemplate(template: TemplateDto) {
        preview(template.name, template.id) {
            SessionBuilder.fromTemplate(template, settings.current.defaultRestSeconds)
        }
    }

    fun previewWorkout(workout: WorkoutDto) {
        preview(workout.name, workout.templateId) {
            SessionBuilder.fromWorkout(workout, settings.current.defaultRestSeconds)
        }
    }

    fun previewLastWorkout() {
        val last = transient.value.lastWorkout

        if (last == null) {
            transient.update { it.copy(message = "Nenhum treino anterior para repetir.") }

            return
        }

        previewWorkout(last)
    }

    fun cancelPreview() = transient.update { it.copy(pendingStart = null) }

    fun addPreviewExercise(exercise: ExerciseEntity) = transient.update { extra ->
        val pending = extra.pendingStart ?: return@update extra

        extra.copy(pendingStart = pending.copy(exercises = pending.exercises + sessionExercise(exercise)))
    }

    fun removePreviewExercise(index: Int) = transient.update { extra ->
        val pending = extra.pendingStart ?: return@update extra

        extra.copy(
            pendingStart = pending.copy(
                exercises = pending.exercises.filterIndexed { position, _ -> position != index },
            ),
        )
    }

    fun startPending() {
        val pending = transient.value.pendingStart ?: return

        viewModelScope.launch {
            val now = System.currentTimeMillis()

            sessions.saveDraft(
                pending
                    .copy(startedAtEpochMillis = now)
                    .record(SessionEventType.SESSION_STARTED, now),
            )
            transient.update { it.copy(pendingStart = null) }
        }
    }

    fun openExercise(index: Int) = mutate { it.copy(openExerciseIndex = index) }

    fun closeExercise() = mutate { it.copy(openExerciseIndex = null) }

    fun addExercise(exercise: ExerciseEntity) {
        mutate { draft ->
            draft.copy(
                exercises = draft.exercises + sessionExercise(exercise),
                openExerciseIndex = draft.exercises.size,
            )
        }

        viewModelScope.launch { enrichDraft(onlyIndex = state.value.draft?.exercises?.lastIndex) }
    }

    fun removeExercise(index: Int) {
        mutate { draft ->
            if (!draft.canRemoveExercise(index)) {
                return@mutate draft
            }

            draft.copy(
                exercises = draft.exercises.filterIndexed { position, _ -> position != index },
                openExerciseIndex = null,
            )
        }
    }

    fun addSet(exerciseIndex: Int) = mutate { draft ->
        draft.updateExercise(exerciseIndex) { exercise ->
            val reference = exercise.sets.lastOrNull()

            exercise.copy(sets = exercise.sets + newSet(exercise, reference))
        }
    }

    fun removeSet(exerciseIndex: Int, setIndex: Int) = mutate { draft ->
        if (!draft.canRemoveSet(exerciseIndex, setIndex)) {
            return@mutate draft
        }

        draft.updateExercise(exerciseIndex) { exercise ->
            exercise.copy(sets = exercise.sets.filterIndexed { position, _ -> position != setIndex })
        }
    }

    fun changeReps(exerciseIndex: Int, setIndex: Int, delta: Int) = updateSet(exerciseIndex, setIndex) { set ->
        set.copy(repetitions = ((set.repetitions ?: 0) + delta).coerceIn(0, 999))
    }

    fun changeWeight(exerciseIndex: Int, setIndex: Int, direction: Int) {
        val options = state.value.weightOptionsFor(
            state.value.draft?.exercises?.getOrNull(exerciseIndex)?.exerciseId,
        )

        updateSet(exerciseIndex, setIndex) { set ->
            set.copy(
                weightKg = if (direction >= 0) {
                    WeightLadder.up(set.weightKg, options)
                } else {
                    WeightLadder.down(set.weightKg, options)
                },
            )
        }
    }

    fun setWeight(exerciseIndex: Int, setIndex: Int, weightKg: Double) = updateSet(exerciseIndex, setIndex) { set ->
        set.copy(weightKg = weightKg.coerceIn(WeightLadder.MIN_KG, WeightLadder.MAX_KG))
    }

    fun changeDuration(exerciseIndex: Int, setIndex: Int, delta: Int) = updateSet(exerciseIndex, setIndex) { set ->
        set.copy(durationSeconds = ((set.durationSeconds ?: 0) + delta).coerceIn(0, 7200))
    }

    fun changeDistance(exerciseIndex: Int, setIndex: Int, direction: Int) = updateSet(exerciseIndex, setIndex) { set ->
        set.copy(
            distanceKm = if (direction >= 0) {
                DistanceLadder.up(set.distanceKm)
            } else {
                DistanceLadder.down(set.distanceKm)
            },
        )
    }

    fun setDistance(exerciseIndex: Int, setIndex: Int, distanceKm: Double) = updateSet(exerciseIndex, setIndex) { set ->
        set.copy(distanceKm = DistanceLadder.clamp(distanceKm))
    }

    fun startSet(exerciseIndex: Int, setIndex: Int) {
        viewModelScope.launch {
            val started = mutations.withLock {
                val draft = sessions.observeDraft().first() ?: return@launch

                if (draft.phase is SessionPhase.Executing) return@launch
                if (draft.exercises.getOrNull(exerciseIndex)?.sets?.getOrNull(setIndex) == null) return@launch

                val now = System.currentTimeMillis()

                sessions.saveDraft(
                    closeRest(draft, now, skipped = true)
                        .record(SessionEventType.SET_STARTED, now, exerciseIndex, setIndex)
                        .copy(phase = SessionPhase.Executing(exerciseIndex, setIndex, now)),
                )

                draft.exercises[exerciseIndex].name to now
            }

            restTimer.startExecution(started.first, started.second)
        }
    }

    fun startNextSet(exerciseIndex: Int) {
        val setIndex = state.value.draft?.nextPendingSet(exerciseIndex) ?: return

        startSet(exerciseIndex, setIndex)
    }

    fun finishSet() {
        viewModelScope.launch {
            val rest = mutations.withLock {
                val draft = sessions.observeDraft().first() ?: return@launch
                val executing = draft.phase as? SessionPhase.Executing ?: return@launch
                val exercise = draft.exercises.getOrNull(executing.exerciseIndex) ?: return@launch
                val now = System.currentTimeMillis()
                val restSeconds = (exercise.restSeconds ?: settings.current.defaultRestSeconds).coerceAtLeast(1)

                sessions.saveDraft(
                    draft.stampExecutingSet(now)
                        .record(SessionEventType.SET_FINISHED, now, executing.exerciseIndex, executing.setIndex)
                        .record(SessionEventType.REST_STARTED, now, executing.exerciseIndex, executing.setIndex)
                        .copy(
                            phase = SessionPhase.Resting(
                                exerciseIndex = executing.exerciseIndex,
                                setIndex = executing.setIndex,
                                startedAtMillis = now,
                                endsAtMillis = now + restSeconds * 1_000L,
                            ),
                        ),
                )

                exercise.name to restSeconds
            }

            restTimer.start(rest.first, rest.second)
        }
    }

    fun undoSet(exerciseIndex: Int, setIndex: Int) = updateSet(exerciseIndex, setIndex) { it.copy(done = false) }

    fun addRest(seconds: Int) {
        restTimer.add(seconds)

        mutate { draft ->
            val resting = draft.phase as? SessionPhase.Resting ?: return@mutate draft

            draft.copy(phase = resting.copy(endsAtMillis = resting.endsAtMillis + seconds * 1_000L))
        }
    }

    fun skipRest() {
        restTimer.skip()

        mutate { draft -> closeRest(draft, System.currentTimeMillis(), skipped = true) }
    }

    fun discard() {
        viewModelScope.launch {
            restTimer.skip()
            sessions.discardDraft()
        }
    }

    fun finish() {
        viewModelScope.launch {
            val closed = mutations.withLock {
                val draft = sessions.observeDraft().first() ?: return@launch
                val now = System.currentTimeMillis()
                val closed = closeRest(closeExecution(draft, now), now, skipped = true)
                    .record(SessionEventType.SESSION_FINISHED, now)
                    .copy(phase = SessionPhase.Idle)

                sessions.saveDraft(closed)

                closed
            }

            restTimer.skip()
            transient.update { it.copy(saving = true) }

            val finishedAt = closed.events.lastOrNull()?.atEpochMillis ?: System.currentTimeMillis()
            val outcome = sessions.finish(closed, finishedAt)

            transient.update {
                when (outcome) {
                    is FinishOutcome.Rejected -> it.copy(saving = false, message = outcome.reason)
                    else -> it.copy(saving = false, outcome = outcome, finished = closed)
                }
            }
        }
    }

    fun acknowledgeOutcome() = transient.update { it.copy(outcome = null, finished = null) }

    private fun preview(name: String, templateId: Int?, skeleton: () -> List<SessionExercise>) {
        viewModelScope.launch {
            transient.update { it.copy(preparing = true, pendingStart = null) }

            val base = newDraft(name, skeleton(), System.currentTimeMillis(), templateId)

            transient.update { it.copy(pendingStart = base) }

            val enriched = enrich(base)

            transient.update { extra ->
                if (extra.pendingStart?.clientUuid == enriched.clientUuid) {
                    extra.copy(pendingStart = enriched, preparing = false)
                } else {
                    extra.copy(preparing = false)
                }
            }
        }
    }

    private fun closeExecution(draft: ActiveSession, nowMillis: Long): ActiveSession {
        val executing = draft.phase as? SessionPhase.Executing ?: return draft

        return draft.stampExecutingSet(nowMillis)
            .record(SessionEventType.SET_FINISHED, nowMillis, executing.exerciseIndex, executing.setIndex)
            .copy(phase = SessionPhase.Idle)
    }

    private fun newSet(exercise: SessionExercise, reference: SessionSet?) = if (exercise.isTimed) {
        SessionSet(
            durationSeconds = reference?.durationSeconds ?: exercise.targetSeconds ?: 0,
            distanceKm = reference?.distanceKm ?: exercise.targetDistanceKm,
            weightKg = reference?.weightKg,
        )
    } else {
        SessionSet(
            repetitions = reference?.repetitions ?: exercise.targetReps ?: 12,
            weightKg = reference?.weightKg,
        )
    }

    private fun closeRest(draft: ActiveSession, nowMillis: Long, skipped: Boolean): ActiveSession {
        val resting = draft.phase as? SessionPhase.Resting ?: return draft

        return draft
            .record(
                type = SessionEventType.REST_FINISHED,
                atMillis = nowMillis,
                exerciseIndex = resting.exerciseIndex,
                setIndex = resting.setIndex,
                meta = if (skipped) mapOf("skipped" to "true") else null,
            )
            .copy(phase = SessionPhase.Idle)
    }

    private fun observeFinishRequests() {
        viewModelScope.launch {
            FinishSetBus.requestedAt.collect { requestedAt ->
                if (requestedAt == null) return@collect

                FinishSetBus.consume()
                finishSet()
            }
        }
    }

    private fun startClock() {
        viewModelScope.launch {
            while (true) {
                transient.update { it.copy(nowMillis = System.currentTimeMillis()) }
                reconcilePhase()
                delay(1_000)
            }
        }
    }

    private suspend fun reconcilePhase() {
        val snapshot = state.value.draft ?: return
        val open = snapshot.phase is SessionPhase.Resting || snapshot.phase is SessionPhase.Executing

        if (snapshot.events.isNotEmpty() && !open) return

        mutations.withLock {
            val draft = sessions.observeDraft().first() ?: return
            val now = System.currentTimeMillis()

            if (draft.events.isEmpty()) {
                sessions.saveDraft(
                    draft.record(SessionEventType.SESSION_STARTED, draft.startedAtEpochMillis),
                )

                return
            }

            (draft.phase as? SessionPhase.Executing)?.let { executing ->
                if (RestTimerBus.current == null) {
                    val name = draft.exercises.getOrNull(executing.exerciseIndex)?.name.orEmpty()

                    restTimer.startExecution(name, executing.startedAtMillis)
                }

                return
            }

            val resting = draft.phase as? SessionPhase.Resting ?: return

            if (resting.endsAtMillis <= now) {
                sessions.saveDraft(closeRest(draft, resting.endsAtMillis, skipped = false))

                return
            }

            if (RestTimerBus.current == null) {
                val name = draft.exercises.getOrNull(resting.exerciseIndex)?.name.orEmpty()

                restTimer.start(name, ((resting.endsAtMillis - now) / 1_000L).toInt().coerceAtLeast(1))
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

    private suspend fun enrichDraft(onlyIndex: Int? = null) {
        val draft = sessions.observeDraft().first() ?: return
        val enriched = enrich(draft, onlyIndex)

        mutate { current ->
            current.copy(
                exercises = current.exercises.mapIndexed { index, exercise ->
                    enriched.exercises.getOrNull(index)?.takeIf { it.name == exercise.name } ?: exercise
                },
            )
        }
    }

    private suspend fun enrich(session: ActiveSession, onlyIndex: Int? = null): ActiveSession {
        var result = session

        session.exercises.forEachIndexed { index, exercise ->
            if (onlyIndex != null && index != onlyIndex) return@forEachIndexed

            val latest = sessions.latest(exercise.exerciseId, exercise.name) ?: return@forEachIndexed
            val previous = latest.sessions.firstOrNull()

            result = result.updateExercise(index) { target ->
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

        return result
    }

    private fun summarize(session: ExerciseSessionDto): String {
        val weight = session.topSet?.weightKg
        val reps = session.topSet?.repetitions
        val date = Format.parseDate(session.date)?.let(Format::dayMonth)
        val core = listOfNotNull(
            session.sets.size.takeIf { it > 0 }?.let { "${it}×${reps ?: 0}" },
            weight?.let(Format::weight),
        ).joinToString(" · ")

        return listOfNotNull(core.takeIf { it.isNotBlank() }, date).joinToString(" · ")
    }

    private fun sessionExercise(exercise: ExerciseEntity): SessionExercise {
        val base = SessionExercise(
            exerciseId = exercise.id,
            name = exercise.name,
            executionType = exercise.executionType,
            usesWeight = exercise.usesWeight,
            tracksDistance = exercise.tracksDistance,
            muscleGroup = exercise.muscleGroup,
            equipment = exercise.equipment,
            restSeconds = exercise.restSeconds ?: settings.current.defaultRestSeconds,
            bestWeightKg = exercise.bestWeightKg,
        )

        return base.copy(sets = listOf(newSet(base, reference = null).copy(weightKg = exercise.bestWeightKg)))
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
            mutations.withLock {
                val draft = sessions.observeDraft().first() ?: return@withLock
                val next = transform(draft)

                if (next !== draft) sessions.saveDraft(next)
            }
        }
    }

    private fun newDraft(
        name: String,
        exercises: List<SessionExercise>,
        startedAtMillis: Long,
        templateId: Int? = null,
    ) = ActiveSession(
        clientUuid = UUID.randomUUID().toString(),
        name = name,
        workoutType = "Musculação",
        templateId = templateId,
        startedAtEpochMillis = startedAtMillis,
        exercises = exercises,
    )

    private data class TransientState(
        val pendingStart: ActiveSession? = null,
        val finished: ActiveSession? = null,
        val templates: List<TemplateDto> = emptyList(),
        val lastWorkout: WorkoutDto? = null,
        val recentWorkouts: List<WorkoutDto> = emptyList(),
        val nowMillis: Long = System.currentTimeMillis(),
        val preparing: Boolean = false,
        val saving: Boolean = false,
        val message: String? = null,
        val outcome: FinishOutcome? = null,
        val geometry: BodyGeometry? = null,
    )
}
