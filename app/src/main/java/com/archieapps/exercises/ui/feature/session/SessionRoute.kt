package com.archieapps.exercises.ui.feature.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.archieapps.exercises.core.data.FinishOutcome
import com.archieapps.exercises.core.db.ExerciseEntity
import com.archieapps.exercises.core.format.Format
import com.archieapps.exercises.core.format.Pace
import com.archieapps.exercises.core.net.dto.TemplateDto
import com.archieapps.exercises.core.session.ActiveSession
import com.archieapps.exercises.core.session.SessionTimeBreakdown
import com.archieapps.exercises.core.session.SessionTimelineLocal
import com.archieapps.exercises.ui.component.GhostButton
import com.archieapps.exercises.ui.component.HeroSurface
import com.archieapps.exercises.ui.component.MetricValue
import com.archieapps.exercises.ui.component.PrimaryButton
import com.archieapps.exercises.ui.component.IronCard
import com.archieapps.exercises.ui.component.PillTag
import com.archieapps.exercises.ui.component.SectionLabel
import com.archieapps.exercises.ui.component.SessionTimeCard
import com.archieapps.exercises.ui.component.SessionTimeMetrics
import com.archieapps.exercises.ui.component.SessionTimelineList
import com.archieapps.exercises.ui.component.TimelineEntry
import com.archieapps.exercises.ui.design.IronPositive
import com.archieapps.exercises.ui.design.IronRecord
import com.archieapps.exercises.ui.design.IronTextSecondary
import com.archieapps.exercises.ui.design.Spacing

data class SessionActions(
    val onOpenExercise: (Int) -> Unit,
    val onCloseExercise: () -> Unit,
    val onAddExercise: (ExerciseEntity) -> Unit,
    val onRemoveExercise: (Int) -> Unit,
    val onAddSet: (Int) -> Unit,
    val onRemoveSet: (Int, Int) -> Unit,
    val onChangeReps: (Int, Int, Int) -> Unit,
    val onChangeWeight: (Int, Int, Int) -> Unit,
    val onSetWeight: (Int, Int, Double) -> Unit,
    val onChangeDuration: (Int, Int, Int) -> Unit,
    val onChangeDistance: (Int, Int, Int) -> Unit,
    val onSetDistance: (Int, Int, Double) -> Unit,
    val onStartSet: (Int, Int) -> Unit,
    val onStartNextSet: (Int) -> Unit,
    val onFinishSet: () -> Unit,
    val onUndoSet: (Int, Int) -> Unit,
    val onAddRest: (Int) -> Unit,
    val onSkipRest: () -> Unit,
    val onFinish: () -> Unit,
    val onDiscard: () -> Unit,
    val onDismissMessage: () -> Unit,
)

data class PreviewActions(
    val onStart: () -> Unit,
    val onCancel: () -> Unit,
    val onAddExercise: (ExerciseEntity) -> Unit,
    val onRemoveExercise: (Int) -> Unit,
)

@Composable
fun SessionRoute(
    state: SessionUiState,
    actions: SessionActions,
    previewActions: PreviewActions,
    onClose: () -> Unit,
    onRepeatLast: () -> Unit,
    onStartTemplate: (TemplateDto) -> Unit,
    onRepeatWorkout: (com.archieapps.exercises.core.net.dto.WorkoutDto) -> Unit,
    onStartEmpty: () -> Unit,
    onManageTemplates: () -> Unit,
    onPlanWorkout: () -> Unit,
    onDone: () -> Unit,
    onOpenWorkout: (Int) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val outcome = state.outcome
    val draft = state.draft
    val pending = state.pendingStart

    when {
        outcome != null -> SessionSummary(
            outcome = outcome,
            finished = state.finished,
            onDone = onDone,
            onOpenWorkout = onOpenWorkout,
            modifier = modifier,
            contentPadding = contentPadding,
        )

        draft != null -> LiveSessionScreen(
            state = state,
            draft = draft,
            actions = actions,
            modifier = modifier,
            contentPadding = contentPadding,
        )

        pending != null -> SessionPreviewScreen(
            state = state,
            pending = pending,
            actions = previewActions,
            modifier = modifier,
            contentPadding = contentPadding,
        )

        else -> StartSessionScreen(
            state = state,
            onClose = onClose,
            onRepeatLast = onRepeatLast,
            onStartTemplate = onStartTemplate,
            onRepeatWorkout = onRepeatWorkout,
            onStartEmpty = onStartEmpty,
            onManageTemplates = onManageTemplates,
            onPlanWorkout = onPlanWorkout,
            onDismissMessage = actions.onDismissMessage,
            modifier = modifier,
            contentPadding = contentPadding,
        )
    }
}

@Composable
private fun SessionSummary(
    outcome: FinishOutcome,
    finished: ActiveSession?,
    onDone: () -> Unit,
    onOpenWorkout: (Int) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val saved = outcome is FinishOutcome.Saved
    val nowMillis = finished?.events?.lastOrNull()?.atEpochMillis ?: System.currentTimeMillis()
    val breakdown = finished?.let { SessionTimeBreakdown.of(it, nowMillis) }
    val entries = finished?.let { session ->
        SessionTimelineLocal.segments(session, nowMillis).map(TimelineEntry::of)
    }.orEmpty()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(horizontal = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Spacer(Modifier.height(Spacing.md))

        HeroSurface {
            SectionLabel(text = if (saved) "treino salvo" else "guardado no aparelho")
            Spacer(Modifier.height(Spacing.md))
            MetricValue(
                value = Format.clock(breakdown?.totalSeconds),
                style = MaterialTheme.typography.displayMedium,
                color = if (saved) IronPositive else IronRecord,
            )
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = when (outcome) {
                    is FinishOutcome.Saved -> "Treino registrado no servidor."
                    is FinishOutcome.Queued -> outcome.reason
                    is FinishOutcome.Rejected -> outcome.reason
                },
                style = MaterialTheme.typography.bodyLarge,
                color = IronTextSecondary,
            )
        }

        SessionTimeCard(
            metrics = breakdown?.let(SessionTimeMetrics::of),
            fallbackTotalSeconds = breakdown?.totalSeconds,
            modifier = Modifier.fillMaxWidth(),
        )

        finished?.takeIf { it.totalDistanceKm > 0 }?.let { session ->
            IronCard(Modifier.fillMaxWidth()) {
                SectionLabel(text = "cardio")
                Spacer(Modifier.height(Spacing.sm))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    PillTag(text = Format.distance(session.totalDistanceKm))
                    Pace.speedKmh(
                        session.totalDistanceKm,
                        session.exercises.sumOf { it.timedSeconds },
                    )?.let { PillTag(text = Format.speed(it)) }
                }
            }
        }

        if (entries.isNotEmpty()) {
            SessionTimelineList(
                entries = entries,
                startLabel = Format.timeOfDay(finished?.startedAtEpochMillis),
                endLabel = Format.timeOfDay(nowMillis),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (outcome is FinishOutcome.Saved) {
            GhostButton(
                text = "Ver treino completo",
                onClick = { onOpenWorkout(outcome.workoutId) },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        PrimaryButton(text = "Voltar para a home", onClick = onDone, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(Spacing.xl))
    }
}
