package com.archieapps.exercises.ui.feature.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.archieapps.exercises.R
import com.archieapps.exercises.core.format.Format
import com.archieapps.exercises.core.session.SessionExercise
import com.archieapps.exercises.core.session.SessionPhase
import com.archieapps.exercises.ui.component.CircleIconButton
import com.archieapps.exercises.ui.component.GhostButton
import com.archieapps.exercises.ui.component.IronCard
import com.archieapps.exercises.ui.component.MuscleChipsRow
import com.archieapps.exercises.ui.component.PillTag
import com.archieapps.exercises.ui.component.PrimaryButton
import com.archieapps.exercises.ui.component.SectionLabel
import com.archieapps.exercises.ui.component.SetAction
import com.archieapps.exercises.ui.component.SetEditorHeader
import com.archieapps.exercises.ui.component.SetEditorRow
import com.archieapps.exercises.ui.design.IronPositive
import com.archieapps.exercises.ui.design.IronRecord
import com.archieapps.exercises.ui.design.IronText
import com.archieapps.exercises.ui.design.IronTextMuted
import com.archieapps.exercises.ui.design.IronTextSecondary
import com.archieapps.exercises.ui.design.Spacing
import com.archieapps.exercises.core.anatomy.BodyView
import com.archieapps.exercises.ui.component.BodyMap
import com.archieapps.exercises.ui.feature.anatomy.activationRegions

private val FloatingBarClearance = 116.dp

private fun exercisePhaseLabel(state: SessionUiState, index: Int, exercise: SessionExercise): String {
    val phase = state.phase

    return when {
        phase is SessionPhase.Executing && phase.exerciseIndex == index -> "série ${phase.setIndex + 1} em andamento"
        phase is SessionPhase.Resting && phase.exerciseIndex == index -> "em descanso"
        exercise.isComplete -> "concluído"
        else -> "ocioso"
    }
}

@Composable
fun ExerciseLogScreen(
    state: SessionUiState,
    exercise: SessionExercise,
    index: Int,
    actions: SessionActions,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircleIconButton(
                    iconRes = R.drawable.ic_chevron_left,
                    contentDescription = "Voltar para a lista",
                    onClick = actions.onCloseExercise,
                    tint = IronTextSecondary,
                )
                Column(
                    Modifier
                        .weight(1f)
                        .padding(start = Spacing.md)
                ) {
                    SectionLabel(text = exercisePhaseLabel(state, index, exercise))
                    Text(
                        text = exercise.name,
                        style = MaterialTheme.typography.headlineMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = "${exercise.doneSets}/${exercise.sets.size}",
                    style = IronText.metricSmall,
                    color = if (exercise.isComplete) IronPositive else IronTextSecondary,
                )
            }
        }

        item {
            IronCard(Modifier.padding(horizontal = Spacing.lg)) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    exercise.equipment?.let { PillTag(text = it) }

                    if (exercise.tracksDistance) {
                        PillTag(text = Format.distance(exercise.distanceKm))
                        exercise.averageSpeedKmh?.let { PillTag(text = Format.speed(it)) }
                    } else {
                        PillTag(text = Format.volume(exercise.volumeKg))
                    }
                }

                Spacer(Modifier.height(Spacing.sm))

                val muscles = state.muscleMap.of(exercise.exerciseId)

                MuscleChipsRow(
                    muscles = muscles,
                    modifier = Modifier.fillMaxWidth(),
                    fallback = exercise.muscleGroup,
                )

                if (!muscles.isEmpty && state.geometry != null) {
                    Spacer(Modifier.height(Spacing.md))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                    ) {
                        val regions = activationRegions(muscles)

                        BodyView.entries.forEach { view ->
                            BodyMap(
                                geometry = state.geometry,
                                values = regions,
                                view = view,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(160.dp),
                                interactive = false,
                                contentDescription = null,
                            )
                        }
                    }
                }

                exercise.previousSummary?.let { summary ->
                    Spacer(Modifier.height(Spacing.md))
                    Text(
                        text = "Última vez: $summary",
                        style = MaterialTheme.typography.bodyMedium,
                        color = IronTextMuted,
                    )
                }

                exercise.suggestion?.let { suggestion ->
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        text = suggestion,
                        style = MaterialTheme.typography.bodyMedium,
                        color = IronRecord,
                    )
                }

                Spacer(Modifier.height(Spacing.lg))
                SetEditorHeader(exercise = exercise)
                Spacer(Modifier.height(Spacing.sm))

                val weightOptions = state.weightOptionsFor(exercise.exerciseId)

                val canStartSet = state.phase !is SessionPhase.Executing

                exercise.sets.forEachIndexed { setIndex, set ->
                    SetEditorRow(
                        position = setIndex + 1,
                        set = set,
                        exercise = exercise,
                        onRepsChange = { delta -> actions.onChangeReps(index, setIndex, delta) },
                        onWeightStep = { direction -> actions.onChangeWeight(index, setIndex, direction) },
                        onWeightSet = { weight -> actions.onSetWeight(index, setIndex, weight) },
                        onDurationChange = { delta -> actions.onChangeDuration(index, setIndex, delta) },
                        onDistanceStep = { direction -> actions.onChangeDistance(index, setIndex, direction) },
                        onDistanceSet = { distance -> actions.onSetDistance(index, setIndex, distance) },
                        action = SetAction.Live(
                            runState = state.setRunState(index, setIndex),
                            startEnabled = canStartSet,
                            onStart = { actions.onStartSet(index, setIndex) },
                            onFinish = actions.onFinishSet,
                            onUndo = { actions.onUndoSet(index, setIndex) },
                        ),
                        onRemove = { actions.onRemoveSet(index, setIndex) },
                        removable = state.draft?.canRemoveSet(index, setIndex) ?: true,
                        weightOptions = weightOptions,
                        elapsedSeconds = state.elapsedSecondsOf(index, setIndex),
                    )
                    Spacer(Modifier.height(Spacing.sm))
                }

                Spacer(Modifier.height(Spacing.xs))
                GhostButton(
                    text = "+ Série",
                    onClick = { actions.onAddSet(index) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        item {
            Column(
                Modifier.padding(horizontal = Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                PrimaryButton(
                    text = "Voltar para a lista",
                    onClick = actions.onCloseExercise,
                    iconRes = if (exercise.isComplete) R.drawable.ic_check else null,
                    modifier = Modifier.fillMaxWidth(),
                )
                GhostButton(
                    text = "Remover exercício do treino",
                    onClick = { actions.onRemoveExercise(index) },
                    enabled = state.draft?.canRemoveExercise(index) ?: true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        item { Spacer(Modifier.height(FloatingBarClearance)) }
    }
}
