package com.archieapps.exercises.ui.feature.session

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.archieapps.exercises.R
import com.archieapps.exercises.core.format.Format
import com.archieapps.exercises.core.session.ActiveSession
import com.archieapps.exercises.core.session.SessionExercise
import com.archieapps.exercises.core.session.SessionSet
import com.archieapps.exercises.ui.component.CircleIconButton
import com.archieapps.exercises.ui.component.ExercisePickerOverlay
import com.archieapps.exercises.ui.component.SetEditorRow
import com.archieapps.exercises.ui.component.GhostButton
import com.archieapps.exercises.ui.component.IronCard
import com.archieapps.exercises.ui.component.NoticeBanner
import com.archieapps.exercises.ui.component.PillTag
import com.archieapps.exercises.ui.component.PrimaryButton
import com.archieapps.exercises.ui.component.SectionLabel
import com.archieapps.exercises.ui.design.IronBackground
import com.archieapps.exercises.ui.design.IronBorder
import com.archieapps.exercises.ui.design.IronPositive
import com.archieapps.exercises.ui.design.IronPrimary
import com.archieapps.exercises.ui.design.IronRecord
import com.archieapps.exercises.ui.design.IronSurface
import com.archieapps.exercises.ui.design.IronSurfaceHigh
import com.archieapps.exercises.ui.design.IronText
import com.archieapps.exercises.ui.design.IronTextMuted
import com.archieapps.exercises.ui.design.IronTextPrimary
import com.archieapps.exercises.ui.design.IronTextSecondary
import com.archieapps.exercises.ui.design.Spacing

@Composable
fun LiveSessionScreen(
    state: SessionUiState,
    draft: ActiveSession,
    actions: SessionActions,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    var confirmFinish by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }
    var pickingExercise by remember { mutableStateOf(false) }
    val current = draft.current()
    val view = LocalView.current

    DisposableEffect(state.keepScreenOn) {
        view.keepScreenOn = state.keepScreenOn
        onDispose { view.keepScreenOn = false }
    }

    Box(modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            item {
                SessionHeader(
                    draft = draft,
                    nowMillis = state.nowMillis,
                    onDiscard = { confirmDiscard = true },
                )
            }

            if (state.message != null) {
                item {
                    NoticeBanner(
                        message = state.message,
                        isError = true,
                        actionText = "ok",
                        onAction = actions.onDismissMessage,
                        modifier = Modifier.padding(horizontal = Spacing.lg),
                    )
                }
            }

            if (draft.exercises.isNotEmpty()) {
                item {
                    ExerciseChips(
                        draft = draft,
                        onSelect = actions.onSelectExercise,
                        onAdd = { pickingExercise = true },
                    )
                }
            }

            if (current == null) {
                item {
                    IronCard(Modifier.padding(horizontal = Spacing.lg)) {
                        Text(text = "Treino vazio", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(Spacing.sm))
                        Text(
                            text = "Adicione o primeiro exercício para começar a registrar as séries.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = IronTextSecondary,
                        )
                        Spacer(Modifier.height(Spacing.lg))
                        PrimaryButton(
                            text = "Adicionar exercício",
                            onClick = { pickingExercise = true },
                            iconRes = R.drawable.ic_plus,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            } else {
                item {
                    ExerciseCard(
                        exercise = current,
                        index = draft.currentIndex,
                        actions = actions,
                        modifier = Modifier.padding(horizontal = Spacing.lg),
                    )
                }

                item {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.lg),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                    ) {
                        GhostButton(
                            text = "Adicionar exercício",
                            onClick = { pickingExercise = true },
                            modifier = Modifier.weight(1f),
                        )
                        if (draft.currentIndex < draft.exercises.lastIndex) {
                            GhostButton(
                                text = "Próximo",
                                onClick = { actions.onSelectExercise(draft.currentIndex + 1) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            item {
                PrimaryButton(
                    text = "Finalizar treino",
                    onClick = { confirmFinish = true },
                    enabled = draft.hasWork,
                    loading = state.saving,
                    iconRes = R.drawable.ic_check,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg),
                )
            }

            item { Spacer(Modifier.height(Spacing.xxl)) }
        }

        AnimatedVisibility(
            visible = state.rest != null,
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            state.rest?.let { rest ->
                RestBar(
                    rest = rest,
                    onAdd = { actions.onAddRest(15) },
                    onSkip = actions.onSkipRest,
                    modifier = Modifier.padding(Spacing.lg),
                )
            }
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

    if (confirmFinish) {
        AlertDialog(
            onDismissRequest = { confirmFinish = false },
            containerColor = IronSurface,
            titleContentColor = IronTextPrimary,
            textContentColor = IronTextSecondary,
            title = { Text("Finalizar treino?", style = MaterialTheme.typography.titleLarge) },
            text = {
                Text(
                    text = "${draft.completedSets} séries · ${Format.volume(draft.totalVolumeKg)} · " +
                        Format.minutes(draft.durationMinutes(state.nowMillis)),
                    style = MaterialTheme.typography.bodyLarge,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmFinish = false
                    actions.onFinish()
                }) {
                    Text("Salvar", color = IronPrimary, style = MaterialTheme.typography.labelLarge)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmFinish = false }) {
                    Text("Continuar treinando", color = IronTextMuted)
                }
            },
        )
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            containerColor = IronSurface,
            titleContentColor = IronTextPrimary,
            textContentColor = IronTextSecondary,
            title = { Text("Descartar treino?", style = MaterialTheme.typography.titleLarge) },
            text = { Text("As séries registradas até agora serão perdidas.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDiscard = false
                    actions.onDiscard()
                }) {
                    Text("Descartar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDiscard = false }) {
                    Text("Voltar", color = IronTextMuted)
                }
            },
        )
    }
}

@Composable
private fun SessionHeader(draft: ActiveSession, nowMillis: Long, onDiscard: () -> Unit) {
    val elapsed = draft.elapsedSeconds(nowMillis)

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            SectionLabel(text = "em treino")
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = "%d:%02d:%02d".format(elapsed / 3600, (elapsed % 3600) / 60, elapsed % 60),
                style = MaterialTheme.typography.displayMedium,
            )
            Text(
                text = "${draft.name} · ${draft.completedSets} séries · ${Format.volume(draft.totalVolumeKg)}",
                style = MaterialTheme.typography.bodyMedium,
                color = IronTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        CircleIconButton(
            iconRes = R.drawable.ic_chevron_left,
            contentDescription = "Descartar treino",
            onClick = onDiscard,
            tint = IronTextSecondary,
        )
    }
}

@Composable
private fun ExerciseChips(draft: ActiveSession, onSelect: (Int) -> Unit, onAdd: () -> Unit) {
    LazyRow(
        Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = Spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        itemsIndexed(draft.exercises) { index, exercise ->
            val selected = index == draft.currentIndex
            val done = exercise.isComplete

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (selected) IronPrimary.copy(alpha = 0.18f) else IronSurfaceHigh)
                    .border(
                        width = 1.dp,
                        color = if (selected) IronPrimary else Color.Transparent,
                        shape = RoundedCornerShape(999.dp),
                    )
                    .clickable { onSelect(index) }
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (done) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = null,
                        tint = IronPositive,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(Spacing.xs))
                }
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) IronTextPrimary else IronTextSecondary,
                    maxLines = 1,
                )
            }
        }

        item {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(IronSurfaceHigh)
                    .clickable(onClick = onAdd),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_plus),
                    contentDescription = "Adicionar exercício",
                    tint = IronTextSecondary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun ExerciseCard(
    exercise: SessionExercise,
    index: Int,
    actions: SessionActions,
    modifier: Modifier = Modifier,
) {
    IronCard(modifier) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(Spacing.xs))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    exercise.muscleGroup?.let { PillTag(text = it) }
                    exercise.equipment?.let { PillTag(text = it) }
                }
            }
            Text(
                text = Format.volume(exercise.volumeKg),
                style = IronText.metricSmall,
                color = IronTextSecondary,
            )
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

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "SÉRIE",
                style = MaterialTheme.typography.labelSmall,
                color = IronTextMuted,
                modifier = Modifier.width(48.dp),
            )
            Text(
                text = if (exercise.executionType == "time") "TEMPO" else "REPS",
                style = MaterialTheme.typography.labelSmall,
                color = IronTextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            if (exercise.usesWeight) {
                Text(
                    text = "CARGA",
                    style = MaterialTheme.typography.labelSmall,
                    color = IronTextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.width(52.dp))
        }

        Spacer(Modifier.height(Spacing.sm))

        exercise.sets.forEachIndexed { setIndex, set ->
            SetEditorRow(
                position = setIndex + 1,
                set = set,
                exercise = exercise,
                onRepsChange = { delta -> actions.onChangeReps(index, setIndex, delta) },
                onWeightChange = { delta -> actions.onChangeWeight(index, setIndex, delta) },
                onDurationChange = { delta -> actions.onChangeDuration(index, setIndex, delta) },
                onToggle = { actions.onToggleSet(index, setIndex) },
                onRemove = { actions.onRemoveSet(index, setIndex) },
            )
            Spacer(Modifier.height(Spacing.sm))
        }

        Spacer(Modifier.height(Spacing.sm))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            GhostButton(
                text = "+ Série",
                onClick = { actions.onAddSet(index) },
                modifier = Modifier.weight(1f),
            )
            GhostButton(
                text = "Remover exercício",
                onClick = { actions.onRemoveExercise(index) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun RestBar(
    rest: RestState,
    onAdd: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress by animateFloatAsState(rest.progress, label = "restProgress")

    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(IronSurfaceHigh)
            .border(1.dp, IronPrimary.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .padding(Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(46.dp), contentAlignment = Alignment.Center) {
            androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                val stroke = 4.dp.toPx()
                drawArc(
                    color = IronBorder,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
                drawArc(
                    color = IronPrimary,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
            Text(
                text = "%d:%02d".format(rest.remainingSeconds / 60, rest.remainingSeconds % 60),
                style = MaterialTheme.typography.labelMedium,
                color = IronTextPrimary,
            )
        }

        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = Spacing.md)
        ) {
            Text(text = "Descanso", style = MaterialTheme.typography.titleMedium)
            Text(
                text = rest.exerciseName,
                style = MaterialTheme.typography.labelMedium,
                color = IronTextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        GhostButton(text = "+15s", onClick = onAdd)
        Spacer(Modifier.width(Spacing.sm))
        GhostButton(text = "Pular", onClick = onSkip)
    }
}

