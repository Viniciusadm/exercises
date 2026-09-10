package com.archieapps.exercises.ui.feature.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.archieapps.exercises.core.db.ExerciseEntity
import com.archieapps.exercises.core.format.Format
import com.archieapps.exercises.core.net.dto.TemplateDto
import com.archieapps.exercises.ui.component.CardHeader
import com.archieapps.exercises.ui.component.EmptyState
import com.archieapps.exercises.ui.component.GhostButton
import com.archieapps.exercises.core.net.dto.MuscleDto
import com.archieapps.exercises.ui.component.IronCard
import com.archieapps.exercises.core.session.WeightLadder
import com.archieapps.exercises.ui.component.IronBottomSheet
import com.archieapps.exercises.ui.component.MusclePickerSheet
import com.archieapps.exercises.ui.component.MuscleSelectionSummary
import com.archieapps.exercises.ui.component.SelectChip
import com.archieapps.exercises.ui.component.NoticeBanner
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
    onDeleteExercise: () -> Unit,
    onDeleteTemplate: (TemplateDto) -> Unit,
    onOpenTemplate: (Int?) -> Unit,
    onPlanWorkout: () -> Unit,
    onDismissMessage: () -> Unit,
    onRetryMuscleCatalog: () -> Unit,
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
                    SelectChip(
                        label = "Catálogo",
                        selected = !state.showTemplates,
                        onClick = { onShowTemplates(false) },
                    )
                    SelectChip(
                        label = "Templates (${state.templates.size})",
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
                            SelectChip(
                                label = group,
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
            val mapped = state.muscleMap.of(form.id)
            val muscleNames = remember(state.muscleCatalog, mapped) {
                mapped.all.associate { it.id to it.name } +
                    state.muscleCatalog.associate { it.id to it.name }
            }

            ExerciseFormOverlay(
                form = form,
                saving = state.saving,
                dirty = state.formDirty,
                muscleGroups = state.muscleGroups,
                muscleCatalog = state.muscleCatalog,
                muscleNames = muscleNames,
                unmappedReason = state.muscleMap.reasonFor(form.id),
                onChange = onFormChange,
                onSubmit = onSubmitForm,
                onCancel = onCancelForm,
                onDelete = onDeleteExercise,
                onRetryMuscleCatalog = onRetryMuscleCatalog,
            )
        }
    }
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
    dirty: Boolean,
    muscleGroups: List<String>,
    muscleCatalog: List<MuscleDto>,
    muscleNames: Map<Int, String>,
    unmappedReason: String?,
    onChange: ((ExerciseForm) -> ExerciseForm) -> Unit,
    onSubmit: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    onRetryMuscleCatalog: () -> Unit,
) {
    var confirmDiscard by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var pickingMuscles by remember { mutableStateOf(false) }

    val requestDismiss: () -> Unit = { if (dirty) confirmDiscard = true else onCancel() }

    IronBottomSheet(
        onDismissRequest = requestDismiss,
        onBack = requestDismiss,
        header = {
            SectionLabel(text = if (form.id == null) "novo exercício" else "editar exercício")
            Text(
                text = form.name.ifBlank { "Sem nome" },
                style = MaterialTheme.typography.titleLarge,
            )
        },
        footer = {
            PrimaryButton(
                text = if (form.id == null) "Criar exercício" else "Salvar alterações",
                onClick = onSubmit,
                enabled = form.isValid,
                loading = saving,
                modifier = Modifier.fillMaxWidth(),
            )
            GhostButton(
                text = "Cancelar",
                onClick = requestDismiss,
                modifier = Modifier.fillMaxWidth(),
            )
            if (form.id != null) {
                TextButton(
                    onClick = { confirmDelete = true },
                    enabled = !saving,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Excluir exercício", color = MaterialTheme.colorScheme.error)
                }
            }
        },
    ) {
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
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                muscleGroups.forEach { group ->
                    SelectChip(
                        label = group,
                        selected = form.muscleGroup == group,
                        onClick = { onChange { it.copy(muscleGroup = group) } },
                    )
                }
            }
        }

        SectionLabel(text = "músculos trabalhados")
        MuscleSelectionSummary(
            selection = form.muscles,
            names = muscleNames,
            emptyMessage = unmappedReason ?: "Nenhum músculo escolhido ainda.",
            modifier = Modifier.fillMaxWidth(),
        )
        GhostButton(
            text = if (form.muscles.isEmpty) {
                "Escolher músculos"
            } else {
                "Editar músculos (${form.muscles.total})"
            },
            onClick = { pickingMuscles = true },
            modifier = Modifier.fillMaxWidth(),
        )

        IronTextField(
            value = form.equipment,
            onValueChange = { value -> onChange { it.copy(equipment = value) } },
            label = "Equipamento",
        )

        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            SelectChip(
                label = "Séries e reps",
                selected = form.executionType == "sets",
                onClick = { onChange { it.copy(executionType = "sets") } },
            )
            SelectChip(
                label = "Por tempo",
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

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Registra distância",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = form.tracksDistance,
                onCheckedChange = { value -> onChange { it.copy(tracksDistance = value) } },
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
            GhostButton(text = "\u221215s", onClick = {
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
    }

    if (pickingMuscles) {
        MusclePickerSheet(
            catalog = muscleCatalog,
            selection = form.muscles,
            onSelectionChange = { selection -> onChange { it.copy(muscles = selection) } },
            onClose = { pickingMuscles = false },
            onRetryCatalog = onRetryMuscleCatalog,
        )
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            containerColor = IronSurface,
            titleContentColor = IronTextPrimary,
            textContentColor = IronTextSecondary,
            title = { Text("Descartar alterações?", style = MaterialTheme.typography.titleLarge) },
            text = { Text("O que você mudou neste exercício será perdido.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDiscard = false
                    onCancel()
                }) {
                    Text("Descartar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDiscard = false }) {
                    Text("Continuar editando", color = IronTextMuted)
                }
            },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = IronSurface,
            titleContentColor = IronTextPrimary,
            textContentColor = IronTextSecondary,
            title = { Text("Excluir exercício?", style = MaterialTheme.typography.titleLarge) },
            text = {
                Text(
                    "\"${form.name.trim()}\" sai do catálogo, dos templates e do mapeamento de músculos. " +
                        "Os treinos já feitos continuam no histórico."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) {
                    Text("Excluir", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text("Manter", color = IronTextMuted)
                }
            },
        )
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

        if (options.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                items(options) { weight ->
                    SelectChip(
                        label = Format.weight(weight),
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
