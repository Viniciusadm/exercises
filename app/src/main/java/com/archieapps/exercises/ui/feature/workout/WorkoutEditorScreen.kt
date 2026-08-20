package com.archieapps.exercises.ui.feature.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.archieapps.exercises.R
import com.archieapps.exercises.core.format.Format
import com.archieapps.exercises.core.session.SessionExercise
import com.archieapps.exercises.ui.component.CardHeader
import com.archieapps.exercises.ui.component.CircleIconButton
import com.archieapps.exercises.ui.component.ExercisePickerOverlay
import com.archieapps.exercises.ui.component.GhostButton
import com.archieapps.exercises.ui.component.IronCard
import com.archieapps.exercises.ui.component.LoadingSkeleton
import com.archieapps.exercises.ui.component.NoticeBanner
import com.archieapps.exercises.ui.component.PillTag
import com.archieapps.exercises.ui.component.PrimaryButton
import com.archieapps.exercises.ui.component.SectionLabel
import com.archieapps.exercises.ui.component.SetEditorHeader
import com.archieapps.exercises.ui.component.SetEditorRow
import com.archieapps.exercises.ui.design.IronPrimary
import com.archieapps.exercises.ui.design.IronSurface
import com.archieapps.exercises.ui.design.IronText
import com.archieapps.exercises.ui.design.IronTextMuted
import com.archieapps.exercises.ui.design.IronTextPrimary
import com.archieapps.exercises.ui.design.IronTextSecondary
import com.archieapps.exercises.ui.design.Spacing
import com.archieapps.exercises.ui.feature.auth.IronTextField

data class WorkoutEditorActions(
    val onNameChange: (String) -> Unit,
    val onNotesChange: (String) -> Unit,
    val onShiftDate: (Long) -> Unit,
    val onChangeDuration: (Int) -> Unit,
    val onAddExercise: (com.archieapps.exercises.core.db.ExerciseEntity) -> Unit,
    val onRemoveExercise: (Int) -> Unit,
    val onAddSet: (Int) -> Unit,
    val onRemoveSet: (Int, Int) -> Unit,
    val onChangeReps: (Int, Int, Int) -> Unit,
    val onChangeWeight: (Int, Int, Double) -> Unit,
    val onChangeDurationSeconds: (Int, Int, Int) -> Unit,
    val onToggleSet: (Int, Int) -> Unit,
    val onSave: () -> Unit,
    val onDelete: () -> Unit,
    val onDismissMessage: () -> Unit,
)

@Composable
fun WorkoutEditorScreen(
    state: WorkoutEditorUiState,
    actions: WorkoutEditorActions,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    var picking by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    Box(modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
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
                    Column(Modifier.weight(1f)) {
                        SectionLabel(text = "editar treino")
                        Text(
                            text = Format.dateWithWeekday(state.date),
                            style = MaterialTheme.typography.headlineMedium,
                        )
                        Text(
                            text = "${state.countedSets} séries · ${Format.volume(state.totalVolumeKg)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = IronTextSecondary,
                        )
                    }
                    CircleIconButton(
                        iconRes = R.drawable.ic_chevron_left,
                        contentDescription = "Voltar",
                        onClick = onClose,
                        tint = IronTextSecondary,
                    )
                }
            }

            state.message?.let { message ->
                item {
                    NoticeBanner(
                        message = message,
                        isError = state.isError,
                        actionText = "ok",
                        onAction = actions.onDismissMessage,
                        modifier = Modifier.padding(horizontal = Spacing.lg),
                    )
                }
            }

            if (state.loading) {
                item { LoadingSkeleton(Modifier.padding(horizontal = Spacing.lg)) }

                return@LazyColumn
            }

            item {
                IronCard(Modifier.padding(horizontal = Spacing.lg)) {
                    CardHeader(title = "Resumo", caption = "nome, data e duração da sessão")
                    Spacer(Modifier.height(Spacing.md))
                    IronTextField(value = state.name, onValueChange = actions.onNameChange, label = "Nome")
                    Spacer(Modifier.height(Spacing.md))
                    ValueRow(
                        label = "Data",
                        value = Format.dayMonth(state.date),
                        onDecrease = { actions.onShiftDate(-1) },
                        onIncrease = { actions.onShiftDate(1) },
                    )
                    ValueRow(
                        label = "Duração",
                        value = Format.minutes(state.durationMinutes),
                        onDecrease = { actions.onChangeDuration(-5) },
                        onIncrease = { actions.onChangeDuration(5) },
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    IronTextField(
                        value = state.notes,
                        onValueChange = actions.onNotesChange,
                        label = "Observações",
                    )
                }
            }

            itemsIndexed(state.exercises, key = { index, item -> "${item.exerciseId ?: item.name}-$index" }) { index, exercise ->
                ExerciseEditorCard(
                    exercise = exercise,
                    index = index,
                    actions = actions,
                    modifier = Modifier.padding(horizontal = Spacing.lg),
                )
            }

            item {
                GhostButton(
                    text = "Adicionar exercício",
                    onClick = { picking = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg),
                )
            }

            item {
                Column(
                    Modifier.padding(horizontal = Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    PrimaryButton(
                        text = "Salvar treino",
                        onClick = actions.onSave,
                        enabled = state.canSave,
                        loading = state.saving,
                        iconRes = R.drawable.ic_check,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    GhostButton(
                        text = "Excluir treino",
                        onClick = { confirmDelete = true },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

        }

        if (picking) {
            ExercisePickerOverlay(
                catalog = state.catalog,
                onPick = {
                    actions.onAddExercise(it)
                    picking = false
                },
                onClose = { picking = false },
            )
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = IronSurface,
            titleContentColor = IronTextPrimary,
            textContentColor = IronTextSecondary,
            title = { Text("Excluir treino?", style = MaterialTheme.typography.titleLarge) },
            text = { Text("A sessão de ${Format.dateWithWeekday(state.date)} sai do histórico e das estatísticas.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    actions.onDelete()
                }) {
                    Text("Excluir", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text("Manter", color = IronTextMuted)
                }
            },
        )
    }
}

@Composable
private fun ExerciseEditorCard(
    exercise: SessionExercise,
    index: Int,
    actions: WorkoutEditorActions,
    modifier: Modifier = Modifier,
) {
    IronCard(modifier) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(Spacing.xs))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    exercise.muscleGroup?.let { PillTag(text = it) }
                }
            }
            Text(
                text = Format.volume(exercise.volumeKg),
                style = IronText.metricSmall,
                color = IronTextSecondary,
            )
        }

        Spacer(Modifier.height(Spacing.md))

        SetEditorHeader(exercise = exercise)
        Spacer(Modifier.height(Spacing.sm))

        exercise.sets.forEachIndexed { setIndex, set ->
            SetEditorRow(
                position = setIndex + 1,
                set = set,
                exercise = exercise,
                onRepsChange = { delta -> actions.onChangeReps(index, setIndex, delta) },
                onWeightChange = { delta -> actions.onChangeWeight(index, setIndex, delta) },
                onDurationChange = { delta -> actions.onChangeDurationSeconds(index, setIndex, delta) },
                onToggle = { actions.onToggleSet(index, setIndex) },
                onRemove = { actions.onRemoveSet(index, setIndex) },
            )
            Spacer(Modifier.height(Spacing.sm))
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            GhostButton(
                text = "+ Série",
                onClick = { actions.onAddSet(index) },
                modifier = Modifier.weight(1f),
            )
            GhostButton(
                text = "Remover",
                onClick = { actions.onRemoveExercise(index) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ValueRow(
    label: String,
    value: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = IronTextSecondary,
            modifier = Modifier.weight(1f),
        )
        GhostButton(text = "−", onClick = onDecrease)
        Text(
            text = value,
            style = IronText.metricSmall,
            color = IronPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(96.dp),
        )
        GhostButton(text = "+", onClick = onIncrease)
    }
}
