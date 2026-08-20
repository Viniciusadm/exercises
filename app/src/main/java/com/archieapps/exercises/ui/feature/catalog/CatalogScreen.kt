package com.archieapps.exercises.ui.feature.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.archieapps.exercises.core.db.ExerciseEntity
import com.archieapps.exercises.core.format.Format
import com.archieapps.exercises.core.net.dto.TemplateDto
import com.archieapps.exercises.ui.component.CardHeader
import com.archieapps.exercises.ui.component.EmptyState
import com.archieapps.exercises.ui.component.GhostButton
import com.archieapps.exercises.core.net.dto.ExerciseMusclesDto
import com.archieapps.exercises.ui.component.IronCard
import com.archieapps.exercises.core.session.WeightLadder
import com.archieapps.exercises.ui.component.MuscleActivationSection
import com.archieapps.exercises.ui.component.NoticeBanner
import com.archieapps.exercises.ui.component.PillTag
import com.archieapps.exercises.ui.component.PrimaryButton
import com.archieapps.exercises.ui.component.SectionLabel
import com.archieapps.exercises.ui.design.IronBackground
import com.archieapps.exercises.ui.design.IronPrimary
import com.archieapps.exercises.ui.design.IronSurface
import com.archieapps.exercises.ui.design.IronSurfaceHigh
import com.archieapps.exercises.ui.design.IronText
import com.archieapps.exercises.ui.design.IronTextMuted
import com.archieapps.exercises.ui.design.IronTextPrimary
import com.archieapps.exercises.ui.design.IronTextSecondary
import com.archieapps.exercises.ui.design.Spacing
import com.archieapps.exercises.ui.feature.auth.IronTextField

@Composable
fun CatalogScreen(
    state: CatalogUiState,
    onQueryChange: (String) -> Unit,
    onMuscleFilter: (String?) -> Unit,
    onShowTemplates: (Boolean) -> Unit,
    onCreate: () -> Unit,
    onEdit: (ExerciseEntity) -> Unit,
    onFormChange: ((ExerciseForm) -> ExerciseForm) -> Unit,
    onSubmitForm: () -> Unit,
    onCancelForm: () -> Unit,
    onDeleteTemplate: (TemplateDto) -> Unit,
    onOpenTemplate: (Int?) -> Unit,
    onPlanWorkout: () -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    Box(modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            item {
                Column(Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md)) {
                    SectionLabel(text = "biblioteca")
                    Text(text = "Exercícios", style = MaterialTheme.typography.headlineMedium)
                }
            }

            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    Chip(
                        text = "Catálogo",
                        selected = !state.showTemplates,
                        onClick = { onShowTemplates(false) },
                    )
                    Chip(
                        text = "Templates (${state.templates.size})",
                        selected = state.showTemplates,
                        onClick = { onShowTemplates(true) },
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

            if (state.showTemplates) {
                item {
                    PrimaryButton(
                        text = "Novo template",
                        onClick = { onOpenTemplate(null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.lg),
                    )
                }

                item {
                    GhostButton(
                        text = "Planejar com IA",
                        onClick = onPlanWorkout,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.lg),
                    )
                }

                if (state.templates.isEmpty()) {
                    item {
                        EmptyState(
                            title = "Nenhum template",
                            message = "Monte um aqui definindo séries, repetições e carga alvo, ou abra um treino " +
                                "no histórico e use “criar template com este treino”.",
                        )
                    }
                }

                items(state.templates, key = { it.id }) { template ->
                    TemplateRow(
                        template = template,
                        onEdit = { onOpenTemplate(template.id) },
                        onDelete = { onDeleteTemplate(template) },
                        modifier = Modifier.padding(horizontal = Spacing.lg),
                    )
                }
            } else {
                item {
                    Column(Modifier.padding(horizontal = Spacing.lg)) {
                        IronTextField(
                            value = state.query,
                            onValueChange = onQueryChange,
                            label = "Buscar exercício",
                        )
                    }
                }

                item {
                    LazyRow(
                        Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = Spacing.lg),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        items(state.muscleGroups) { group ->
                            Chip(
                                text = group,
                                selected = state.muscleFilter == group,
                                onClick = { onMuscleFilter(group) },
                            )
                        }
                    }
                }

                item {
                    PrimaryButton(
                        text = "Novo exercício",
                        onClick = onCreate,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.lg),
                    )
                }

                items(state.exercises, key = { it.id }) { exercise ->
                    ExerciseRow(
                        exercise = exercise,
                        onClick = { onEdit(exercise) },
                        modifier = Modifier.padding(horizontal = Spacing.lg),
                    )
                }
            }

        }

        state.form?.let { form ->
            ExerciseFormOverlay(
                form = form,
                saving = state.saving,
                muscleGroups = state.muscleGroups,
                muscles = state.muscleMap.of(form.id),
                unmappedReason = state.muscleMap.reasonFor(form.id),
                onChange = onFormChange,
                onSubmit = onSubmitForm,
                onCancel = onCancelForm,
            )
        }
    }
}

@Composable
private fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = if (selected) IronTextPrimary else IronTextSecondary,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) IronPrimary.copy(alpha = 0.18f) else IronSurfaceHigh)
            .border(
                width = 1.dp,
                color = if (selected) IronPrimary else Color.Transparent,
                shape = RoundedCornerShape(999.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
    )
}

@Composable
private fun ExerciseRow(exercise: ExerciseEntity, onClick: () -> Unit, modifier: Modifier = Modifier) {
    IronCard(modifier = modifier, onClick = onClick, contentPadding = PaddingValues(Spacing.md)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = listOfNotNull(
                        exercise.muscleGroup,
                        exercise.equipment,
                        exercise.lastPerformedAt?.let { last ->
                            Format.parseDate(last)?.let { "última ${Format.dayMonth(it)}" }
                        },
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.labelMedium,
                    color = IronTextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = Format.weight(exercise.bestWeightKg), style = IronText.metricSmall)
                Text(
                    text = "${exercise.sessionsCount} sessões",
                    style = MaterialTheme.typography.labelMedium,
                    color = IronTextMuted,
                )
            }
        }
    }
}

@Composable
private fun TemplateRow(
    template: TemplateDto,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IronCard(modifier = modifier, onClick = onEdit) {
        CardHeader(
            title = template.name,
            caption = "${template.exercises.size} exercícios",
        )
        if (template.exercises.isNotEmpty()) {
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = template.exercises.joinToString(" · ") { item ->
                    listOfNotNull(
                        item.name,
                        item.targetSets?.let { sets -> "${sets}×${item.targetReps ?: 0}" },
                    ).joinToString(" ")
                },
                style = MaterialTheme.typography.bodyMedium,
                color = IronTextSecondary,
            )
        }
        Spacer(Modifier.height(Spacing.md))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            GhostButton(text = "Editar metas", onClick = onEdit, modifier = Modifier.weight(1f))
            GhostButton(text = "Remover", onClick = onDelete, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ExerciseFormOverlay(
    form: ExerciseForm,
    saving: Boolean,
    muscleGroups: List<String>,
    muscles: ExerciseMusclesDto,
    unmappedReason: String?,
    onChange: ((ExerciseForm) -> ExerciseForm) -> Unit,
    onSubmit: () -> Unit,
    onCancel: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(IronBackground.copy(alpha = 0.9f))
            .clickable(onClick = onCancel),
    ) {
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                .background(IronSurface)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            SectionLabel(text = if (form.id == null) "novo exercício" else "editar exercício")
            Text(
                text = form.name.ifBlank { "Sem nome" },
                style = MaterialTheme.typography.titleLarge,
            )

            IronTextField(
                value = form.name,
                onValueChange = { value -> onChange { it.copy(name = value) } },
                label = "Nome",
            )
            IronTextField(
                value = form.muscleGroup,
                onValueChange = { value -> onChange { it.copy(muscleGroup = value) } },
                label = "Grupo muscular",
            )

            if (muscleGroups.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    items(muscleGroups) { group ->
                        Chip(
                            text = group,
                            selected = form.muscleGroup == group,
                            onClick = { onChange { it.copy(muscleGroup = group) } },
                        )
                    }
                }
            }

            if (form.id != null) {
                SectionLabel(text = "músculos trabalhados")
                MuscleActivationSection(
                    muscles = muscles,
                    unmappedReason = unmappedReason,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            IronTextField(
                value = form.equipment,
                onValueChange = { value -> onChange { it.copy(equipment = value) } },
                label = "Equipamento",
            )

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Chip(
                    text = "Séries e reps",
                    selected = form.executionType == "sets",
                    onClick = { onChange { it.copy(executionType = "sets") } },
                )
                Chip(
                    text = "Por tempo",
                    selected = form.executionType == "time",
                    onClick = { onChange { it.copy(executionType = "time") } },
                )
            }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Usa carga",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = form.usesWeight,
                    onCheckedChange = { value -> onChange { it.copy(usesWeight = value) } },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = IronBackground,
                        checkedTrackColor = IronPrimary,
                        uncheckedTrackColor = IronSurfaceHigh,
                    ),
                )
            }

            if (form.usesWeight) {
                WeightOptionsSection(
                    options = form.weightOptions,
                    onChange = { options -> onChange { it.copy(weightOptions = options) } },
                )
            }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Descanso padrão",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                GhostButton(text = "−15s", onClick = {
                    onChange { it.copy(restSeconds = (it.restSeconds - 15).coerceAtLeast(15)) }
                })
                Spacer(Modifier.padding(horizontal = Spacing.xs))
                Text(
                    text = "${form.restSeconds}s",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.padding(horizontal = Spacing.xs))
                GhostButton(text = "+15s", onClick = {
                    onChange { it.copy(restSeconds = (it.restSeconds + 15).coerceAtMost(600)) }
                })
            }

            IronTextField(
                value = form.notes,
                onValueChange = { value -> onChange { it.copy(notes = value) } },
                label = "Observações",
            )

            PrimaryButton(
                text = if (form.id == null) "Criar exercício" else "Salvar alterações",
                onClick = onSubmit,
                enabled = form.isValid,
                loading = saving,
                modifier = Modifier.fillMaxWidth(),
            )
            GhostButton(text = "Cancelar", onClick = onCancel, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(Spacing.lg))
        }
    }
}

@Composable
private fun WeightOptionsSection(options: List<Double>, onChange: (List<Double>) -> Unit) {
    var single by remember { mutableStateOf("") }
    var from by remember { mutableStateOf("") }
    var to by remember { mutableStateOf("") }
    var step by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        SectionLabel(text = "cargas disponíveis")
        Text(
            text = if (options.isEmpty()) {
                "Sem cargas cadastradas o +/- anda de 1 em 1 kg."
            } else {
                "Toque numa carga para removê-la."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = IronTextMuted,
        )

        if (options.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                items(options) { weight ->
                    Chip(
                        text = Format.weight(weight),
                        selected = false,
                        onClick = { onChange(options.filterNot { it == weight }) },
                    )
                }
            }
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IronTextField(
                value = single,
                onValueChange = { single = it },
                label = "Carga (kg)",
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier.weight(1f),
            )
            GhostButton(text = "Adicionar", onClick = {
                parseWeight(single)?.let { weight ->
                    onChange(WeightLadder.sanitize(options + weight))
                    single = ""
                }
            })
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IronTextField(
                value = from,
                onValueChange = { from = it },
                label = "De",
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier.weight(1f),
            )
            IronTextField(
                value = to,
                onValueChange = { to = it },
                label = "Até",
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier.weight(1f),
            )
            IronTextField(
                value = step,
                onValueChange = { step = it },
                label = "Passo",
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier.weight(1f),
            )
        }

        GhostButton(
            text = "Gerar faixa",
            onClick = {
                val generated = generateRange(parseWeight(from), parseWeight(to), parseWeight(step))

                if (generated.isNotEmpty()) {
                    onChange(WeightLadder.sanitize(options + generated))
                    from = ""
                    to = ""
                    step = ""
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun parseWeight(raw: String): Double? = raw.trim().replace(',', '.').toDoubleOrNull()

private fun generateRange(from: Double?, to: Double?, step: Double?): List<Double> {
    if (from == null || to == null || step == null) return emptyList()
    if (step <= 0.0 || to < from) return emptyList()
    if ((to - from) / step > MAX_GENERATED_WEIGHTS) return emptyList()

    return generateSequence(from) { it + step }.takeWhile { it <= to + 1e-6 }.toList()
}

private const val MAX_GENERATED_WEIGHTS = 200
