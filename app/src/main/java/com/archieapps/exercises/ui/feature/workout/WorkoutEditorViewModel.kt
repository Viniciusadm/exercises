package com.archieapps.exercises.ui.feature.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archieapps.exercises.core.data.ExerciseRepository
import com.archieapps.exercises.core.data.WorkoutRepository
import com.archieapps.exercises.core.db.ExerciseEntity
import com.archieapps.exercises.core.net.ApiResult
import com.archieapps.exercises.core.net.ServerTime
import com.archieapps.exercises.core.net.dto.WorkoutDto
import com.archieapps.exercises.core.net.dto.WorkoutExerciseRequest
import com.archieapps.exercises.core.net.dto.WorkoutRequest
import com.archieapps.exercises.core.net.dto.WorkoutSetRequest
import com.archieapps.exercises.core.session.SessionExercise
import com.archieapps.exercises.core.session.SessionSet
import com.archieapps.exercises.core.session.WeightLadder
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WorkoutEditorUiState(
    val workoutId: Int = 0,
    val name: String = "",
    val date: LocalDate = ServerTime.today(),
    val durationMinutes: Int = 50,
    val notes: String = "",
    val exercises: List<SessionExercise> = emptyList(),
    val catalog: List<ExerciseEntity> = emptyList(),
    val loading: Boolean = true,
    val saving: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false,
    val closed: Boolean = false,
) {
    val countedSets: Int get() = exercises.sumOf { exercise -> exercise.sets.count { it.done } }

    val totalVolumeKg: Double
        get() = exercises.sumOf { exercise ->
            exercise.sets.filter { it.done }.sumOf { (it.repetitions ?: 0) * (it.weightKg ?: 0.0) }
        }

    val canSave: Boolean get() = name.isNotBlank() && countedSets > 0 && !saving

    fun weightOptionsFor(exerciseId: Int?): List<Double> =
        catalog.firstOrNull { it.id == exerciseId }?.weightOptions.orEmpty()
}

class WorkoutEditorViewModel(
    private val workoutId: Int,
    private val workouts: WorkoutRepository,
    private val exercises: ExerciseRepository,
) : ViewModel() {
    private val form = MutableStateFlow(FormState())

    val state: StateFlow<WorkoutEditorUiState> = combine(exercises.catalog, form) { catalog, current ->
        WorkoutEditorUiState(
            workoutId = workoutId,
            name = current.name,
            date = current.date,
            durationMinutes = current.durationMinutes,
            notes = current.notes,
            exercises = current.exercises,
            catalog = catalog,
            loading = current.loading,
            saving = current.saving,
            message = current.message,
            isError = current.isError,
            closed = current.closed,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkoutEditorUiState())

    init {
        viewModelScope.launch {
            when (val result = workouts.detail(workoutId)) {
                is ApiResult.Success -> fill(result.value)
                is ApiResult.Failure -> form.update {
                    it.copy(loading = false, message = result.message, isError = true)
                }
            }
        }
    }

    fun setName(value: String) = form.update { it.copy(name = value) }

    fun setNotes(value: String) = form.update { it.copy(notes = value) }

    fun shiftDate(days: Long) = form.update { it.copy(date = it.date.plusDays(days)) }

    fun changeDuration(delta: Int) = form.update {
        it.copy(durationMinutes = (it.durationMinutes + delta).coerceIn(1, 1440))
    }

    fun addExercise(exercise: ExerciseEntity) = form.update { current ->
        current.copy(
            exercises = current.exercises + SessionExercise(
                exerciseId = exercise.id,
                name = exercise.name,
                executionType = exercise.executionType,
                usesWeight = exercise.usesWeight,
                muscleGroup = exercise.muscleGroup,
                equipment = exercise.equipment,
                sets = listOf(SessionSet(repetitions = 12, weightKg = exercise.bestWeightKg, done = true)),
            )
        )
    }

    fun removeExercise(index: Int) = form.update { current ->
        current.copy(exercises = current.exercises.filterIndexed { position, _ -> position != index })
    }

    fun addSet(exerciseIndex: Int) = updateExercise(exerciseIndex) { exercise ->
        val reference = exercise.sets.lastOrNull()

        exercise.copy(
            sets = exercise.sets + SessionSet(
                repetitions = reference?.repetitions ?: 12,
                weightKg = reference?.weightKg,
                durationSeconds = reference?.durationSeconds,
                done = true,
            )
        )
    }

    fun removeSet(exerciseIndex: Int, setIndex: Int) = updateExercise(exerciseIndex) { exercise ->
        exercise.copy(sets = exercise.sets.filterIndexed { position, _ -> position != setIndex })
    }

    fun changeReps(exerciseIndex: Int, setIndex: Int, delta: Int) = updateSet(exerciseIndex, setIndex) { set ->
        set.copy(repetitions = ((set.repetitions ?: 0) + delta).coerceIn(0, 999))
    }

    fun changeWeight(exerciseIndex: Int, setIndex: Int, direction: Int) {
        val options = state.value.weightOptionsFor(
            state.value.exercises.getOrNull(exerciseIndex)?.exerciseId,
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

    fun changeDurationSeconds(exerciseIndex: Int, setIndex: Int, delta: Int) =
        updateSet(exerciseIndex, setIndex) { set ->
            set.copy(durationSeconds = ((set.durationSeconds ?: 0) + delta).coerceIn(0, 7200))
        }

    fun toggleSet(exerciseIndex: Int, setIndex: Int) = updateSet(exerciseIndex, setIndex) { set ->
        set.copy(done = !set.done)
    }

    fun dismissMessage() = form.update { it.copy(message = null, isError = false) }

    fun save() {
        val current = form.value
        if (current.name.isBlank()) return

        viewModelScope.launch {
            form.update { it.copy(saving = true) }

            when (val result = workouts.update(workoutId, current.toRequest())) {
                is ApiResult.Success -> {
                    workouts.page(page = 1)
                    form.update { it.copy(saving = false, closed = true) }
                }

                is ApiResult.Failure -> form.update {
                    it.copy(saving = false, message = result.message, isError = true)
                }
            }
        }
    }

    fun delete() {
        viewModelScope.launch {
            form.update { it.copy(saving = true) }

            when (val result = workouts.delete(workoutId)) {
                is ApiResult.Success -> {
                    workouts.page(page = 1)
                    form.update { it.copy(saving = false, closed = true) }
                }

                is ApiResult.Failure -> form.update {
                    it.copy(saving = false, message = result.message, isError = true)
                }
            }
        }
    }

    private fun fill(workout: WorkoutDto) = form.update {
        FormState(
            name = workout.name,
            date = LocalDate.parse(workout.date.take(10)),
            durationMinutes = workout.durationMinutes.coerceAtLeast(1),
            notes = workout.notes.orEmpty(),
            workoutType = workout.workoutType ?: "Musculação",
            templateId = workout.templateId,
            clientUuid = workout.clientUuid,
            loading = false,
            exercises = workout.exercises.sortedBy { entry -> entry.order }.map { entry ->
                SessionExercise(
                    exerciseId = entry.exerciseId,
                    name = entry.name,
                    executionType = entry.executionType,
                    usesWeight = entry.usesWeight,
                    muscleGroup = entry.muscleGroup,
                    difficulty = entry.difficulty,
                    notes = entry.notes,
                    sets = entry.sets.map { set ->
                        SessionSet(
                            repetitions = set.repetitions,
                            weightKg = set.weightKg,
                            durationSeconds = set.durationSeconds,
                            done = true,
                        )
                    },
                )
            },
        )
    }

    private fun updateExercise(index: Int, transform: (SessionExercise) -> SessionExercise) = form.update { current ->
        current.copy(
            exercises = current.exercises.mapIndexed { position, exercise ->
                if (position == index) transform(exercise) else exercise
            }
        )
    }

    private fun updateSet(exerciseIndex: Int, setIndex: Int, transform: (SessionSet) -> SessionSet) =
        updateExercise(exerciseIndex) { exercise ->
            exercise.copy(
                sets = exercise.sets.mapIndexed { position, set ->
                    if (position == setIndex) transform(set) else set
                }
            )
        }

    private data class FormState(
        val name: String = "",
        val date: LocalDate = ServerTime.today(),
        val durationMinutes: Int = 50,
        val notes: String = "",
        val workoutType: String = "Musculação",
        val templateId: Int? = null,
        val clientUuid: String? = null,
        val exercises: List<SessionExercise> = emptyList(),
        val loading: Boolean = true,
        val saving: Boolean = false,
        val message: String? = null,
        val isError: Boolean = false,
        val closed: Boolean = false,
    ) {
        fun toRequest() = WorkoutRequest(
            clientUuid = clientUuid ?: UUID.randomUUID().toString(),
            name = name.trim(),
            date = date.toString(),
            durationMinutes = durationMinutes,
            workoutType = workoutType,
            templateId = templateId,
            notes = notes.takeIf { it.isNotBlank() },
            exercises = exercises.mapNotNull { exercise ->
                val counted = exercise.sets.filter { it.done }

                if (counted.isEmpty()) {
                    null
                } else {
                    WorkoutExerciseRequest(
                        exerciseId = exercise.exerciseId,
                        name = exercise.name,
                        executionType = exercise.executionType,
                        usesWeight = exercise.usesWeight,
                        muscleGroup = exercise.muscleGroup,
                        difficulty = exercise.difficulty,
                        notes = exercise.notes?.takeIf { it.isNotBlank() },
                        sets = counted.mapIndexed { index, set ->
                            WorkoutSetRequest(
                                setNumber = index + 1,
                                repetitions = set.repetitions,
                                durationSeconds = set.durationSeconds,
                                weightKg = set.weightKg,
                            )
                        },
                    )
                }
            },
        )
    }
}
