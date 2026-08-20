package com.archieapps.exercises.ui.feature.session

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.archieapps.exercises.core.format.Format
import com.archieapps.exercises.core.session.ActiveSession
import com.archieapps.exercises.ui.component.ExercisePickerOverlay
import com.archieapps.exercises.ui.component.GhostButton
import com.archieapps.exercises.ui.design.IronBorder
import com.archieapps.exercises.ui.design.IronPrimary
import com.archieapps.exercises.ui.design.IronSurface
import com.archieapps.exercises.ui.design.IronSurfaceHigh
import com.archieapps.exercises.ui.design.IronTextMuted
import com.archieapps.exercises.ui.design.IronTextPrimary
import com.archieapps.exercises.ui.design.IronTextSecondary
import com.archieapps.exercises.ui.design.Spacing

/**
 * Container da sessão ao vivo: decide entre a lista de escolha e a tela do exercício, e concentra o
 * que vale para os dois níveis — descanso, seletor do catálogo, diálogos e tela ligada.
 */
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
    val open = draft.openExercise()
    val view = LocalView.current

    DisposableEffect(state.keepScreenOn) {
        view.keepScreenOn = state.keepScreenOn
        onDispose { view.keepScreenOn = false }
    }

    BackHandler(enabled = open != null) { actions.onCloseExercise() }

    Box(modifier.fillMaxSize()) {
        if (open != null && draft.openExerciseIndex != null) {
            ExerciseLogScreen(
                state = state,
                exercise = open,
                index = draft.openExerciseIndex,
                actions = actions,
                contentPadding = contentPadding,
            )
        } else {
            SessionExerciseListScreen(
                state = state,
                draft = draft,
                onOpenExercise = actions.onOpenExercise,
                onAddExercise = { pickingExercise = true },
                onFinish = { confirmFinish = true },
                onDiscard = { confirmDiscard = true },
                onDismissMessage = actions.onDismissMessage,
                contentPadding = contentPadding,
            )
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

