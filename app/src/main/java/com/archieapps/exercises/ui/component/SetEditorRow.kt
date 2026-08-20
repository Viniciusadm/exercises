package com.archieapps.exercises.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.archieapps.exercises.R
import com.archieapps.exercises.core.format.Format
import com.archieapps.exercises.core.session.SessionExercise
import com.archieapps.exercises.core.session.SessionSet
import com.archieapps.exercises.ui.design.IronBackground
import com.archieapps.exercises.ui.design.IronBorder
import com.archieapps.exercises.ui.design.IronPositive
import com.archieapps.exercises.ui.design.IronSurface
import com.archieapps.exercises.ui.design.IronSurfaceHigh
import com.archieapps.exercises.ui.design.IronText
import com.archieapps.exercises.ui.design.IronTextMuted
import com.archieapps.exercises.ui.design.IronTextPrimary
import com.archieapps.exercises.ui.design.IronTextSecondary
import com.archieapps.exercises.ui.design.Spacing

private val ColumnNumberWidth = 34.dp
private val ColumnActionWidth = 44.dp
private val StepButtonSize = 32.dp

@Composable
fun SetEditorHeader(exercise: SessionExercise, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(
            text = "SÉRIE",
            style = MaterialTheme.typography.labelSmall,
            color = IronTextMuted,
            modifier = Modifier.width(ColumnNumberWidth),
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
                text = "CARGA (KG)",
                style = MaterialTheme.typography.labelSmall,
                color = IronTextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.width(ColumnActionWidth))
    }
}

@Composable
fun SetEditorRow(
    position: Int,
    set: SessionSet,
    exercise: SessionExercise,
    onRepsChange: (Int) -> Unit,
    onWeightChange: (Double) -> Unit,
    onDurationChange: (Int) -> Unit,
    onToggle: () -> Unit,
    onRemove: () -> Unit,
) {
    val background = if (set.done) IronPositive.copy(alpha = 0.10f) else IronSurfaceHigh

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .padding(horizontal = Spacing.sm, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Box(
            Modifier
                .width(ColumnNumberWidth)
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = position.toString(),
                style = IronText.metricSmall,
                color = if (set.done) IronPositive else IronTextSecondary,
            )
        }

        if (exercise.executionType == "time") {
            SetStepper(
                value = Format.integer(set.durationSeconds ?: 0) + "s",
                onDecrease = { onDurationChange(-15) },
                onIncrease = { onDurationChange(15) },
                modifier = Modifier.weight(1f),
            )
        } else {
            SetStepper(
                value = Format.integer(set.repetitions ?: 0),
                onDecrease = { onRepsChange(-1) },
                onIncrease = { onRepsChange(1) },
                modifier = Modifier.weight(1f),
            )
        }

        if (exercise.usesWeight) {
            SetStepper(
                value = Format.weightValue(set.weightKg ?: 0.0),
                onDecrease = { onWeightChange(-2.5) },
                onIncrease = { onWeightChange(2.5) },
                modifier = Modifier.weight(1f),
            )
        }

        Box(
            Modifier
                .size(ColumnActionWidth)
                .clip(RoundedCornerShape(12.dp))
                .background(if (set.done) IronPositive else IronSurface)
                .border(1.dp, if (set.done) IronPositive else IronBorder, RoundedCornerShape(12.dp))
                .clickable(onClick = onToggle),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = if (set.done) "Desmarcar série" else "Concluir série",
                tint = if (set.done) IronBackground else IronTextMuted,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
fun SetStepper(
    value: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        SetStepperButton(label = "−", onClick = onDecrease)
        Text(
            text = value,
            style = IronText.stepper,
            color = IronTextPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.weight(1f),
        )
        SetStepperButton(label = "+", onClick = onIncrease)
    }
}

@Composable
private fun SetStepperButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(StepButtonSize)
            .clip(CircleShape)
            .background(IronSurface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, style = MaterialTheme.typography.titleMedium, color = IronTextSecondary)
    }
}
