package com.archieapps.exercises.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.archieapps.exercises.core.net.dto.MuscleDto
import com.archieapps.exercises.ui.design.IronPrimary
import com.archieapps.exercises.ui.design.IronTextMuted
import com.archieapps.exercises.ui.design.IronTextSecondary
import com.archieapps.exercises.ui.design.Spacing
import com.archieapps.exercises.ui.feature.auth.IronTextField
import java.text.Normalizer

@Composable
fun MusclePickerSheet(
    catalog: List<MuscleDto>,
    selection: MuscleSelection,
    onSelectionChange: (MuscleSelection) -> Unit,
    onClose: () -> Unit,
    onRetryCatalog: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val overrides = remember { mutableStateMapOf<String, Boolean>() }

    val byId = remember(catalog) { catalog.associateBy { it.id } }
    val filtered = remember(catalog, query) {
        val needle = query.foldForSearch()

        if (needle.isBlank()) {
            catalog
        } else {
            catalog.filter { muscle ->
                muscle.name.foldForSearch().contains(needle) ||
                    muscle.anatomicalName?.foldForSearch()?.contains(needle) == true ||
                    muscle.regionLabel?.foldForSearch()?.contains(needle) == true
            }
        }
    }
    val regions = remember(filtered) { filtered.groupBy { it.region } }

    IronBottomSheet(
        onDismissRequest = onClose,
        onBack = onClose,
        bodyScrollable = false,
        header = {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    SectionLabel(text = "anatomia")
                    Text(text = "Músculos trabalhados", style = MaterialTheme.typography.titleLarge)
                }
                GhostButton(text = "Fechar", onClick = onClose)
            }
        },
        footer = {
            PrimaryButton(
                text = "Concluir",
                onClick = onClose,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        IronTextField(value = query, onValueChange = { query = it }, label = "Buscar músculo")

        Text(
            text = "Principal faz o movimento · Auxiliar ajuda · Estabilizador segura a postura.",
            style = MaterialTheme.typography.bodySmall,
            color = IronTextMuted,
        )

        if (catalog.isEmpty()) {
            Text(
                text = "O catálogo de músculos ainda não foi baixado neste aparelho.",
                style = MaterialTheme.typography.bodyMedium,
                color = IronTextMuted,
            )
            GhostButton(
                text = "Tentar de novo",
                onClick = onRetryCatalog,
                modifier = Modifier.fillMaxWidth(),
            )

            return@IronBottomSheet
        }

        LazyColumn(Modifier.weight(1f)) {
            if (!selection.isEmpty) {
                item(key = "selected") {
                    Column(
                        Modifier.padding(bottom = Spacing.md),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        SectionLabel(text = "escolhidos (${selection.total})")
                        Text(
                            text = "Toque para remover.",
                            style = MaterialTheme.typography.bodySmall,
                            color = IronTextMuted,
                        )
                        MuscleActivation.entries.forEach { activation ->
                            val ids = selection.of(activation)
                            if (ids.isEmpty()) return@forEach

                            SectionLabel(text = activation.label)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                            ) {
                                ids.forEach { id ->
                                    MuscleChip(
                                        name = byId[id]?.name ?: "Músculo $id",
                                        activation = activation,
                                        modifier = Modifier.clickable {
                                            onSelectionChange(selection.with(id, null))
                                        },
                                    )
                                }
                            }
                        }
                        HairLine()
                    }
                }
            }

            regions.forEach { (region, muscles) ->
                val label = muscles.firstOrNull()?.regionLabel ?: region
                val chosen = muscles.count { selection.activationOf(it.id) != null }
                val open = overrides[region] ?: (query.isNotBlank() || chosen > 0)

                item(key = "region:$region") {
                    RegionHeader(
                        label = label,
                        chosen = chosen,
                        total = muscles.size,
                        expanded = open,
                        onToggle = { overrides[region] = !open },
                    )
                }

                if (!open) return@forEach

                items(muscles, key = { "muscle:${it.id}" }) { muscle ->
                    MusclePickerRow(
                        muscle = muscle,
                        parentName = muscle.parentId?.let { byId[it]?.name },
                        activation = selection.activationOf(muscle.id),
                        onActivation = { activation ->
                            onSelectionChange(selection.with(muscle.id, activation))
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun RegionHeader(
    label: String,
    chosen: Int,
    total: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = if (chosen > 0) "$chosen de $total" else "$total",
            style = MaterialTheme.typography.labelMedium,
            color = if (chosen > 0) IronPrimary else IronTextMuted,
        )
        Spacer(Modifier.width(Spacing.sm))
        Text(
            text = if (expanded) "−" else "+",
            style = MaterialTheme.typography.titleMedium,
            color = IronTextSecondary,
        )
    }
}

@Composable
private fun MusclePickerRow(
    muscle: MuscleDto,
    parentName: String?,
    activation: MuscleActivation?,
    onActivation: (MuscleActivation?) -> Unit,
) {
    val caption = listOfNotNull(
        parentName?.let { "parte de $it" },
        muscle.anatomicalName,
    ).joinToString(" · ")

    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = if (parentName != null) Spacing.md else 0.dp)
            .padding(bottom = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Column {
            Text(text = muscle.name, style = MaterialTheme.typography.bodyLarge)
            if (caption.isNotBlank()) {
                Text(
                    text = caption,
                    style = MaterialTheme.typography.labelMedium,
                    color = IronTextMuted,
                )
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            MuscleActivation.entries.forEach { candidate ->
                SelectChip(
                    label = candidate.shortLabel,
                    selected = activation == candidate,
                    onClick = {
                        onActivation(if (activation == candidate) null else candidate)
                    },
                )
            }
        }

        HairLine()
    }
}

private val Diacritics = "\\p{Mn}+".toRegex()

private fun String.foldForSearch(): String =
    Normalizer.normalize(trim(), Normalizer.Form.NFD).replace(Diacritics, "").lowercase()
