package com.archieapps.exercises.ui.feature.anatomy

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.archieapps.exercises.R
import com.archieapps.exercises.core.format.Format
import com.archieapps.exercises.core.net.dto.MuscleHighlightDto
import com.archieapps.exercises.core.net.dto.MuscleVolumeDto
import com.archieapps.exercises.ui.component.CircleIconButton
import com.archieapps.exercises.ui.component.CardHeader
import com.archieapps.exercises.ui.component.EmptyState
import com.archieapps.exercises.ui.component.IronCard
import com.archieapps.exercises.ui.component.KeyValueRow
import com.archieapps.exercises.ui.component.LoadingSkeleton
import com.archieapps.exercises.ui.component.NoticeBanner
import com.archieapps.exercises.ui.component.PillTag
import com.archieapps.exercises.ui.component.SectionLabel
import com.archieapps.exercises.ui.component.ShareBars
import com.archieapps.exercises.ui.component.StatTile
import com.archieapps.exercises.ui.design.IronNegative
import com.archieapps.exercises.ui.design.IronPositive
import com.archieapps.exercises.ui.design.IronRecord
import com.archieapps.exercises.ui.design.IronText
import com.archieapps.exercises.ui.design.IronTextMuted
import com.archieapps.exercises.ui.design.IronTextSecondary
import com.archieapps.exercises.ui.design.Spacing
import com.archieapps.exercises.ui.design.muscleColorFor
import com.archieapps.exercises.core.anatomy.BodyView
import com.archieapps.exercises.ui.component.BodyMap
import com.archieapps.exercises.ui.component.BodyMapLegend
import com.archieapps.exercises.ui.component.BodyViewToggle
import com.archieapps.exercises.ui.component.HairLine
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import com.archieapps.exercises.core.net.dto.MuscleRecommendationDto
import com.archieapps.exercises.ui.component.GhostButton
import com.archieapps.exercises.ui.design.IronBackground
import com.archieapps.exercises.ui.design.IronSurface

@Composable
fun AnatomyScreen(
    state: AnatomyUiState,
    onBack: () -> Unit,
    onSelectRegion: (String?) -> Unit,
    onSelectView: (BodyView) -> Unit,
    onSelectRegionId: (String?) -> Unit,
    onOpenSuggestions: (String?) -> Unit,
    onAdopt: (String) -> Unit,
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
                Column(
                    Modifier
                        .weight(1f)
                        .padding(start = Spacing.md)
                ) {
                    SectionLabel(
                        text = if (state.range.weeks > 0) "últimas ${state.range.weeks} semanas" else "anatomia"
                    )
                    Text(text = "Músculos", style = MaterialTheme.typography.headlineMedium)
                }
            }
        }

        state.message?.let { message ->
            item {
                NoticeBanner(
                    message = message,
                    isError = true,
                    modifier = Modifier.padding(horizontal = Spacing.lg),
                    actionText = "ok",
                    onAction = onDismissMessage,
                )
            }
        }

        if (state.loading && state.isEmpty) {
            item { LoadingSkeleton(Modifier.padding(horizontal = Spacing.lg)) }
        }

        if (!state.loading && state.isEmpty) {
            item {
                EmptyState(
                    title = "Sem dados anatômicos",
                    message = "Registre treinos com exercícios mapeados para ver o volume por músculo.",
                    modifier = Modifier.padding(horizontal = Spacing.lg),
                )
            }
        }

        if (state.regions.isNotEmpty()) {
            item { RegionFilter(state, onSelectRegion) }
        }

        if (state.visibleMuscles.isNotEmpty()) {
            item { MapCard(state, onSelectView, onSelectRegionId) }
            item { VolumeCard(state) }
            item { AttentionCard(state) }
            item { NeglectedMusclesCard(state, onOpenSuggestions) }
            item { MuscleListCard(state) }
            item { AttributionCard(state) }
        }
    }

        state.openedSuggestions?.let { recommendation ->
            SuggestionsOverlay(
                recommendation = recommendation,
                adopting = state.adopting,
                onAdopt = onAdopt,
                onDismiss = { onOpenSuggestions(null) },
            )
        }
    }
}

@Composable
private fun RegionFilter(state: AnatomyUiState, onSelectRegion: (String?) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        contentPadding = PaddingValues(horizontal = Spacing.lg),
    ) {
        items(state.regions, key = { it.region }) { region ->
            RegionChip(
                label = region.label,
                caption = "${region.sets} sér",
                selected = state.selectedRegion == region.region,
                onClick = { onSelectRegion(region.region) },
            )
        }
    }
}

@Composable
private fun RegionChip(label: String, caption: String, selected: Boolean, onClick: () -> Unit) {
    PillTag(
        text = "$label · $caption",
        modifier = Modifier.clickable(onClick = onClick),
        color = if (selected) com.archieapps.exercises.ui.design.IronTextPrimary else IronTextSecondary,
        background = if (selected) {
            com.archieapps.exercises.ui.design.IronPrimary.copy(alpha = 0.18f)
        } else {
            com.archieapps.exercises.ui.design.IronSurfaceHigh
        },
    )
}

@Composable
private fun MapCard(
    state: AnatomyUiState,
    onSelectView: (BodyView) -> Unit,
    onSelectRegionId: (String?) -> Unit,
) {
    val values = state.regionValues

    IronCard(Modifier.padding(horizontal = Spacing.lg)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                CardHeader(title = "Mapa anatômico", caption = "quanto mais claro, mais estímulo")
            }
            BodyViewToggle(view = state.view, onChange = onSelectView)
        }

        Spacer(Modifier.height(Spacing.md))

        // Altura manda, largura sai da proporcao: a figura e alta e estreita (~0.3), entao
        // deixar a largura mandar pediria mais de mil dp de altura.
        BodyMap(
            geometry = state.geometry,
            values = values,
            view = state.view,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .height(360.dp),
            selectedRegionId = state.selectedRegionId,
            interactive = true,
            onRegionTap = { onSelectRegionId(it?.regionId) },
            contentDescription = mapDescription(state),
        )

        if (state.geometry?.hitTestingAvailable == false) {
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = "Toque no mapa indisponível neste aparelho. Use a lista abaixo.",
                style = MaterialTheme.typography.bodyMedium,
                color = IronTextMuted,
            )
        }

        Spacer(Modifier.height(Spacing.md))
        BodyMapLegend(
            values = values,
            onSelect = { onSelectRegionId(it.regionId) },
            selectedRegionId = state.selectedRegionId,
            modifier = Modifier.fillMaxWidth(),
        )

        state.selectedMuscle?.let { muscle ->
            Spacer(Modifier.height(Spacing.md))
            HairLine()
            Spacer(Modifier.height(Spacing.md))
            SelectedMuscle(muscle)
        }
    }
}

private fun mapDescription(state: AnatomyUiState): String {
    val view = if (state.view == BodyView.FRONT) "frontal" else "posterior"
    val top = state.regionValues
        .filter { it.intensity > 0f }
        .distinctBy { it.muscleName }
        .sortedByDescending { it.intensity }
        .take(3)
        .joinToString(", ") { it.muscleName }

    return if (top.isBlank()) {
        "Mapa anatômico, vista $view. Nenhum músculo com volume no período."
    } else {
        "Mapa anatômico, vista $view. Maior estímulo: $top."
    }
}

@Composable
private fun SelectedMuscle(muscle: MuscleVolumeDto) {
    Column {
        SectionLabel(text = statusLabel(muscle.relativeStatus))
        Text(text = muscle.name, style = MaterialTheme.typography.titleMedium)
        muscle.anatomicalName?.let {
            Text(text = it, style = MaterialTheme.typography.bodyMedium, color = IronTextMuted)
        }

        Spacer(Modifier.height(Spacing.md))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            StatTile(
                label = "séries/semana",
                value = trimZero(muscle.setsPerWeek),
                modifier = Modifier.weight(1f),
            )
            StatTile(
                label = "séries",
                value = "${muscle.sets}",
                caption = "${muscle.sessions} sessões",
                modifier = Modifier.weight(1f),
            )
            StatTile(
                label = "volume",
                value = Format.volume(muscle.volumeKg),
                modifier = Modifier.weight(1f),
            )
        }

        muscle.coverageVsRegion?.let { coverage ->
            Spacer(Modifier.height(Spacing.sm))
            KeyValueRow(
                label = "vs. mediana de ${muscle.regionLabel}",
                value = "${coverage.pct}%",
            )
        }
    }
}

@Composable
private fun VolumeCard(state: AnatomyUiState) {
    val muscles = state.visibleMuscles
    val trained = muscles.count { it.setsPerWeek > 0.0 }

    IronCard(Modifier.padding(horizontal = Spacing.lg)) {
        CardHeader(
            title = "Séries por semana",
            caption = "ponderadas: auxiliar conta metade, estabilizador não conta",
        )
        Spacer(Modifier.height(Spacing.md))

        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            StatTile(
                label = "músculos ativos",
                value = "$trained",
                caption = "de ${muscles.size} no catálogo",
                modifier = Modifier.weight(1f),
            )
            StatTile(
                label = "melhor",
                value = trimZero(muscles.maxOfOrNull { it.setsPerWeek } ?: 0.0),
                unit = "sér/sem",
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(Spacing.lg))
        ShareBars(rows = shareRows(muscles), modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun AttentionCard(state: AnatomyUiState) {
    val groups = listOf(
        Triple("Sem estímulo", state.highlights.untrained, IronNegative),
        Triple("Só como estabilizador", state.highlights.stabilizerOnly, IronRecord),
        Triple("Muito abaixo da região", state.highlights.undertrained, IronRecord),
        Triple("Em ênfase", state.highlights.emphasized, IronPositive),
    ).filter { it.second.isNotEmpty() }

    if (groups.isEmpty()) return

    IronCard(Modifier.padding(horizontal = Spacing.lg)) {
        CardHeader(title = "Pontos de atenção", caption = "comparado aos músculos da mesma região")

        groups.forEach { (label, items, color) ->
            Spacer(Modifier.height(Spacing.md))
            SectionLabel(text = label)
            items.forEach { highlight -> HighlightRow(highlight) }
        }
    }
}

@Composable
private fun HighlightRow(highlight: MuscleHighlightDto) {
    Column(Modifier.padding(top = Spacing.sm)) {
        Text(
            text = highlight.message.ifBlank { highlight.name },
            style = MaterialTheme.typography.bodyMedium,
            color = IronTextSecondary,
        )
    }
}

@Composable
private fun NeglectedMusclesCard(state: AnatomyUiState, onOpenSuggestions: (String?) -> Unit) {
    val actionable = state.recommendations.filter { it.hasSuggestions }

    if (actionable.isEmpty()) return

    IronCard(Modifier.padding(horizontal = Spacing.lg)) {
        CardHeader(
            title = "O que treinar",
            caption = "músculos sem estímulo direto no período",
        )

        actionable.forEach { recommendation ->
            Spacer(Modifier.height(Spacing.md))

            Column {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = recommendation.muscle.name,
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            text = recommendation.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = IronTextMuted,
                        )
                    }
                    PillTag(text = statusLabel(recommendation.relativeStatus))
                }

                Spacer(Modifier.height(Spacing.sm))
                GhostButton(
                    text = if (state.openedRecommendation == recommendation.muscle.slug) {
                        "Esconder exercícios"
                    } else {
                        "Ver exercícios"
                    },
                    onClick = { onOpenSuggestions(recommendation.muscle.slug) },
                )
            }
        }
    }
}

@Composable
private fun SuggestionsOverlay(
    recommendation: MuscleRecommendationDto,
    adopting: String?,
    onAdopt: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(IronBackground.copy(alpha = 0.9f))
            .clickable(onClick = onDismiss),
    ) {
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                .background(IronSurface)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            SectionLabel(text = statusLabel(recommendation.relativeStatus))
            Text(text = recommendation.muscle.name, style = MaterialTheme.typography.titleLarge)
            Text(
                text = recommendation.message,
                style = MaterialTheme.typography.bodyMedium,
                color = IronTextMuted,
            )

            if (recommendation.fromYourHistory.isNotEmpty()) {
                SectionLabel(text = "do seu catálogo")
                recommendation.fromYourHistory.forEach { exercise ->
                    KeyValueRow(
                        label = exercise.name,
                        value = exercise.daysSince?.let { "há $it dias" } ?: "nunca feito",
                        trailing = {
                            Text(
                                text = "${exercise.sessionsCount} sessões",
                                style = MaterialTheme.typography.labelSmall,
                                color = IronTextMuted,
                            )
                        },
                    )
                }
            }

            if (recommendation.newOptions.isNotEmpty()) {
                SectionLabel(text = "fora do seu catálogo")
                recommendation.newOptions.forEach { exercise ->
                    Column {
                        KeyValueRow(
                            label = exercise.name,
                            value = exercise.equipment ?: "-",
                        )
                        GhostButton(
                            text = if (adopting == exercise.name) "Adicionando..." else "Adicionar ao catálogo",
                            onClick = { onAdopt(exercise.name) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            GhostButton(text = "Fechar", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun MuscleListCard(state: AnatomyUiState) {
    IronCard(Modifier.padding(horizontal = Spacing.lg)) {
        CardHeader(title = "Todos os músculos", caption = "séries ponderadas por semana")
        Spacer(Modifier.height(Spacing.sm))

        state.visibleMuscles
            .sortedWith(compareByDescending<MuscleVolumeDto> { it.setsPerWeek }.thenBy { it.name })
            .forEach { muscle -> MuscleRow(muscle) }
    }
}

@Composable
private fun MuscleRow(muscle: MuscleVolumeDto) {
    KeyValueRow(
        label = muscle.name,
        value = "${trimZero(muscle.setsPerWeek)} sér/sem",
        trailing = {
            Text(
                text = statusLabel(muscle.relativeStatus),
                style = MaterialTheme.typography.labelSmall,
                color = muscleColorFor(muscle.slug),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
    )
}

@Composable
private fun AttributionCard(state: AnatomyUiState) {
    val attribution = state.attribution
    val unmapped = attribution.unmapped

    IronCard(Modifier.padding(horizontal = Spacing.lg)) {
        CardHeader(title = "Como esta conta é feita", caption = "para os números não enganarem")
        Spacer(Modifier.height(Spacing.sm))

        Text(
            text = "Um exercício conta para todos os músculos que recruta, então a soma por " +
                "músculo é maior que o volume da sessão. É proposital: mede carga recebida " +
                "por músculo, não divisão do treino.",
            style = MaterialTheme.typography.bodyMedium,
            color = IronTextMuted,
        )

        Spacer(Modifier.height(Spacing.md))
        KeyValueRow(label = "Volume das sessões", value = Format.volume(attribution.sessionVolumeKg))
        KeyValueRow(label = "Somado por músculo", value = Format.volume(attribution.attributedVolumeKg))
        attribution.attributionFactor?.let {
            KeyValueRow(label = "Fator de atribuição", value = "${trimZero(it)}×")
        }

        if (unmapped.sets > 0) {
            Spacer(Modifier.height(Spacing.md))
            Text(
                text = "${unmapped.sets} séries ficaram fora da conta por falta de mapeamento: " +
                    unmapped.exerciseNames.joinToString(", "),
                style = MaterialTheme.typography.bodyMedium,
                color = IronRecord,
            )
        }
    }
}
