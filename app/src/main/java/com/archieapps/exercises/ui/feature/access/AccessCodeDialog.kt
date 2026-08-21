package com.archieapps.exercises.ui.feature.access

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.archieapps.exercises.core.access.AccessCode
import com.archieapps.exercises.ui.design.IronTextSecondary

@Composable
fun AccessCodeDialog(
    submitting: Boolean,
    error: String?,
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val pairs = remember { AccessCode.pairs() }
    var code by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Código de acesso") },
        text = {
            Column {
                Text("•".repeat(code.length / 2).ifEmpty { "————" }, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))

                pairs.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        row.forEach { (first, second) ->
                            OutlinedButton(
                                onClick = { code += "$first$second" },
                                enabled = code.length < AccessCode.LENGTH,
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("$first ou $second")
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                if (error != null) {
                    Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSubmit(code) },
                enabled = code.length == AccessCode.LENGTH && !submitting,
            ) {
                Text(if (submitting) "Validando…" else "Liberar")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { code = "" }) { Text("Apagar") }
                TextButton(onClick = onDismiss) { Text("Depois") }
            }
        },
    )
}
