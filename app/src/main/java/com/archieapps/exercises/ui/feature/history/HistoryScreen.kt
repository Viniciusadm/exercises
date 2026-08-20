package com.archieapps.exercises.ui.feature.history

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.archieapps.exercises.core.format.Format
import com.archieapps.exercises.core.net.dto.WorkoutDto
import com.archieapps.exercises.ui.component.EmptyState
import com.archieapps.exercises.ui.component.GhostButton
import com.archieapps.exercises.ui.component.HairLine
import com.archieapps.exercises.ui.component.IronCard
import com.archieapps.exercises.ui.component.KeyValueRow
import com.archieapps.exercises.ui.component.LoadingSkeleton
import com.archieapps.exercises.ui.component.NoticeBanner
import com.archieapps.exercises.ui.component.PillTag
import com.archieapps.exercises.ui.component.SectionLabel
import com.archieapps.exercises.ui.design.IronSurface
import com.archieapps.exercises.ui.design.IronText
import com.archieapps.exercises.ui.design.IronTextMuted
import com.archieapps.exercises.ui.design.IronTextPrimary
import com.archieapps.exercises.ui.design.IronTextSecondary
import com.archieapps.exercises.ui.design.Spacing
import java.time.YearMonth

@Composable
fun HistoryScreen(
    state: HistoryUiState,
    onToggle: (Int) -> Unit,
    onLoadMore: () -> Unit,
    onCreateTemplate: (WorkoutDto) -> Unit,
    onEdit: (WorkoutDto) -> Unit,
    onDelete: (WorkoutDto) -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val listState = rememberLazyListState()
    val shouldLoadMore by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0

            last >= listState.layoutInfo.totalItemsCount - 3
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { shouldLoadMore }.collect { if (it) onLoadMore() }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        item {
            Column(Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md)) {
                SectionLabel(text = "histórico")
                Text(text = "Treinos", style = MaterialTheme.typography.headlineMedium)
            }
        }

        (state.message ?: state.error)?.let { notice ->
            item {
                NoticeBanner(
                    message = notice,
                    isError = state.error != null,
                    actionText = "ok",
                    onAction = onDismissMessage,
                    modifier = Modifier.padding(horizontal = Spacing.lg),
                )
            }
        }

        if (state.workouts.isEmpty()) {
            item {
                if (state.loading) {
                    LoadingSkeleton(Modifier.padding(horizontal = Spacing.lg))
                } else {
                    EmptyState(
                        title = "Sem treinos por aqui",
                        message = "Quando você registrar uma sessão, ela aparece nesta lista.",
                    )
                }
            }

            return@LazyColumn
        }

        var lastMonth: YearMonth? = null

        state.workouts.forEach { workout ->
            val month = Format.parseDate(workout.date)?.let { YearMonth.from(it) }

            if (month != null && month != lastMonth) {
                lastMonth = month

                item(key = "month-${month}") {
                    MonthDivider(month, state.workouts, Modifier.padding(horizontal = Spacing.lg))
                }
            }

            item(key = workout.id) {
                WorkoutCard(
                    workout = workout,
                    expanded = state.expandedId == workout.id,
                    onToggle = { onToggle(workout.id) },
                    onCreateTemplate = { onCreateTemplate(workout) },
                    onEdit = { onEdit(workout) },
                    onDelete = { onDelete(workout) },
                    modifier = Modifier.padding(horizontal = Spacing.lg),
                )
            }
        }

        if (state.loadingMore) {
            item {
                Text(
                    text = "Carregando mais…",
                    style = MaterialTheme.typography.labelMedium,
                    color = IronTextMuted,
                    modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md),
                )
            }
        }

        item { Spacer(Modifier.height(Spacing.xl)) }
    }
}

@Composable
private fun MonthDivider(month: YearMonth, workouts: List<WorkoutDto>, modifier: Modifier = Modifier) {
    val ofMonth = workouts.filter { Format.parseDate(it.date)?.let { date -> YearMonth.from(date) } == month }
    val volume = ofMonth.sumOf { it.totalVolumeKg }

    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "${Format.monthShort(month)} ${month.year}".uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = IronTextSecondary,
        )
        Text(
            text = "${ofMonth.size} treinos · ${Format.volume(volume)}",
            style = MaterialTheme.typography.labelMedium,
            color = IronTextMuted,
        )
    }
}

@Composable
private fun WorkoutCard(
    workout: WorkoutDto,
    expanded: Boolean,
    onToggle: () -> Unit,
    onCreateTemplate: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmDelete by remember { mutableStateOf(false) }

    IronCard(modifier = modifier, onClick = onToggle) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = Format.parseDate(workout.date)?.let(Format::dateWithWeekday) ?: workout.date,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "${workout.exerciseCount} exercícios · ${workout.setsCount} séries · " +
                        Format.minutes(workout.durationMinutes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = IronTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(text = Format.volume(workout.totalVolumeKg), style = IronText.metricSmall)
        }

        if (workout.muscleGroups.isNotEmpty()) {
            Spacer(Modifier.height(Spacing.sm))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                workout.muscleGroups.take(4).forEach { group -> PillTag(text = group) }
            }
        }

        if (confirmDelete) {
        DeleteWorkoutDialog(
            workout = workout,
            onConfirm = {
                confirmDelete = false
                onDelete()
            },
            onDismiss = { confirmDelete = false },
        )
    }

    AnimatedVisibility(visible = expanded) {
            Column(Modifier.fillMaxWidth()) {
                Spacer(Modifier.height(Spacing.md))
                HairLine()
                Spacer(Modifier.height(Spacing.sm))

                workout.exercises.forEach { exercise ->
                    KeyValueRow(
                        label = exercise.name,
                        value = exercise.sets.joinToString("  ") { set ->
                            listOfNotNull(
                                set.repetitions?.let { "$it" },
                                set.weightKg?.let { Format.weightValue(it) },
                            ).joinToString("×")
                        },
                    )
                }

                Spacer(Modifier.height(Spacing.md))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    GhostButton(text = "Editar", onClick = onEdit, modifier = Modifier.weight(1f))
                    GhostButton(
                        text = "Excluir",
                        onClick = { confirmDelete = true },
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(Spacing.sm))
                GhostButton(
                    text = "Criar template com este treino",
                    onClick = onCreateTemplate,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun DeleteWorkoutDialog(
    workout: WorkoutDto,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = IronSurface,
        titleContentColor = IronTextPrimary,
        textContentColor = IronTextSecondary,
        title = { Text("Excluir treino?", style = MaterialTheme.typography.titleLarge) },
        text = {
            Text(
                "A sessão de " +
                    (Format.parseDate(workout.date)?.let(Format::dateWithWeekday) ?: workout.date) +
                    " sai do histórico e das estatísticas."
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Excluir", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Manter", color = IronTextMuted) }
        },
    )
}
