package com.archieapps.exercises.ui.feature.templates

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.archieapps.exercises.R
import com.archieapps.exercises.core.format.Format
import com.archieapps.exercises.ui.component.ExercisePickerOverlay
import com.archieapps.exercises.ui.component.GhostButton
import com.archieapps.exercises.ui.component.IronCard
import com.archieapps.exercises.ui.component.LoadingSkeleton
import com.archieapps.exercises.ui.component.NoticeBanner
import com.archieapps.exercises.ui.component.PillTag
import com.archieapps.exercises.ui.component.PrimaryButton
import com.archieapps.exercises.ui.component.SectionLabel
import com.archieapps.exercises.ui.design.IronSurface
import com.archieapps.exercises.ui.design.IronSurfaceHigh
import com.archieapps.exercises.ui.design.IronText
import com.archieapps.exercises.ui.design.IronTextMuted
import com.archieapps.exercises.ui.design.IronTextPrimary
import com.archieapps.exercises.ui.design.IronTextSecondary
import com.archieapps.exercises.ui.design.Spacing
import com.archieapps.exercises.ui.feature.auth.IronTextField

@Composable
fun TemplateEditorScreen(
    state: TemplateEditorUiState,
    onNameChange: (String) -> Unit,
    onAddExercise: (com.archieapps.exercises.core.db.ExerciseEntity) -> Unit,
    onRemoveItem: (Int) -> Unit,
    onMoveItem: (Int, Int) -> Unit,
    onUpdateItem: (Int, (TemplateItemForm) -> TemplateItemForm) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    var picking by remember { mutableStateOf(false) }

    Box(modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            item {
                Column(Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md)) {
                    SectionLabel(text = if (state.isNew) "novo template" else "editar template")
                    Text(
                        text = state.name.ifBlank { "Sem nome" },
                        style = MaterialTheme.typography.headlineMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            state.message?.let { message ->
                item {
                    NoticeBanner(
                        message = message,
                        isError = state.isError,
                        actionText = "ok",
                        onAction = onDismissMessage,
                        modifier = Modifier.padding(horizontal = Spacing.lg),
                    )
                }
            }

            if (state.loading && state.items.isEmpty()) {
                item { LoadingSkeleton(Modifier.padding(horizontal = Spacing.lg)) }

                return@LazyColumn
            }

            item {
                Column(Modifier.padding(horizontal = Spacing.lg)) {
                    IronTextField(
                        value = state.name,
                        onValueChange = onNameChange,
                        label = "Nome do template",
                    )
                }
            }

            itemsIndexed(state.items, key = { index, item -> "${item.exerciseId}-$index" }) { index, item ->
                TemplateItemCard(
                    item = item,
                    position = index + 1,
                    isFirst = index == 0,
                    isLast = index == state.items.lastIndex,
                    onMoveUp = { onMoveItem(index, -1) },
                    onMoveDown = { onMoveItem(index, 1) },
                    onRemove = { onRemoveItem(index) },
                    onUpdate = { transform -> onUpdateItem(index, transform) },
                    modifier = Modifier.padding(horizontal = Spacing.lg),
                )
            }

            item {
                GhostButton(
                    text = "Adicionar exercício",
                    onClick = { picking = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg),
                )
            }

            item {
                Column(
                    Modifier.padding(horizontal = Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    PrimaryButton(
                        text = if (state.isNew) "Criar template" else "Salvar template",
                        onClick = onSave,
                        enabled = state.canSave,
                        loading = state.saving,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (!state.isNew) {
                        GhostButton(
                            text = "Excluir template",
                            onClick = onDelete,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(Spacing.xl)) }
        }

        if (picking) {
            ExercisePickerOverlay(
                catalog = state.catalog,
                onPick = {
                    onAddExercise(it)
                    picking = false
                },
                onClose = { picking = false },
            )
        }
    }
}

@Composable
private fun TemplateItemCard(
    item: TemplateItemForm,
    position: Int,
    isFirst: Boolean,
    isLast: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    onUpdate: ((TemplateItemForm) -> TemplateItemForm) -> Unit,
    modifier: Modifier = Modifier,
) {
    IronCard(modifier) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Text(
                text = position.toString(),
                style = IronText.metricSmall,
                color = IronTextMuted,
                modifier = Modifier.width(28.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(Spacing.xs))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    item.muscleGroup?.let { PillTag(text = it) }
                    item.equipment?.let { PillTag(text = it) }
                }
            }
            Column {
                MoveButton(iconRes = R.drawable.ic_chevron_left, rotation = 90f, enabled = !isFirst, onClick = onMoveUp)
                Spacer(Modifier.height(Spacing.xs))
                MoveButton(
                    iconRes = R.drawable.ic_chevron_left,
                    rotation = -90f,
                    enabled = !isLast,
                    onClick = onMoveDown,
                )
            }
        }

        Spacer(Modifier.height(Spacing.md))

        TargetRow(
            label = "Séries",
            value = item.targetSets?.toString() ?: "—",
            onDecrease = { onUpdate { it.copy(targetSets = ((it.targetSets ?: 1) - 1).coerceAtLeast(1)) } },
            onIncrease = { onUpdate { it.copy(targetSets = ((it.targetSets ?: 0) + 1).coerceAtMost(12)) } },
        )

        if (item.executionType == "time") {
            TargetRow(
                label = "Tempo (s)",
                value = item.targetReps?.toString() ?: "—",
                onDecrease = { onUpdate { it.copy(targetReps = ((it.targetReps ?: 15) - 15).coerceAtLeast(15)) } },
                onIncrease = { onUpdate { it.copy(targetReps = ((it.targetReps ?: 0) + 15).coerceAtMost(3600)) } },
            )
        } else {
            TargetRow(
                label = "Repetições",
                value = item.targetReps?.toString() ?: "—",
                onDecrease = { onUpdate { it.copy(targetReps = ((it.targetReps ?: 1) - 1).coerceAtLeast(1)) } },
                onIncrease = { onUpdate { it.copy(targetReps = ((it.targetReps ?: 0) + 1).coerceAtMost(100)) } },
            )
        }

        if (item.usesWeight) {
            TargetRow(
                label = "Carga alvo",
                value = item.targetWeightKg?.let { Format.weight(it) } ?: "—",
                onDecrease = {
                    onUpdate { it.copy(targetWeightKg = ((it.targetWeightKg ?: 0.0) - 2.5).coerceAtLeast(0.0)) }
                },
                onIncrease = {
                    onUpdate { it.copy(targetWeightKg = ((it.targetWeightKg ?: 0.0) + 2.5).coerceAtMost(999.0)) }
                },
            )
        }

        TargetRow(
            label = "Descanso",
            value = item.restSeconds?.let { seconds ->
                if (seconds >= 60) {
                    "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
                } else {
                    "${seconds}s"
                }
            } ?: "padrão",
            onDecrease = { onUpdate { it.copy(restSeconds = ((it.restSeconds ?: 90) - 15).coerceAtLeast(15)) } },
            onIncrease = { onUpdate { it.copy(restSeconds = ((it.restSeconds ?: 0) + 15).coerceAtMost(600)) } },
        )

        Spacer(Modifier.height(Spacing.sm))

        IronTextField(
            value = item.notes.orEmpty(),
            onValueChange = { value -> onUpdate { it.copy(notes = value) } },
            label = "Observação",
        )

        Spacer(Modifier.height(Spacing.sm))

        GhostButton(text = "Remover do template", onClick = onRemove, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun TargetRow(
    label: String,
    value: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = IronTextSecondary,
            modifier = Modifier.weight(1f),
        )
        StepButton(label = "−", onClick = onDecrease)
        Text(
            text = value,
            style = IronText.metricSmall,
            color = IronTextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(74.dp),
        )
        StepButton(label = "+", onClick = onIncrease)
    }
}

@Composable
private fun StepButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(IronSurfaceHigh)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, style = MaterialTheme.typography.titleMedium, color = IronTextSecondary)
    }
}

@Composable
private fun MoveButton(iconRes: Int, rotation: Float, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(IronSurface)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = if (rotation > 0) "Mover para cima" else "Mover para baixo",
            tint = if (enabled) IronTextSecondary else IronTextMuted.copy(alpha = 0.4f),
            modifier = Modifier
                .size(16.dp)
                .rotate(rotation),
        )
    }
}
