package com.archieapps.exercises.ui.feature.workout

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.archieapps.exercises.R
import com.archieapps.exercises.core.format.Format
import com.archieapps.exercises.core.net.dto.WorkoutExerciseDto
import com.archieapps.exercises.core.session.ExecutionType
import com.archieapps.exercises.ui.component.CardHeader
import com.archieapps.exercises.ui.component.CircleIconButton
import com.archieapps.exercises.ui.component.GhostButton
import com.archieapps.exercises.ui.component.IronBottomSheet
import com.archieapps.exercises.ui.component.IronCard
import com.archieapps.exercises.ui.component.KeyValueRow
import com.archieapps.exercises.ui.component.LoadingSkeleton
import com.archieapps.exercises.ui.component.NoticeBanner
import com.archieapps.exercises.ui.component.PillTag
import com.archieapps.exercises.ui.component.PrimaryButton
import com.archieapps.exercises.ui.component.SectionLabel
import com.archieapps.exercises.ui.component.SessionTimeCard
import com.archieapps.exercises.ui.component.SessionTimeMetrics
import com.archieapps.exercises.ui.component.SessionTimelineList
import com.archieapps.exercises.ui.component.TimelineEntry
import com.archieapps.exercises.ui.design.IronTextMuted
import com.archieapps.exercises.ui.design.IronTextSecondary
import com.archieapps.exercises.ui.design.Spacing
import com.archieapps.exercises.ui.feature.auth.IronTextField

@Composable
fun WorkoutDetailScreen(
    state: WorkoutDetailUiState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onAdjustSegment: (Int, Int) -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    var editing by remember { mutableStateOf<TimelineEntry?>(null) }
    val workout = state.workout
    val entries = state.timeline?.segments.orEmpty().map(TimelineEntry::of)

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
                    contentDescription = "Voltar",
                    onClick = onBack,
                    tint = IronTextSecondary,
                )
                Column(
                    Modifier
                        .weight(1f)
                        .padding(start = Spacing.md)
                ) {
                    SectionLabel(
                        text = Format.parseDate(workout?.date)?.let(Format::dateWithWeekday) ?: "treino",
                    )
                    Text(
                        text = workout?.name ?: "…",
                        style = MaterialTheme.typography.headlineMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        if (state.message != null) {
            item {
                NoticeBanner(
                    message = state.message,
                    isError = state.isError,
                    actionText = "ok",
                    onAction = onDismissMessage,
                    modifier = Modifier.padding(horizontal = Spacing.lg),
                )
            }
        }

        if (state.loading && workout == null) {
            item { LoadingSkeleton(Modifier.padding(horizontal = Spacing.lg)) }
        }

        if (workout != null) {
            item {
                SessionTimeCard(
                    metrics = workout.time?.let(SessionTimeMetrics::of),
                    fallbackTotalSeconds = workout.durationMinutes.toLong() * 60,
                    modifier = Modifier.padding(horizontal = Spacing.lg),
                )
            }

            item {
                IronCard(Modifier.padding(horizontal = Spacing.lg)) {
                    CardHeader(title = "Volume", caption = workout.workoutType)
                    Spacer(Modifier.height(Spacing.sm))
                    KeyValueRow(label = "Exercícios", value = Format.integer(workout.exerciseCount))
                    KeyValueRow(label = "Séries registradas", value = Format.integer(workout.setsCount))
                    KeyValueRow(label = "Repetições", value = Format.integer(workout.totalReps))
                    KeyValueRow(label = "Volume total", value = Format.volume(workout.totalVolumeKg))
                    workout.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                        Spacer(Modifier.height(Spacing.sm))
                        Text(
                            text = notes,
                            style = MaterialTheme.typography.bodyMedium,
                            color = IronTextSecondary,
                        )
                    }
                }
            }

            if (state.hasSegregatedMetrics) {
                item {
                    SessionTimelineList(
                        entries = entries,
                        startLabel = Format.timeOfDay(state.timeline?.startedAt ?: workout.startedAt),
                        endLabel = Format.timeOfDay(state.timeline?.finishedAt ?: workout.finishedAt),
                        onEditSegment = { entry -> editing = entry },
                        modifier = Modifier.padding(horizontal = Spacing.lg),
                    )
                }
            }

            item {
                SectionLabel(
                    text = "exercícios",
                    modifier = Modifier.padding(horizontal = Spacing.lg),
                )
            }

            items(workout.exercises, key = { it.id ?: it.order }) { exercise ->
                ExerciseSummaryCard(exercise, Modifier.padding(horizontal = Spacing.lg))
            }

            item {
                GhostButton(
                    text = "Editar treino",
                    onClick = onEdit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg),
                )
            }
        }

        item { Spacer(Modifier.height(Spacing.xl)) }
    }

    editing?.let { entry ->
        SegmentDurationSheet(
            entry = entry,
            saving = state.adjusting,
            onDismiss = { editing = null },
            onConfirm = { seconds ->
                onAdjustSegment(entry.sequence, seconds)
                editing = null
            },
        )
    }
}

@Composable
private fun ExerciseSummaryCard(exercise: WorkoutExerciseDto, modifier: Modifier = Modifier) {
    IronCard(modifier) {
        CardHeader(
            title = exercise.name,
            caption = listOfNotNull(
                exercise.muscleGroup,
                "${exercise.sets.size} séries",
                exercise.avgSpeedKmh?.let { Format.speed(it) },
            ).joinToString(" · "),
            trailing = {
                PillTag(
                    text = if (exercise.tracksDistance) {
                        Format.distance(exercise.totalDistanceKm)
                    } else {
                        Format.volume(exercise.totalVolumeKg)
                    },
                )
            },
        )

        Spacer(Modifier.height(Spacing.sm))

        exercise.sets.forEach { set ->
            val detail = if (exercise.executionType == ExecutionType.TIME) {
                listOfNotNull(
                    Format.duration(set.durationSeconds),
                    set.distanceKm?.takeIf { it > 0 }?.let(Format::distance),
                    set.speedKmh?.let(Format::speed),
                ).joinToString(" · ")
            } else {
                listOfNotNull(
                    set.repetitions?.let { "$it reps" },
                    set.weightKg?.let(Format::weight),
                ).joinToString(" · ").ifBlank { Format.PLACEHOLDER }
            }

            KeyValueRow(label = "Série ${set.setNumber}", value = detail)
        }
    }
}

@Composable
private fun SegmentDurationSheet(
    entry: TimelineEntry,
    saving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var minutes by remember { mutableStateOf((entry.durationSeconds / 60).toString()) }
    var seconds by remember { mutableStateOf((entry.durationSeconds % 60).toString()) }
    val parsed = (minutes.toIntOrNull() ?: 0) * 60 + (seconds.toIntOrNull() ?: 0)

    IronBottomSheet(
        onDismissRequest = onDismiss,
        heightFraction = 0.55f,
        onBack = onDismiss,
        header = {
            SectionLabel(text = "ajustar duração")
            Spacer(Modifier.height(Spacing.xs))
            Text(text = entry.label, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(Spacing.sm))
        },
        footer = {
            PrimaryButton(
                text = "Salvar duração",
                onClick = { onConfirm(parsed) },
                enabled = parsed >= 0 && parsed <= 86400 && !saving,
                loading = saving,
                modifier = Modifier.fillMaxWidth(),
            )
            GhostButton(text = "Cancelar", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
        },
    ) {
        Text(
            text = "Registrado: ${Format.duration(entry.durationSeconds)} às ${Format.timeOfDay(entry.atEpochMillis)}.",
            style = MaterialTheme.typography.bodyMedium,
            color = IronTextSecondary,
        )
        Text(
            text = "A diferença entre a duração antiga e a nova sai (ou volta) do tempo ocioso. " +
                "O tempo total do treino não muda.",
            style = MaterialTheme.typography.bodyMedium,
            color = IronTextMuted,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            IronTextField(
                value = minutes,
                onValueChange = { minutes = it.filter(Char::isDigit).take(4) },
                label = "minutos",
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f),
            )
            IronTextField(
                value = seconds,
                onValueChange = { seconds = it.filter(Char::isDigit).take(2) },
                label = "segundos",
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f),
            )
        }

        Text(
            text = "Nova duração: ${Format.duration(parsed.toLong())}",
            style = MaterialTheme.typography.titleMedium,
        )
    }
}
