package com.archieapps.exercises.ui.feature.session

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.archieapps.exercises.core.data.FinishOutcome
import com.archieapps.exercises.core.db.ExerciseEntity
import com.archieapps.exercises.core.format.Format
import com.archieapps.exercises.core.net.dto.TemplateDto
import com.archieapps.exercises.ui.component.CardHeader
import com.archieapps.exercises.ui.component.GhostButton
import com.archieapps.exercises.ui.component.HeroSurface
import com.archieapps.exercises.ui.component.IronCard
import com.archieapps.exercises.ui.component.MetricValue
import com.archieapps.exercises.ui.component.PillTag
import com.archieapps.exercises.ui.component.PrimaryButton
import com.archieapps.exercises.ui.component.SectionLabel
import com.archieapps.exercises.ui.design.IronBackground
import com.archieapps.exercises.ui.design.IronPositive
import com.archieapps.exercises.ui.design.IronRecord
import com.archieapps.exercises.ui.design.IronSurface
import com.archieapps.exercises.ui.design.IronTextMuted
import com.archieapps.exercises.ui.design.IronTextSecondary
import com.archieapps.exercises.ui.feature.auth.IronTextField
import com.archieapps.exercises.ui.design.Spacing

data class SessionActions(
    val onSelectExercise: (Int) -> Unit,
    val onAddExercise: (ExerciseEntity) -> Unit,
    val onRemoveExercise: (Int) -> Unit,
    val onAddSet: (Int) -> Unit,
    val onRemoveSet: (Int, Int) -> Unit,
    val onChangeReps: (Int, Int, Int) -> Unit,
    val onChangeWeight: (Int, Int, Double) -> Unit,
    val onChangeDuration: (Int, Int, Int) -> Unit,
    val onToggleSet: (Int, Int) -> Unit,
    val onAddRest: (Int) -> Unit,
    val onSkipRest: () -> Unit,
    val onFinish: () -> Unit,
    val onDiscard: () -> Unit,
    val onDismissMessage: () -> Unit,
)

@Composable
fun SessionRoute(
    state: SessionUiState,
    actions: SessionActions,
    onRepeatLast: () -> Unit,
    onStartTemplate: (TemplateDto) -> Unit,
    onStartEmpty: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val outcome = state.outcome
    val draft = state.draft

    when {
        outcome != null -> SessionSummary(
            outcome = outcome,
            onDone = onDone,
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

        else -> StartSessionScreen(
            state = state,
            onRepeatLast = onRepeatLast,
            onStartTemplate = onStartTemplate,
            onStartEmpty = onStartEmpty,
            onDismissMessage = actions.onDismissMessage,
            modifier = modifier,
            contentPadding = contentPadding,
        )
    }
}

@Composable
private fun SessionSummary(
    outcome: FinishOutcome,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val saved = outcome is FinishOutcome.Saved

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = Spacing.lg),
        verticalArrangement = Arrangement.Center,
    ) {
        HeroSurface {
            SectionLabel(text = if (saved) "treino salvo" else "guardado no aparelho")
            Spacer(Modifier.height(Spacing.md))
            MetricValue(
                value = if (saved) "OK" else "OFF",
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
            Spacer(Modifier.height(Spacing.xl))
            PrimaryButton(text = "Voltar para a home", onClick = onDone, modifier = Modifier.fillMaxWidth())
        }
    }
}
