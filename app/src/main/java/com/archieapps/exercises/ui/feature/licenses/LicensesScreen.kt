package com.archieapps.exercises.ui.feature.licenses

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.archieapps.exercises.R
import com.archieapps.exercises.ui.component.CardHeader
import com.archieapps.exercises.ui.component.CircleIconButton
import com.archieapps.exercises.ui.component.IronCard
import com.archieapps.exercises.ui.component.SectionLabel
import com.archieapps.exercises.ui.design.IronText
import com.archieapps.exercises.ui.design.IronTextMuted
import com.archieapps.exercises.ui.design.IronTextSecondary
import com.archieapps.exercises.ui.design.Spacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val NOTICE_ASSET = "anatomy/LICENSE-body-muscles.txt"

@Composable
fun LicensesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val context = LocalContext.current
    var notice by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        notice = withContext(Dispatchers.Default) {
            runCatching {
                context.assets.open(NOTICE_ASSET).bufferedReader().use { it.readText() }
            }.getOrNull()
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        item {
            Row(
                Modifier
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
                Column(Modifier.padding(start = Spacing.md)) {
                    SectionLabel(text = "código de terceiros")
                    Text(text = "Licenças", style = MaterialTheme.typography.headlineMedium)
                }
            }
        }

        item {
            IronCard(Modifier.padding(horizontal = Spacing.lg)) {
                CardHeader(title = "Silhueta anatômica", caption = "vulovix/body-muscles · Apache-2.0")
                Spacer(Modifier.height(Spacing.md))
                Text(
                    text = notice ?: "Carregando aviso de licença...",
                    style = IronText.tabular,
                    color = IronTextMuted,
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                )
            }
        }
    }
}
