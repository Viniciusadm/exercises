package com.archieapps.exercises.ui.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.archieapps.exercises.BuildConfig
import com.archieapps.exercises.ui.component.CardHeader
import com.archieapps.exercises.ui.component.GhostButton
import com.archieapps.exercises.ui.component.IronCard
import com.archieapps.exercises.ui.component.NoticeBanner
import com.archieapps.exercises.ui.component.PrimaryButton
import com.archieapps.exercises.ui.component.SectionLabel
import com.archieapps.exercises.ui.design.IronBackground
import com.archieapps.exercises.ui.design.IronPrimary
import com.archieapps.exercises.ui.design.IronSurfaceHigh
import com.archieapps.exercises.ui.design.IronTextMuted
import com.archieapps.exercises.ui.design.IronTextSecondary
import com.archieapps.exercises.ui.design.Spacing
import com.archieapps.exercises.ui.feature.auth.IronTextField

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onSyncCatalog: () -> Unit,
    onFlushPending: () -> Unit,
    onRestChange: (Int) -> Unit,
    onKeepScreenOnChange: (Boolean) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(horizontal = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Spacer(Modifier.height(Spacing.sm))
        SectionLabel(text = "ajustes")
        Text(text = "Configuração", style = MaterialTheme.typography.headlineMedium)

        if (state.message != null) {
            NoticeBanner(message = state.message, isError = state.isError)
        }

        IronCard {
            CardHeader(title = "Servidor", caption = "definido em env.properties, fora do git")
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = BuildConfig.BASE_URL,
                style = MaterialTheme.typography.bodyLarge,
                color = IronTextSecondary,
            )
        }

        IronCard {
            CardHeader(
                title = "Catálogo de exercícios",
                caption = "${state.catalogSize} exercícios no cache local",
            )
            Spacer(Modifier.height(Spacing.md))
            PrimaryButton(
                text = "Sincronizar catálogo",
                onClick = onSyncCatalog,
                loading = state.syncing,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        IronCard {
            CardHeader(
                title = "Treinos aguardando envio",
                caption = if (state.pendingWorkouts == 0) {
                    "nada na fila — tudo sincronizado"
                } else {
                    "${state.pendingWorkouts} sessão(ões) salvas no aparelho"
                },
            )
            Spacer(Modifier.height(Spacing.md))
            GhostButton(
                text = "Enviar agora",
                onClick = onFlushPending,
                enabled = state.pendingWorkouts > 0 && !state.syncing,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        IronCard {
            CardHeader(title = "Treino", caption = "padrões usados na sessão ao vivo")
            Spacer(Modifier.height(Spacing.md))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Descanso padrão",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Stepper(
                    value = state.defaultRestSeconds,
                    onChange = onRestChange,
                    step = 15,
                    range = 15..600,
                    format = { seconds ->
                        if (seconds >= 60) {
                            "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
                        } else {
                            "${seconds}s"
                        }
                    },
                )
            }
            Spacer(Modifier.height(Spacing.sm))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(text = "Manter a tela ligada", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = "durante a sessão de treino",
                        style = MaterialTheme.typography.labelMedium,
                        color = IronTextMuted,
                    )
                }
                Switch(
                    checked = state.keepScreenOn,
                    onCheckedChange = onKeepScreenOnChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = IronBackground,
                        checkedTrackColor = IronPrimary,
                        uncheckedTrackColor = IronSurfaceHigh,
                    ),
                )
            }
        }

        IronCard {
            CardHeader(
                title = state.userName ?: "Sessão",
                caption = "token guardado neste aparelho",
            )
            Spacer(Modifier.height(Spacing.md))
            GhostButton(text = "Sair da conta", onClick = onLogout, modifier = Modifier.fillMaxWidth())
        }

        Text(
            text = "Treinos · fase 2",
            style = MaterialTheme.typography.labelMedium,
            color = IronTextSecondary,
            modifier = Modifier.padding(vertical = Spacing.lg),
        )
    }
}

@Composable
private fun Stepper(
    value: Int,
    onChange: (Int) -> Unit,
    step: Int,
    range: IntRange,
    format: (Int) -> String,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        GhostButton(text = "−", onClick = { onChange((value - step).coerceIn(range)) })
        Text(text = format(value), style = MaterialTheme.typography.titleMedium)
        GhostButton(text = "+", onClick = { onChange((value + step).coerceIn(range)) })
    }
}
