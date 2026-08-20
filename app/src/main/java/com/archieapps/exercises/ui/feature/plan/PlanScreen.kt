package com.archieapps.exercises.ui.feature.plan

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.archieapps.exercises.R
import com.archieapps.exercises.core.net.dto.PlanDayDto
import com.archieapps.exercises.core.net.dto.PlanExerciseDto
import com.archieapps.exercises.ui.component.CardHeader
import com.archieapps.exercises.ui.component.CircleIconButton
import com.archieapps.exercises.ui.component.EmptyState
import com.archieapps.exercises.ui.component.GhostButton
import com.archieapps.exercises.ui.component.HairLine
import com.archieapps.exercises.ui.component.IronButtonHeight
import com.archieapps.exercises.ui.component.IronCard
import com.archieapps.exercises.ui.component.NoticeBanner
import com.archieapps.exercises.ui.component.PillTag
import com.archieapps.exercises.ui.component.PrimaryButton
import com.archieapps.exercises.ui.component.SectionLabel
import com.archieapps.exercises.ui.component.SelectChip
import com.archieapps.exercises.ui.component.SetStepper
import com.archieapps.exercises.ui.design.IronBackground
import com.archieapps.exercises.ui.design.IronPrimary
import com.archieapps.exercises.ui.design.IronRecord
import com.archieapps.exercises.ui.design.IronSurface
import com.archieapps.exercises.ui.design.IronText
import com.archieapps.exercises.ui.design.IronTextMuted
import com.archieapps.exercises.ui.design.IronTextSecondary
import com.archieapps.exercises.ui.design.Spacing

@Composable
fun PlanScreen(
    state: PlanUiState,
    actions: PlanActions,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    Box(modifier.fillMaxSize()) {
        when (state.step) {
            PlanStep.FORM -> PlanFormScreen(state, actions, contentPadding)
            PlanStep.GENERATING -> PlanGeneratingScreen(state, contentPadding)
            PlanStep.REVIEW -> PlanReviewScreen(state, actions, contentPadding)
            PlanStep.DONE -> PlanDoneScreen(state, actions, contentPadding)
        }

        if (state.swappingRef != null) {
            PlanSwapOverlay(state, actions)
        }
    }
}

data class PlanActions(
    val onBack: () -> Unit,
    val onSetObjective: (String) -> Unit,
    val onSetDays: (Int) -> Unit,
    val onSetMinutes: (Int) -> Unit,
    val onToggleEquipment: (String) -> Unit,
    val onClearEquipment: () -> Unit,
    val onGenerate: () -> Unit,
    val onBackToForm: () -> Unit,
    val onDiscard: () -> Unit,
    val onSetSets: (Int, String, Int) -> Unit,
    val onSetReps: (Int, String, Int?) -> Unit,
    val onRemove: (Int, String) -> Unit,
    val onOpenSwap: (Int, String) -> Unit,
    val onCloseSwap: () -> Unit,
    val onApplySwap: (PlanExerciseDto) -> Unit,
    val onConfirm: () -> Unit,
    val onOpenWorkouts: () -> Unit,
    val onPlanAnother: () -> Unit,
    val onDismissMessage: () -> Unit,
)

@Composable
private fun PlanFormScreen(state: PlanUiState, actions: PlanActions, contentPadding: PaddingValues) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        item { PlanHeader(title = "Planejar treino", onBack = actions.onBack) }

        state.message?.let { message ->
            item {
                NoticeBanner(
                    message = message,
                    isError = true,
                    actionText = "ok",
                    onAction = actions.onDismissMessage,
                    modifier = Modifier.padding(horizontal = Spacing.lg),
                )
            }
        }

        item {
            IronCard(Modifier.padding(horizontal = Spacing.lg)) {
                CardHeader(title = "Objetivo")
                Spacer(Modifier.height(Spacing.sm))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    items(state.options.objectives, key = { it.value }) { objective ->
                        SelectChip(
                            label = objective.label,
                            selected = objective.value == state.objective,
                            onClick = { actions.onSetObjective(objective.value) },
                        )
                    }
                }
            }
        }

        item {
            IronCard(Modifier.padding(horizontal = Spacing.lg)) {
                CardHeader(title = "Dias por semana")
                Spacer(Modifier.height(Spacing.sm))
                SetStepper(
                    value = state.daysPerWeek.toString(),
                    onDecrease = { actions.onSetDays(state.daysPerWeek - 1) },
                    onIncrease = { actions.onSetDays(state.daysPerWeek + 1) },
                )
            }
        }

        item {
            IronCard(Modifier.padding(horizontal = Spacing.lg)) {
                CardHeader(title = "Minutos por treino")
                Spacer(Modifier.height(Spacing.sm))
                SetStepper(
                    value = "${state.minutesPerDay} min",
                    onDecrease = { actions.onSetMinutes(state.minutesPerDay - state.options.minutesPerDay.step) },
                    onIncrease = { actions.onSetMinutes(state.minutesPerDay + state.options.minutesPerDay.step) },
                )
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    text = "Cabem cerca de ${estimatedSets(state.minutesPerDay)} séries.",
                    style = MaterialTheme.typography.bodySmall,
                    color = IronTextMuted,
                )
            }
        }

        if (state.equipmentOptions.isNotEmpty()) {
            item {
                IronCard(Modifier.padding(horizontal = Spacing.lg)) {
                    CardHeader(
                        title = "Equipamento",
                        trailing = {
                            if (state.equipment.isNotEmpty()) {
                                GhostButton(text = "Limpar", onClick = actions.onClearEquipment)
                            }
                        },
                    )
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        text = if (state.equipment.isEmpty()) {
                            "Sem filtro: vale tudo que você tem."
                        } else {
                            "Só exercícios com o que você marcou."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = IronTextMuted,
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        items(state.equipmentOptions, key = { it }) { option ->
                            SelectChip(
                                label = option,
                                selected = option in state.equipment,
                                onClick = { actions.onToggleEquipment(option) },
                            )
                        }
                    }
                }
            }
        }

        item {
            PrimaryButton(
                text = "Montar plano",
                onClick = actions.onGenerate,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg),
            )
        }

        item { Spacer(Modifier.height(Spacing.md)) }
    }
}

@Composable
private fun PlanGeneratingScreen(state: PlanUiState, contentPadding: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(Spacing.xl),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(color = IronPrimary)
        Spacer(Modifier.height(Spacing.lg))
        Text(
            text = "Montando ${state.daysPerWeek} ${if (state.daysPerWeek == 1) "treino" else "treinos"}",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = "Olhando o que seus músculos andaram recebendo nas últimas 4 semanas.",
            style = MaterialTheme.typography.bodyMedium,
            color = IronTextSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PlanReviewScreen(state: PlanUiState, actions: PlanActions, contentPadding: PaddingValues) {
    val proposal = state.proposal

    if (proposal == null || proposal.days.isEmpty()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            PlanHeader(title = "Revisar plano", onBack = actions.onBackToForm)
            EmptyState(
                title = "Nada para revisar",
                message = "O plano ficou vazio. Ajuste o formulário e tente de novo.",
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        item { PlanHeader(title = "Revisar plano", onBack = actions.onBackToForm) }

        state.message?.let { message ->
            item {
                NoticeBanner(
                    message = message,
                    isError = true,
                    actionText = "ok",
                    onAction = actions.onDismissMessage,
                    modifier = Modifier.padding(horizontal = Spacing.lg),
                )
            }
        }

        item {
            IronCard(Modifier.padding(horizontal = Spacing.lg)) {
                Text(text = state.summary, style = IronText.metricSmall)

                proposal.summary?.let { summary ->
                    Spacer(Modifier.height(Spacing.sm))
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = IronTextSecondary,
                    )
                }

                if (state.byAi) {
                    Spacer(Modifier.height(Spacing.sm))
                    PillTag(text = "organizado por IA")
                }
            }
        }

        state.warnings.forEach { warning ->
            item {
                NoticeBanner(message = warning, modifier = Modifier.padding(horizontal = Spacing.lg))
            }
        }

        if (state.uncoverable.isNotEmpty()) {
            item {
                NoticeBanner(
                    message = "Sem exercício para: ${state.uncoverable.joinToString(", ")}.",
                    modifier = Modifier.padding(horizontal = Spacing.lg),
                )
            }
        }

        proposal.days.forEach { day ->
            item(key = "day-${day.day}") {
                PlanDayCard(day = day, state = state, actions = actions)
            }
        }

        state.problems.forEach { problem ->
            item {
                NoticeBanner(
                    message = problem,
                    isError = true,
                    modifier = Modifier.padding(horizontal = Spacing.lg),
                )
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                GhostButton(
                    text = "Descartar",
                    onClick = actions.onDiscard,
                    modifier = Modifier
                        .weight(1f)
                        .height(IronButtonHeight),
                )
                PrimaryButton(
                    text = confirmLabel(proposal.days.size),
                    onClick = actions.onConfirm,
                    enabled = state.canConfirm,
                    loading = state.confirming,
                    modifier = Modifier
                        .weight(1f)
                        .height(IronButtonHeight),
                )
            }
        }

        item { Spacer(Modifier.height(Spacing.md)) }
    }
}

@Composable
private fun PlanDayCard(day: PlanDayDto, state: PlanUiState, actions: PlanActions) {
    IronCard(Modifier.padding(horizontal = Spacing.lg)) {
        CardHeader(
            title = day.name,
            trailing = {
                Text(
                    text = "${daySets(day)} sér · ${dayMinutes(day)} min",
                    style = IronText.tabular,
                    color = IronTextSecondary,
                )
            },
        )

        day.exercises.forEachIndexed { index, item ->
            if (index > 0) HairLine()

            PlanExerciseRow(day = day, item = item, state = state, actions = actions)
        }
    }
}

@Composable
private fun PlanExerciseRow(
    day: PlanDayDto,
    item: PlanExerciseDto,
    state: PlanUiState,
    actions: PlanActions,
) {
    Column(Modifier.padding(vertical = Spacing.sm)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = item.muscles.primary.joinToString(", ") { it.name }.ifBlank { "—" },
                    style = MaterialTheme.typography.bodySmall,
                    color = IronTextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (item.isNew) {
                Spacer(Modifier.width(Spacing.sm))
                PillTag(text = "novo no seu catálogo", color = IronRecord)
            }
        }

        Spacer(Modifier.height(Spacing.sm))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(text = "Séries", style = MaterialTheme.typography.labelSmall, color = IronTextMuted)
                SetStepper(
                    value = item.targetSets.toString(),
                    onDecrease = { actions.onSetSets(day.day, item.ref, item.targetSets - 1) },
                    onIncrease = { actions.onSetSets(day.day, item.ref, item.targetSets + 1) },
                )
            }

            Spacer(Modifier.width(Spacing.md))

            Column(Modifier.weight(1f)) {
                Text(text = repsLabel(item), style = MaterialTheme.typography.labelSmall, color = IronTextMuted)

                if (item.targetReps == null) {
                    Text(
                        text = item.notes ?: "por tempo",
                        style = IronText.tabular,
                        color = IronTextSecondary,
                        maxLines = 1,
                    )
                } else {
                    SetStepper(
                        value = item.targetReps.toString(),
                        onDecrease = { actions.onSetReps(day.day, item.ref, item.targetReps - 1) },
                        onIncrease = { actions.onSetReps(day.day, item.ref, item.targetReps + 1) },
                    )
                }
            }
        }

        Spacer(Modifier.height(Spacing.xs))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = loadLabel(item),
                style = MaterialTheme.typography.bodySmall,
                color = IronTextMuted,
                modifier = Modifier.weight(1f),
            )
            GhostButton(text = "Trocar", onClick = { actions.onOpenSwap(day.day, item.ref) })
            Spacer(Modifier.width(Spacing.sm))
            GhostButton(text = "Remover", onClick = { actions.onRemove(day.day, item.ref) })
        }
    }
}

@Composable
private fun PlanSwapOverlay(state: PlanUiState, actions: PlanActions) {
    Box(
        Modifier
            .fillMaxSize()
            .background(IronBackground.copy(alpha = 0.94f))
            .clickable(onClick = actions.onCloseSwap),
    ) {
        Column(Modifier.fillMaxSize()) {
            PlanHeader(title = "Trocar exercício", onBack = actions.onCloseSwap)

            if (state.alternatives.isEmpty()) {
                EmptyState(
                    title = "Sem substituto",
                    message = "Nenhum outro exercício do seu catálogo trabalha esse músculo.",
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    modifier = Modifier.padding(horizontal = Spacing.lg),
                ) {
                    items(state.alternatives, key = { it.ref }) { option ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(IronSurface, RoundedCornerShape(14.dp))
                                .clickable { actions.onApplySwap(option) }
                                .padding(Spacing.lg),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(text = option.name, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = option.muscles.primary.joinToString(", ") { it.name },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = IronTextMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            option.equipment?.let { PillTag(text = it) }
                        }
                    }

                    item { Spacer(Modifier.height(Spacing.md)) }
                }
            }
        }
    }
}

@Composable
private fun PlanDoneScreen(state: PlanUiState, actions: PlanActions, contentPadding: PaddingValues) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        item { PlanHeader(title = "Plano salvo", onBack = actions.onOpenWorkouts) }

        item {
            IronCard(Modifier.padding(horizontal = Spacing.lg)) {
                Text(
                    text = if (state.alreadyConfirmed) {
                        "Esse plano já estava salvo."
                    } else {
                        "${state.createdTemplates} ${if (state.createdTemplates == 1) "treino criado" else "treinos criados"}."
                    },
                    style = IronText.metricSmall,
                )

                state.confirmation?.templates?.forEach { template ->
                    Spacer(Modifier.height(Spacing.sm))
                    Text(
                        text = "${template.name} · ${template.exercises.size} exercícios",
                        style = MaterialTheme.typography.bodyMedium,
                        color = IronTextSecondary,
                    )
                }
            }
        }

        if (state.adoptedNames.isNotEmpty()) {
            item {
                IronCard(Modifier.padding(horizontal = Spacing.lg)) {
                    SectionLabel(text = "Entraram no seu catálogo")
                    Spacer(Modifier.height(Spacing.sm))
                    Text(
                        text = state.adoptedNames.joinToString(", "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = IronTextSecondary,
                    )
                }
            }
        }

        if (state.sessionInProgress) {
            item {
                NoticeBanner(
                    message = "Você tem um treino em andamento. Os novos treinos ficam salvos para depois.",
                    modifier = Modifier.padding(horizontal = Spacing.lg),
                )
            }
        }

        item {
            Column(Modifier.padding(horizontal = Spacing.lg)) {
                PrimaryButton(
                    text = "Ver meus treinos",
                    onClick = actions.onOpenWorkouts,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(Spacing.sm))
                GhostButton(
                    text = "Planejar outro",
                    onClick = actions.onPlanAnother,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IronButtonHeight),
                )
            }
        }

        item { Spacer(Modifier.height(Spacing.md)) }
    }
}

@Composable
private fun PlanHeader(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleIconButton(
            iconRes = R.drawable.ic_chevron_left,
            contentDescription = "Voltar",
            onClick = onBack,
            tint = IronTextSecondary,
        )
        Spacer(Modifier.width(Spacing.md))
        Text(text = title, style = MaterialTheme.typography.titleLarge)
    }
}

private fun confirmLabel(days: Int): String =
    if (days == 1) "Criar treino" else "Criar $days treinos"

private fun repsLabel(item: PlanExerciseDto): String =
    if (item.targetReps == null) "Duração" else "Repetições"

private fun loadLabel(item: PlanExerciseDto): String = when {
    item.targetWeightKg != null -> "Carga sugerida: ${item.targetWeightKg} kg · descanso ${item.restSeconds ?: 0}s"
    !item.usesWeight -> "Peso corporal · descanso ${item.restSeconds ?: 0}s"
    else -> "Sem histórico de carga · descanso ${item.restSeconds ?: 0}s"
}

private fun estimatedSets(minutes: Int): Int =
    ((minutes - PLAN_OVERHEAD_MINUTES) / PLAN_MINUTES_PER_SET).toInt().coerceIn(6, 30)
