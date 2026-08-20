package com.archieapps.exercises.ui.component

import androidx.compose.foundation.background
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
import com.archieapps.exercises.core.db.ExerciseEntity
import com.archieapps.exercises.core.format.Format
import com.archieapps.exercises.ui.design.IronBackground
import com.archieapps.exercises.ui.design.IronSurface
import com.archieapps.exercises.ui.design.IronTextMuted
import com.archieapps.exercises.ui.design.Spacing
import com.archieapps.exercises.ui.feature.auth.IronTextField

@Composable
fun ExercisePickerOverlay(
    catalog: List<ExerciseEntity>,
    onPick: (ExerciseEntity) -> Unit,
    onClose: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val results = remember(query, catalog) {
        if (query.isBlank()) {
            catalog
        } else {
            catalog.filter { exercise ->
                exercise.name.contains(query, ignoreCase = true) ||
                    exercise.muscleGroup?.contains(query, ignoreCase = true) == true ||
                    exercise.equipment?.contains(query, ignoreCase = true) == true
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(IronBackground.copy(alpha = 0.88f))
            .clickable(onClick = onClose),
    ) {
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.86f)
                .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                .background(IronSurface)
                .imePadding()
                .padding(Spacing.lg),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    SectionLabel(text = "catálogo")
                    Text(text = "Adicionar exercício", style = MaterialTheme.typography.titleLarge)
                }
                GhostButton(text = "Fechar", onClick = onClose)
            }

            Spacer(Modifier.height(Spacing.md))

            IronTextField(value = query, onValueChange = { query = it }, label = "Buscar")

            Spacer(Modifier.height(Spacing.md))

            if (catalog.isEmpty()) {
                Text(
                    text = "Catálogo vazio no aparelho. Sincronize nos ajustes para escolher exercícios.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = IronTextMuted,
                )
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                items(results, key = { it.id }) { exercise ->
                    IronCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onPick(exercise) },
                        contentPadding = PaddingValues(Spacing.md),
                    ) {
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
                                        exercise.bestWeightKg?.let { "melhor ${Format.weight(it)}" },
                                    ).joinToString(" · "),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = IronTextMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            PillTag(text = "${exercise.sessionsCount}×")
                        }
                    }
                }
            }
        }
    }
}
