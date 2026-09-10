package com.archieapps.exercises.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.archieapps.exercises.core.format.Format
import com.archieapps.exercises.core.session.DistanceLadder
import com.archieapps.exercises.ui.design.IronPrimary
import com.archieapps.exercises.ui.design.IronSurface
import com.archieapps.exercises.ui.design.IronTextMuted
import com.archieapps.exercises.ui.design.IronTextPrimary
import com.archieapps.exercises.ui.design.IronTextSecondary
import com.archieapps.exercises.ui.design.Spacing
import com.archieapps.exercises.ui.feature.auth.IronTextField

@Composable
fun DistancePickerDialog(
    current: Double?,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit,
) {
    var typed by remember { mutableStateOf(current?.takeIf { it > 0 }?.let { Format.distanceValue(it) } ?: "") }
    val parsed = typed.replace(',', '.').toDoubleOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = IronSurface,
        titleContentColor = IronTextPrimary,
        textContentColor = IronTextSecondary,
        title = { Text("Distância", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                IronTextField(
                    value = typed,
                    onValueChange = { typed = it },
                    label = "Distância (km)",
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { parsed?.let { onConfirm(DistanceLadder.clamp(it)) } },
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
