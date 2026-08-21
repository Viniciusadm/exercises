package com.archieapps.exercises.ui.feature.session

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.archieapps.exercises.core.format.Format
import com.archieapps.exercises.core.session.ActiveSession
import com.archieapps.exercises.core.session.SessionBuilder
import com.archieapps.exercises.core.session.SessionExercise
import com.archieapps.exercises.ui.component.CardHeader
import com.archieapps.exercises.ui.component.ExercisePickerOverlay
import com.archieapps.exercises.ui.component.GhostButton
import com.archieapps.exercises.ui.component.HairLine
import com.archieapps.exercises.ui.component.HeroSurface
import com.archieapps.exercises.ui.component.IronCard
import com.archieapps.exercises.ui.component.KeyValueRow
import com.archieapps.exercises.ui.component.LoadingSkeleton
import com.archieapps.exercises.ui.component.MuscleChipsRow
import com.archieapps.exercises.ui.component.PillTag
import com.archieapps.exercises.ui.component.PrimaryButton
import com.archieapps.exercises.ui.component.SectionLabel
import com.archieapps.exercises.ui.component.StatTile
import com.archieapps.exercises.ui.design.IronBackground
import com.archieapps.exercises.ui.design.IronRecord
import com.archieapps.exercises.ui.design.IronTextMuted
import com.archieapps.exercises.ui.design.IronTextSecondary
import com.archieapps.exercises.ui.design.Spacing

@Composable
fun SessionPreviewScreen(
    state: SessionUiState,
    pending: ActiveSession,
    actions: PreviewActions,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    var pickingExercise by remember { mutableStateOf(false) }

    BackHandler(onBack = actions.onCancel)

    val plannedSets = SessionBuilder.plannedSets(pending.exercises)
    val estimatedSeconds = SessionBuilder.estimatedSeconds(pending.exercises, state.defaultRestSeconds)
    val plannedVolume = SessionBuilder.plannedVolumeKg(pending.exercises)

    Box(modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            item {
                Column(Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md)) {
                    HeroSurface {
                        SectionLabel(text = "pronto para começar")
                        Spacer(Modifier.height(Spacing.sm))
                        Text(
                            text = pending.name,
                            style = MaterialTheme.typography.headlineMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(Spacing.lg))

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        ) {
                            StatTile(
                                label = "exercícios",
                                value = Format.integer(pending.exercises.size),
                                modifier = Modifier.weight(1f),
                            )
                            StatTile(
                                label = "séries",
                                value = Format.integer(plannedSets),
                                modifier = Modifier.weight(1f),
                            )
                        }

                        Spacer(Modifier.height(Spacing.sm))

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        ) {
                            StatTile(
                                label = "duração estimada",
                                value = Format.duration(estimatedSeconds),
                                modifier = Modifier.weight(1f),
                            )
                            StatTile(
                                label = "volume estimado",
                                value = Format.volume(plannedVolume),
                                accent = IronRecord,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            if (state.preparing) {
                item {
                    LoadingSkeleton(modifier = Modifier.padding(horizontal = Spacing.lg))
                }
            }

            if (pending.exercises.isEmpty()) {
                item {
                    Text(
                        text = "Nenhum exercício neste treino ainda. Adicione antes de começar.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = IronTextMuted,
                        modifier = Modifier.padding(horizontal = Spacing.lg),
                    )
                }
            } else {
                item {
                    SectionLabel(
                        text = "exercícios do treino",
                        modifier = Modifier.padding(horizontal = Spacing.lg),
                    )
                }

                itemsIndexed(
                    pending.exercises,
                    key = { index, item -> "${item.exerciseId ?: item.name}-$index" },
                ) { index, exercise ->
                    PreviewExerciseCard(
                        state = state,
                        exercise = exercise,
                        position = index + 1,
                        defaultRestSeconds = state.defaultRestSeconds,
                        onRemove = { actions.onRemoveExercise(index) },
                        modifier = Modifier.padding(horizontal = Spacing.lg),
                    )
                }
            }

            item {
                GhostButton(
                    text = "Adicionar exercício",
                    onClick = { pickingExercise = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg),
                )
            }

            item { Spacer(Modifier.height(132.dp)) }
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(IronBackground)
                .navigationBarsPadding()
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            PrimaryButton(
                text = "Iniciar treino",
                onClick = actions.onStart,
                enabled = pending.exercises.isNotEmpty() && !state.preparing,
                modifier = Modifier.fillMaxWidth(),
            )
            GhostButton(
                text = "Voltar",
                onClick = actions.onCancel,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (pickingExercise) {
            ExercisePickerOverlay(
                catalog = state.catalog,
                onPick = {
                    actions.onAddExercise(it)
                    pickingExercise = false
                },
                onClose = { pickingExercise = false },
            )
        }
    }
}

@Composable
private fun PreviewExerciseCard(
    state: SessionUiState,
    exercise: SessionExercise,
    position: Int,
    defaultRestSeconds: Int,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val restSeconds = exercise.restSeconds ?: defaultRestSeconds
    val targetReps = exercise.sets.firstOrNull()?.repetitions ?: exercise.targetReps
    val targetWeight = exercise.sets.firstOrNull()?.weightKg
    val plan = if (exercise.executionType == "time") {
        "${exercise.sets.size} × ${Format.duration(exercise.sets.firstOrNull()?.durationSeconds)}"
    } else {
        listOfNotNull(
            "${exercise.sets.size} × ${targetReps ?: "-"}",
            targetWeight?.let(Format::weight),
        ).joinToString(" · ")
    }

    IronCard(modifier) {
        CardHeader(
            title = "$position. ${exercise.name}",
            caption = plan,
            trailing = { PillTag(text = "descanso ${Format.duration(restSeconds)}") },
        )

        Spacer(Modifier.height(Spacing.sm))

        MuscleChipsRow(
            muscles = state.muscleMap.of(exercise.exerciseId),
            modifier = Modifier.fillMaxWidth(),
            fallback = exercise.muscleGroup,
        )

        exercise.equipment?.let { equipment ->
            Spacer(Modifier.height(Spacing.sm))
            KeyValueRow(label = "Equipamento", value = equipment)
        }

        exercise.previousSummary?.let { summary ->
            KeyValueRow(label = "Última vez", value = summary)
        }

        exercise.suggestion?.let { suggestion ->
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = suggestion,
                style = MaterialTheme.typography.bodyMedium,
                color = IronRecord,
            )
        }

        exercise.bestWeightKg?.let { best ->
            KeyValueRow(label = "Melhor carga", value = Format.weight(best))
        }

        exercise.notes?.takeIf { it.isNotBlank() }?.let { notes ->
            Spacer(Modifier.height(Spacing.sm))
            HairLine()
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = notes,
                style = MaterialTheme.typography.bodyMedium,
                color = IronTextSecondary,
            )
        }

        Spacer(Modifier.height(Spacing.sm))
        GhostButton(
            text = "Remover do treino",
            onClick = onRemove,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
