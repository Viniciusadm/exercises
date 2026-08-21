package com.archieapps.exercises.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.archieapps.exercises.core.format.Format
import com.archieapps.exercises.core.session.WeightLadder
import com.archieapps.exercises.ui.design.IronBackground
import com.archieapps.exercises.ui.design.IronBorder
import com.archieapps.exercises.ui.design.IronPrimary
import com.archieapps.exercises.ui.design.IronSurface
import com.archieapps.exercises.ui.design.IronSurfaceHigh
import com.archieapps.exercises.ui.design.IronTextMuted
import com.archieapps.exercises.ui.design.IronTextPrimary
import com.archieapps.exercises.ui.design.IronTextSecondary
import com.archieapps.exercises.ui.design.Spacing
import com.archieapps.exercises.ui.feature.auth.IronTextField

@Composable
fun WeightPickerDialog(
    current: Double?,
    options: List<Double>,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit,
) {
    var typed by remember { mutableStateOf(current?.let { Format.weightValue(it) } ?: "") }
    val ladder = remember(options) { WeightLadder.sanitize(options) }
    val parsed = typed.replace(',', '.').toDoubleOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = IronSurface,
        titleContentColor = IronTextPrimary,
        textContentColor = IronTextSecondary,
        title = { Text("Carga", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                if (!ladder.isEmpty()) {
                    SectionLabel(text = "cargas disponíveis")
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 64.dp),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        items(ladder) { weight ->
                            WeightOption(
                                weight = weight,
                                selected = parsed != null && weight == parsed,
                                onClick = { typed = Format.weightValue(weight) },
                            )
                        }
                    }
                }

                IronTextField(
                    value = typed,
                    onValueChange = { typed = it },
                    label = "Carga (kg)",
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { parsed?.let { onConfirm(it.coerceIn(WeightLadder.MIN_KG, WeightLadder.MAX_KG)) } },
                enabled = parsed != null,
            ) {
                Text("Usar", color = IronPrimary, style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = IronTextMuted)
            }
        },
    )
}

@Composable
private fun WeightOption(weight: Double, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = Format.weightValue(weight),
        style = MaterialTheme.typography.titleMedium,
        color = if (selected) IronBackground else IronTextPrimary,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) IronPrimary else IronSurfaceHigh)
            .border(1.dp, if (selected) IronPrimary else IronBorder, RoundedCornerShape(12.dp))
            .clickable(onClickLabel = "Usar ${Format.weight(weight)}", onClick = onClick)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
    )
}
