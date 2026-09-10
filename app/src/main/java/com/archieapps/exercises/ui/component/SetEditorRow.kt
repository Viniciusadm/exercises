package com.archieapps.exercises.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.archieapps.exercises.R
import com.archieapps.exercises.core.format.Format
import com.archieapps.exercises.core.session.SessionExercise
import com.archieapps.exercises.core.session.SessionSet
import com.archieapps.exercises.core.session.SetRunState
import com.archieapps.exercises.ui.design.IronBackground
import com.archieapps.exercises.ui.design.IronBorder
import com.archieapps.exercises.ui.design.IronPositive
import com.archieapps.exercises.ui.design.IronPrimary
import com.archieapps.exercises.ui.design.IronSurface
import com.archieapps.exercises.ui.design.IronSurfaceHigh
import com.archieapps.exercises.ui.design.IronText
import com.archieapps.exercises.ui.design.IronTextMuted
import com.archieapps.exercises.ui.design.IronTextPrimary
import com.archieapps.exercises.ui.design.IronTextSecondary
import com.archieapps.exercises.ui.design.Spacing

private val ColumnNumberWidth = 34.dp
private val ColumnActionWidth = 62.dp
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
            text = if (exercise.isTimed) "TEMPO" else "REPS",
            style = MaterialTheme.typography.labelSmall,
            color = IronTextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        if (exercise.tracksDistance) {
            Text(
                text = "KM",
                style = MaterialTheme.typography.labelSmall,
                color = IronTextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
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
    onWeightStep: (Int) -> Unit,
    onWeightSet: (Double) -> Unit,
    onDurationChange: (Int) -> Unit,
    onDistanceStep: (Int) -> Unit,
    onDistanceSet: (Double) -> Unit,
    action: SetAction,
    onRemove: () -> Unit,
    removable: Boolean = true,
    weightOptions: List<Double> = emptyList(),
    elapsedSeconds: Long? = null,
) {
    val state = action.runState
    val background = when (state) {
        SetRunState.Running -> IronPrimary.copy(alpha = 0.14f)
        SetRunState.Done -> IronPositive.copy(alpha = 0.10f)
        SetRunState.Pending -> IronSurfaceHigh
    }
    var pickingWeight by remember { mutableStateOf(false) }
    var pickingDistance by remember { mutableStateOf(false) }
    val running = state == SetRunState.Running

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .padding(horizontal = Spacing.sm, vertical = Spacing.sm),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Box(
                Modifier
                    .width(ColumnNumberWidth)
                    .clickable(enabled = removable, onClick = onRemove),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = position.toString(),
                    style = IronText.metricSmall,
                    color = when (state) {
                        SetRunState.Running -> IronPrimary
                        SetRunState.Done -> IronPositive
                        SetRunState.Pending -> IronTextSecondary
                    },
                )
            }

            if (exercise.isTimed) {
                SetStepper(
                    value = if (running && elapsedSeconds != null) {
                        Format.clock(elapsedSeconds)
                    } else {
                        Format.integer(set.durationSeconds ?: 0) + "s"
                    },
                    onDecrease = { onDurationChange(-15) },
                    onIncrease = { onDurationChange(15) },
                    modifier = Modifier.weight(1f),
                    enabled = !running,
                    highlighted = running,
                )
            } else {
                SetStepper(
                    value = Format.integer(set.repetitions ?: 0),
                    onDecrease = { onRepsChange(-1) },
                    onIncrease = { onRepsChange(1) },
                    modifier = Modifier.weight(1f),
                )
            }

            if (exercise.tracksDistance) {
                SetStepper(
                    value = Format.distanceValue(set.distanceKm ?: 0.0),
                    onDecrease = { onDistanceStep(-1) },
                    onIncrease = { onDistanceStep(1) },
                    modifier = Modifier.weight(1f),
                    onValueClick = { pickingDistance = true },
                )
            }

            if (exercise.usesWeight) {
                SetStepper(
                    value = Format.weightValue(set.weightKg ?: 0.0),
                    onDecrease = { onWeightStep(-1) },
                    onIncrease = { onWeightStep(1) },
                    modifier = Modifier.weight(1f),
                    onValueClick = { pickingWeight = true },
                )
            }

            SetActionButton(action = action)
        }

        set.speedKmh?.takeIf { exercise.tracksDistance }?.let { speed ->
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = "${Format.speed(speed)} · ${Format.pace(set.paceSecondsPerKm)}",
                style = MaterialTheme.typography.labelSmall,
                color = IronTextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (pickingDistance) {
        DistancePickerDialog(
            current = set.distanceKm,
            onDismiss = { pickingDistance = false },
            onConfirm = { distance ->
                onDistanceSet(distance)
                pickingDistance = false
            },
        )
    }

    if (pickingWeight) {
        WeightPickerDialog(
            current = set.weightKg,
            options = weightOptions,
            onDismiss = { pickingWeight = false },
            onConfirm = { weight ->
                onWeightSet(weight)
                pickingWeight = false
            },
        )
    }
}

@Composable
private fun SetActionButton(action: SetAction) {
    val shape = RoundedCornerShape(12.dp)
    val enabled = action.enabled

    val style = when (action.runState) {
        SetRunState.Running -> SetActionStyle(IronPrimary, IronPrimary, action.runningLabel, IronBackground)
        SetRunState.Done -> SetActionStyle(IronPositive, IronPositive, action.doneLabel, IronBackground)
        SetRunState.Pending -> SetActionStyle(
            IronSurface,
            if (enabled) IronBorder else IronBorder.copy(alpha = 0.4f),
            action.pendingLabel,
            if (enabled) IronTextSecondary else IronTextMuted,
        )
    }

    Box(
        Modifier
            .width(ColumnActionWidth)
            .clip(shape)
            .background(style.background)
            .border(1.dp, style.border, shape)
            .clickable(enabled = enabled, onClick = action::invoke)
            .padding(vertical = Spacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = style.label,
            style = MaterialTheme.typography.labelSmall,
            color = style.tint,
            maxLines = 1,
            softWrap = false,
        )
    }
}

private data class SetActionStyle(
    val background: Color,
    val border: Color,
    val label: String,
    val tint: Color,
)

sealed interface SetAction {
    val runState: SetRunState
    val enabled: Boolean
    val pendingLabel: String
    val runningLabel: String
    val doneLabel: String

    fun invoke()

    data class Live(
        override val runState: SetRunState,
        val startEnabled: Boolean,
        val onStart: () -> Unit,
        val onFinish: () -> Unit,
        val onUndo: () -> Unit,
    ) : SetAction {
        override val enabled: Boolean get() = runState != SetRunState.Pending || startEnabled

        override val pendingLabel = "INICIAR"

        override val runningLabel = "PARAR"

        override val doneLabel = "FEITA"

        override fun invoke() = when (runState) {
            SetRunState.Running -> onFinish()
            SetRunState.Done -> onUndo()
            SetRunState.Pending -> onStart()
        }
    }

    data class Toggle(val done: Boolean, val onToggle: () -> Unit) : SetAction {
        override val runState: SetRunState get() = if (done) SetRunState.Done else SetRunState.Pending

        override val enabled = true

        override val pendingLabel = "MARCAR"

        override val runningLabel = "MARCAR"

        override val doneLabel = "FEITA"

        override fun invoke() = onToggle()
    }
}

@Composable
fun SetStepper(
    value: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier,
    onValueClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    highlighted: Boolean = false,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        SetStepperButton(label = "−", onClick = onDecrease, enabled = enabled)
        Text(
            text = value,
            style = IronText.stepper,
            color = if (highlighted) IronPrimary else IronTextPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier
                .weight(1f)
                .then(
                    if (onValueClick == null || !enabled) {
                        Modifier
                    } else {
                        Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClickLabel = "Escolher carga", onClick = onValueClick)
                            .padding(vertical = Spacing.xs)
                    }
                ),
        )
        SetStepperButton(label = "+", onClick = onIncrease, enabled = enabled)
    }
}

@Composable
private fun SetStepperButton(label: String, onClick: () -> Unit, enabled: Boolean = true) {
    Box(
        Modifier
            .size(StepButtonSize)
            .clip(CircleShape)
            .background(IronSurface)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = if (enabled) IronTextSecondary else IronTextMuted,
        )
    }
}
