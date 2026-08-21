package com.archieapps.exercises.ui.feature.session

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.archieapps.exercises.R
import com.archieapps.exercises.core.format.Format
import com.archieapps.exercises.core.session.ActiveSession
import com.archieapps.exercises.core.session.SessionExercise
import com.archieapps.exercises.core.session.SessionPhase
import com.archieapps.exercises.ui.component.CircleIconButton
import com.archieapps.exercises.ui.component.ExecutionColor
import com.archieapps.exercises.ui.component.IdleColor
import com.archieapps.exercises.ui.component.RestColor
import com.archieapps.exercises.ui.component.GhostButton
import com.archieapps.exercises.ui.component.NoticeBanner
import com.archieapps.exercises.ui.component.PrimaryButton
import com.archieapps.exercises.ui.component.SectionLabel
import com.archieapps.exercises.ui.design.IronBackground
import com.archieapps.exercises.ui.design.IronBorder
import com.archieapps.exercises.ui.design.IronPositive
import com.archieapps.exercises.ui.design.IronPrimary
import com.archieapps.exercises.ui.design.IronRecord
import com.archieapps.exercises.ui.design.IronSurface
import com.archieapps.exercises.ui.design.IronText
import com.archieapps.exercises.ui.design.IronTextMuted
import com.archieapps.exercises.ui.design.IronTextPrimary
import com.archieapps.exercises.ui.design.IronTextSecondary
import com.archieapps.exercises.ui.design.Spacing

private val FloatingBarClearance = 116.dp

@Composable
fun SessionExerciseListScreen(
    state: SessionUiState,
    draft: ActiveSession,
    onOpenExercise: (Int) -> Unit,
    onStartNextSet: (Int) -> Unit,
    onFinishSet: () -> Unit,
    onAddExercise: () -> Unit,
    onFinish: () -> Unit,
    onDiscard: () -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        item {
            SessionProgressHeader(
                state = state,
                draft = draft,
                onDiscard = onDiscard,
            )
        }

        if (state.message != null) {
            item {
                NoticeBanner(
                    message = state.message,
                    isError = true,
                    actionText = "ok",
                    onAction = onDismissMessage,
                    modifier = Modifier.padding(horizontal = Spacing.lg),
                )
            }
        }

        if (draft.exercises.isEmpty()) {
            item {
                Column(Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md)) {
                    Text(text = "Treino vazio", style = MaterialTheme.typography.titleLarge)
                }
            }
        } else {
            item {
                SectionLabel(
                    text = "escolha o exercício",
                    modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                )
            }

            itemsIndexed(draft.exercises, key = { index, item -> "${item.exerciseId ?: item.name}-$index" }) { index, exercise ->
                ExerciseChoiceRow(
                    exercise = exercise,
                    quickAction = quickActionFor(state, draft, index),
                    onClick = { onOpenExercise(index) },
                    onQuickAction = {
                        when (quickActionFor(state, draft, index)) {
                            QuickAction.Start -> onStartNextSet(index)
                            QuickAction.Finish -> onFinishSet()
                            else -> Unit
                        }
                    },
                    modifier = Modifier.padding(horizontal = Spacing.lg),
                )
            }
        }

        item {
            GhostButton(
                text = "Adicionar exercício",
                onClick = onAddExercise,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
            )
        }

        item {
            PrimaryButton(
                text = "Finalizar treino",
                onClick = onFinish,
                enabled = draft.hasWork,
                loading = state.saving,
                iconRes = R.drawable.ic_check,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg),
            )
        }

        item { Spacer(Modifier.height(FloatingBarClearance)) }
    }
}

enum class QuickAction { Start, Finish, Resting, Blocked, Done }

private fun quickActionFor(state: SessionUiState, draft: ActiveSession, index: Int): QuickAction {
    val phase = state.phase

    return when {
        phase is SessionPhase.Executing && phase.exerciseIndex == index -> QuickAction.Finish
        phase is SessionPhase.Executing -> QuickAction.Blocked
        phase is SessionPhase.Resting && phase.exerciseIndex == index -> QuickAction.Resting
        draft.nextPendingSet(index) == null -> QuickAction.Done
        else -> QuickAction.Start
    }
}

@Composable
private fun SessionProgressHeader(state: SessionUiState, draft: ActiveSession, onDiscard: () -> Unit) {
    val breakdown = state.breakdown
    val phaseLabel = when (state.phase) {
        is SessionPhase.Executing -> "em série"
        is SessionPhase.Resting -> "em descanso"
        SessionPhase.Idle -> "ocioso"
    }
    val phaseColor = when (state.phase) {
        is SessionPhase.Executing -> ExecutionColor
        is SessionPhase.Resting -> RestColor
        SessionPhase.Idle -> IdleColor
    }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.md)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(phaseColor)
                    )
                    Spacer(Modifier.width(Spacing.xs))
                    SectionLabel(text = phaseLabel)
                }
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = Format.clock(breakdown.totalSeconds),
                    style = MaterialTheme.typography.displayMedium,
                )
                Text(
                    text = "${draft.completedSets}/${draft.plannedSets} séries · ${Format.volume(draft.totalVolumeKg)}",
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

        Spacer(Modifier.height(Spacing.md))

        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            PhaseChip("execução", breakdown.executionSeconds, breakdown.executionShare, ExecutionColor, Modifier.weight(1f))
            PhaseChip("descanso", breakdown.restSeconds, breakdown.restShare, RestColor, Modifier.weight(1f))
            PhaseChip("ocioso", breakdown.idleSeconds, breakdown.idleShare, IdleColor, Modifier.weight(1f))
        }
    }
}

@Composable
private fun PhaseChip(
    label: String,
    seconds: Long,
    share: Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.shapes.medium

    Column(
        modifier
            .clip(shape)
            .background(IronSurface)
            .border(1.dp, IronBorder, shape)
            .padding(horizontal = Spacing.sm, vertical = Spacing.sm),
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = IronTextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(2.dp))
        Text(text = Format.duration(seconds), style = IronText.tabular, color = color, maxLines = 1)
        Text(
            text = Format.percent(share.toDouble()),
            style = MaterialTheme.typography.labelSmall,
            color = IronTextMuted,
        )
    }
}

@Composable
private fun ExerciseChoiceRow(
    exercise: SessionExercise,
    quickAction: QuickAction,
    onClick: () -> Unit,
    onQuickAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val done = exercise.isComplete
    val running = exercise.started && !done
    val shape = MaterialTheme.shapes.large
    val nameColor = if (done) IronTextMuted else IronTextPrimary
    val reference = exercise.suggestion?.takeIf { !exercise.started }?.substringBefore(" · ")
        ?.let { "sugestão: $it" }
        ?: exercise.previousSummary?.let { "última vez: $it" }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 68.dp)
            .clip(shape)
            .background(IronSurface)
            .border(1.dp, if (running) IronPrimary else IronBorder, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (done) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                tint = IronPositive,
                modifier = Modifier
                    .padding(end = Spacing.sm)
                    .size(18.dp),
            )
        }

        Column(Modifier.weight(1f)) {
            Text(
                text = exercise.name,
                style = MaterialTheme.typography.titleMedium,
                color = nameColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = listOfNotNull(exercise.muscleGroup, exercise.equipment).joinToString(" · "),
                style = MaterialTheme.typography.labelMedium,
                color = IronTextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (reference != null) {
                Text(
                    text = reference,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (exercise.started || exercise.suggestion == null) IronTextSecondary else IronRecord,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Column(
            modifier = Modifier.padding(start = Spacing.sm),
            horizontalAlignment = Alignment.End,
        ) {
            Text(
                text = "${exercise.doneSets}/${exercise.sets.size}",
                style = IronText.metricSmall,
                color = when {
                    done -> IronPositive
                    running -> IronPrimary
                    else -> IronTextSecondary
                },
            )
            if (exercise.started) {
                Text(
                    text = Format.volume(exercise.volumeKg),
                    style = MaterialTheme.typography.labelMedium,
                    color = IronTextMuted,
                    maxLines = 1,
                )
            } else {
                Text(
                    text = "séries",
                    style = MaterialTheme.typography.labelMedium,
                    color = IronTextMuted,
                )
            }
        }

        QuickActionButton(
            action = quickAction,
            onClick = onQuickAction,
            modifier = Modifier.padding(start = Spacing.sm),
        )
    }
}

@Composable
private fun QuickActionButton(action: QuickAction, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = MaterialTheme.shapes.medium
    val label = when (action) {
        QuickAction.Start -> "INICIAR"
        QuickAction.Finish -> "PARAR"
        QuickAction.Resting -> "DESCANSO"
        QuickAction.Done -> "FEITO"
        QuickAction.Blocked -> "—"
    }
    val color = when (action) {
        QuickAction.Start -> IronPrimary
        QuickAction.Finish -> IronPrimary
        QuickAction.Resting -> IronRecord
        QuickAction.Done -> IronPositive
        QuickAction.Blocked -> IronTextMuted
    }
    val filled = action == QuickAction.Finish
    val enabled = action == QuickAction.Start || action == QuickAction.Finish

    Box(
        modifier
            .width(76.dp)
            .clip(shape)
            .background(if (filled) color else IronSurface)
            .border(1.dp, if (enabled) color else IronBorder, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = Spacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (filled) IronBackground else color,
            maxLines = 1,
            softWrap = false,
        )
    }
}
