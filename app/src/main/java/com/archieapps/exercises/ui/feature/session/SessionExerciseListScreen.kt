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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.archieapps.exercises.R
import com.archieapps.exercises.core.format.Format
import com.archieapps.exercises.core.session.ActiveSession
import com.archieapps.exercises.core.session.SessionExercise
import com.archieapps.exercises.ui.component.CircleIconButton
import com.archieapps.exercises.ui.component.GhostButton
import com.archieapps.exercises.ui.component.NoticeBanner
import com.archieapps.exercises.ui.component.PrimaryButton
import com.archieapps.exercises.ui.component.SectionLabel
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

@Composable
fun SessionExerciseListScreen(
    state: SessionUiState,
    draft: ActiveSession,
    onOpenExercise: (Int) -> Unit,
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
                draft = draft,
                nowMillis = state.nowMillis,
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
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        text = "Adicione o primeiro exercício para começar a registrar as séries.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = IronTextSecondary,
                    )
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
                    onClick = { onOpenExercise(index) },
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
    }
}

@Composable
private fun SessionProgressHeader(draft: ActiveSession, nowMillis: Long, onDiscard: () -> Unit) {
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
}

@Composable
private fun ExerciseChoiceRow(
    exercise: SessionExercise,
    onClick: () -> Unit,
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

        Box(
            Modifier
                .padding(start = Spacing.sm)
                .size(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_chevron_left),
                contentDescription = null,
                tint = if (running) IronPrimary else IronTextMuted,
                modifier = Modifier
                    .size(16.dp)
                    .rotate(180f),
            )
        }
    }
}
