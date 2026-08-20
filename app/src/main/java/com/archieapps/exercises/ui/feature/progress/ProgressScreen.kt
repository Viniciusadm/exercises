package com.archieapps.exercises.ui.feature.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.archieapps.exercises.core.format.Format
import com.archieapps.exercises.core.net.dto.ExerciseHistoryDto
import com.archieapps.exercises.ui.component.CardHeader
import com.archieapps.exercises.ui.component.DeltaBadge
import com.archieapps.exercises.ui.component.EmptyState
import com.archieapps.exercises.ui.component.IronCard
import com.archieapps.exercises.ui.component.KeyValueRow
import com.archieapps.exercises.ui.component.LineChart
import com.archieapps.exercises.ui.component.LoadingSkeleton
import com.archieapps.exercises.ui.component.NoticeBanner
import com.archieapps.exercises.ui.component.SectionLabel
import com.archieapps.exercises.ui.component.StatTile
import com.archieapps.exercises.ui.design.IronPrimary
import com.archieapps.exercises.ui.design.IronRecord
import com.archieapps.exercises.ui.design.IronSurfaceHigh
import com.archieapps.exercises.ui.design.IronTextMuted
import com.archieapps.exercises.ui.design.IronTextPrimary
import com.archieapps.exercises.ui.design.IronTextSecondary
import com.archieapps.exercises.ui.design.Spacing

@Composable
fun ProgressScreen(
    state: ProgressUiState,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        item {
            Column(Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md)) {
                SectionLabel(text = "progresso")
                Text(text = "Evolução", style = MaterialTheme.typography.headlineMedium)
            }
        }

        item {
            LazyRow(
                Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = Spacing.lg),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                items(state.catalog, key = { it.id }) { exercise ->
                    val selected = exercise.id == state.selectedId

                    Text(
                        text = exercise.name,
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
                            .clickable { onSelect(exercise.id) }
                            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                    )
                }
            }
        }

        state.error?.let { error ->
            item { NoticeBanner(message = error, isError = true, modifier = Modifier.padding(horizontal = Spacing.lg)) }
        }

        val history = state.history

        if (history == null) {
            item {
                if (state.loading) {
                    LoadingSkeleton(Modifier.padding(horizontal = Spacing.lg))
                } else {
                    EmptyState(
                        title = "Escolha um exercício",
                        message = "Sincronize o catálogo e selecione um exercício para ver a evolução da carga.",
                    )
                }
            }

            return@LazyColumn
        }

        item { RecordTiles(history, Modifier.padding(horizontal = Spacing.lg)) }
        item { LoadCurveCard(history, Modifier.padding(horizontal = Spacing.lg)) }
        item { SessionsCard(history, Modifier.padding(horizontal = Spacing.lg)) }
    }
}

@Composable
private fun RecordTiles(history: ExerciseHistoryDto, modifier: Modifier = Modifier) {
    val records = history.records

    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
        StatTile(
            label = "melhor carga",
            value = Format.weightValue(records.bestWeight?.value),
            unit = "kg",
            caption = records.bestWeight?.date?.let { Format.parseDate(it)?.let(Format::dayMonth) },
            accent = IronRecord,
            modifier = Modifier.weight(1f),
        )
        StatTile(
            label = "1rm estimado",
            value = Format.weightValue(records.bestEstimated1rm?.value),
            unit = "kg",
            caption = records.bestEstimated1rm?.date?.let { Format.parseDate(it)?.let(Format::dayMonth) },
            modifier = Modifier.weight(1f),
        )
        StatTile(
            label = "sessões",
            value = Format.integer(records.sessionsCount),
            caption = Format.volume(records.totalVolumeKg),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun LoadCurveCard(history: ExerciseHistoryDto, modifier: Modifier = Modifier) {
    val sessions = history.sessions.reversed()
    val weights = sessions.mapNotNull { it.topSet?.weightKg }
    val direction = when (history.trend.direction) {
        "up" -> "subindo"
        "down" -> "caindo"
        else -> "estável"
    }

    IronCard(modifier) {
        CardHeader(
            title = "Carga por sessão",
            caption = "${sessions.size} sessões · tendência $direction",
            trailing = { DeltaBadge(delta = history.trend.deltaKg) },
        )
        Spacer(Modifier.height(Spacing.lg))

        if (weights.size < 2) {
            Text(
                text = "Poucas sessões registradas para desenhar a curva.",
                style = MaterialTheme.typography.bodyMedium,
                color = IronTextMuted,
            )
        } else {
            LineChart(
                values = weights,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
            )
            Spacer(Modifier.height(Spacing.sm))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = sessions.firstOrNull()?.date?.let { Format.parseDate(it)?.let(Format::dayMonth) } ?: "",
                    style = MaterialTheme.typography.labelMedium,
                    color = IronTextMuted,
                )
                Text(
                    text = "mín ${Format.weight(weights.min())} · máx ${Format.weight(weights.max())}",
                    style = MaterialTheme.typography.labelMedium,
                    color = IronTextSecondary,
                )
                Text(
                    text = sessions.lastOrNull()?.date?.let { Format.parseDate(it)?.let(Format::dayMonth) } ?: "",
                    style = MaterialTheme.typography.labelMedium,
                    color = IronTextMuted,
                )
            }
        }
    }
}

@Composable
private fun SessionsCard(history: ExerciseHistoryDto, modifier: Modifier = Modifier) {
    IronCard(modifier) {
        CardHeader(title = "Últimas sessões", caption = history.exercise.name)
        Spacer(Modifier.height(Spacing.sm))

        history.sessions.take(12).forEach { session ->
            KeyValueRow(
                label = session.date?.let { Format.parseDate(it)?.let(Format::dateWithWeekday) } ?: Format.PLACEHOLDER,
                value = buildString {
                    append(session.sets.size)
                    append("× ")
                    append(Format.weight(session.topSet?.weightKg))
                    append(" · ")
                    append(Format.volume(session.totalVolumeKg))
                },
            )
        }
    }
}
