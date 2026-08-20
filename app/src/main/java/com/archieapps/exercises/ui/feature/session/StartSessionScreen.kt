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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.archieapps.exercises.R
import com.archieapps.exercises.core.format.Format
import com.archieapps.exercises.core.net.dto.TemplateDto
import com.archieapps.exercises.core.net.dto.WorkoutDto
import com.archieapps.exercises.ui.component.CardHeader
import com.archieapps.exercises.ui.component.GhostButton
import com.archieapps.exercises.ui.component.IronCard
import com.archieapps.exercises.ui.component.NoticeBanner
import com.archieapps.exercises.ui.component.PillTag
import com.archieapps.exercises.ui.component.PrimaryButton
import com.archieapps.exercises.ui.component.SectionLabel
import com.archieapps.exercises.ui.design.IronTextMuted
import com.archieapps.exercises.ui.design.IronTextSecondary
import com.archieapps.exercises.ui.design.Spacing

@Composable
fun StartSessionScreen(
    state: SessionUiState,
    onRepeatLast: () -> Unit,
    onStartTemplate: (TemplateDto) -> Unit,
    onStartEmpty: () -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        item {
            Column(Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md)) {
                SectionLabel(text = "sessão")
                Text(text = "Começar treino", style = MaterialTheme.typography.headlineMedium)
            }
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

        state.lastWorkout?.let { last ->
            item { RepeatLastCard(last, onRepeatLast, Modifier.padding(horizontal = Spacing.lg)) }
        }

        if (state.templates.isNotEmpty()) {
            item {
                SectionLabel(
                    text = "templates",
                    modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                )
            }

            items(state.templates, key = { it.id }) { template ->
                TemplateCard(
                    template = template,
                    onStart = { onStartTemplate(template) },
                    modifier = Modifier.padding(horizontal = Spacing.lg),
                )
            }
        }

        item {
            IronCard(Modifier.padding(horizontal = Spacing.lg)) {
                CardHeader(
                    title = "Treino livre",
                    caption = "monte na hora, exercício por exercício",
                )
                Spacer(Modifier.height(Spacing.md))
                GhostButton(text = "Começar vazio", onClick = onStartEmpty, modifier = Modifier.fillMaxWidth())
            }
        }

        item { Spacer(Modifier.height(Spacing.lg)) }
    }
}

@Composable
private fun RepeatLastCard(workout: WorkoutDto, onRepeat: () -> Unit, modifier: Modifier = Modifier) {
    IronCard(modifier) {
        CardHeader(
            title = "Repetir o último treino",
            caption = Format.parseDate(workout.date)?.let(Format::dateWithWeekday) ?: workout.date,
        )
        Spacer(Modifier.height(Spacing.md))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            PillTag(text = "${workout.exerciseCount} exercícios")
            PillTag(text = "${workout.setsCount} séries")
            PillTag(text = Format.volume(workout.totalVolumeKg))
        }
        if (workout.muscleGroups.isNotEmpty()) {
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = workout.muscleGroups.joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = IronTextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(Spacing.md))
        PrimaryButton(
            text = "Repetir treino",
            onClick = onRepeat,
            iconRes = R.drawable.ic_refresh,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun TemplateCard(template: TemplateDto, onStart: () -> Unit, modifier: Modifier = Modifier) {
    IronCard(modifier = modifier, onClick = onStart) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(text = template.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = template.exercises.take(3).joinToString(" · ") { it.name },
                    style = MaterialTheme.typography.bodyMedium,
                    color = IronTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            PillTag(text = "${template.exercises.size} ex.")
        }
    }
}
