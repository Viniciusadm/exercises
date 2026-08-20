package com.archieapps.exercises.ui.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.archieapps.exercises.R
import com.archieapps.exercises.core.format.Format
import com.archieapps.exercises.core.format.PtBr
import com.archieapps.exercises.core.net.SafeCode
import com.archieapps.exercises.core.net.dto.CalendarDayDto
import com.archieapps.exercises.core.net.dto.MonthlyDto
import com.archieapps.exercises.core.net.dto.RecordDto
import com.archieapps.exercises.core.net.dto.StatsDto
import com.archieapps.exercises.ui.component.BarChart
import com.archieapps.exercises.ui.component.BarDatum
import com.archieapps.exercises.ui.component.CardHeader
import com.archieapps.exercises.ui.component.CircleIconButton
import com.archieapps.exercises.ui.component.DeltaBadge
import com.archieapps.exercises.ui.component.EmptyState
import com.archieapps.exercises.ui.component.Heatmap
import com.archieapps.exercises.ui.component.HeroSurface
import com.archieapps.exercises.ui.component.IronCard
import com.archieapps.exercises.ui.component.KeyValueRow
import com.archieapps.exercises.ui.component.LoadingSkeleton
import com.archieapps.exercises.ui.component.MetricValue
import com.archieapps.exercises.ui.component.NoticeBanner
import com.archieapps.exercises.ui.component.PillTag
import com.archieapps.exercises.ui.component.PrimaryButton
import com.archieapps.exercises.ui.component.SectionLabel
import com.archieapps.exercises.ui.component.ShareBars
import com.archieapps.exercises.ui.component.ShareRow
import com.archieapps.exercises.ui.component.StatTile
import com.archieapps.exercises.ui.design.IronPositive
import com.archieapps.exercises.ui.design.IronPrimary
import com.archieapps.exercises.ui.design.IronRecord
import com.archieapps.exercises.ui.design.IronText
import com.archieapps.exercises.ui.design.IronTextMuted
import com.archieapps.exercises.ui.design.IronTextSecondary
import com.archieapps.exercises.ui.design.MuscleGroupColors
import com.archieapps.exercises.ui.design.Spacing
import java.time.LocalDate

@Composable
fun HomeScreen(
    state: HomeUiState,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
    onStartWorkout: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val stats = state.cached?.snapshot?.stats
    val calendar = state.cached?.snapshot?.calendar ?: emptyList()

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        item {
            HomeHeader(
                refreshing = state.refreshing,
                onRefresh = onRefresh,
                onOpenSettings = onOpenSettings,
            )
        }

        if (state.error != null) {
            item {
                NoticeBanner(
                    message = if (state.offline && state.cached != null) {
                        "Sem conexão — mostrando os dados sincronizados."
                    } else {
                        state.error
                    },
                    isError = !state.offline,
                    actionText = "Tentar",
                    onAction = onRefresh,
                    modifier = Modifier.padding(horizontal = Spacing.lg),
                )
            }
        }

        if (state.showSkeleton) {
            item { LoadingSkeleton(Modifier.padding(horizontal = Spacing.lg)) }
        }

        if (stats == null) {
            if (state.showEmpty) {
                item {
                    EmptyState(
                        title = "Nada sincronizado ainda",
                        message = "Confira o endereço do servidor nos ajustes e tente sincronizar de novo.",
                        actionText = "Sincronizar",
                        onAction = onRefresh,
                    )
                }
            }

            return@LazyColumn
        }

        item {
            HeroCard(
                stats = stats,
                onStartWorkout = onStartWorkout,
                modifier = Modifier.padding(horizontal = Spacing.lg),
            )
        }

        item { MonthTiles(stats, Modifier.padding(horizontal = Spacing.lg)) }

        if (calendar.isNotEmpty()) {
            item { ConsistencyCard(calendar, Modifier.padding(horizontal = Spacing.lg)) }
        }

        if (stats.monthly.isNotEmpty()) {
            item { VolumeCard(stats.monthly, Modifier.padding(horizontal = Spacing.lg)) }
        }

        if (stats.byMuscleGroup.isNotEmpty()) {
            item { MuscleGroupCard(stats, Modifier.padding(horizontal = Spacing.lg)) }
        }

        if (stats.recentRecords.isNotEmpty()) {
            item { RecordsCard(stats.recentRecords, Modifier.padding(horizontal = Spacing.lg)) }
        }

        if (stats.topExercises.isNotEmpty()) {
            item { TopExercisesCard(stats, Modifier.padding(horizontal = Spacing.lg)) }
        }

        item {
            SyncFooter(
                syncedAt = state.cached?.syncedAt,
                modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm),
            )
        }
    }
}

@Composable
private fun HomeHeader(
    refreshing: Boolean,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val today = LocalDate.now(SafeCode.ServerZone)

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = Format.dateWithWeekday(today).uppercase(PtBr),
                style = MaterialTheme.typography.labelSmall,
                color = IronTextMuted,
            )
            Text(text = "Seus treinos", style = MaterialTheme.typography.headlineMedium)
        }
        CircleIconButton(
            iconRes = R.drawable.ic_refresh,
            contentDescription = "Sincronizar",
            onClick = onRefresh,
            tint = if (refreshing) IronPrimary else IronTextSecondary,
        )
        Spacer(Modifier.size(Spacing.sm))
        CircleIconButton(
            iconRes = R.drawable.ic_sliders,
            contentDescription = "Ajustes",
            onClick = onOpenSettings,
            tint = IronTextSecondary,
        )
    }
}

@Composable
private fun HeroCard(stats: StatsDto, onStartWorkout: () -> Unit, modifier: Modifier = Modifier) {
    val days = stats.daysSinceLast
    val last = stats.lastWorkout

    HeroSurface(modifier) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                SectionLabel(text = "último treino")
                Spacer(Modifier.height(Spacing.sm))
                MetricValue(
                    value = days?.toString() ?: Format.PLACEHOLDER,
                    unit = if (days == 1) "dia atrás" else "dias atrás",
                    style = MaterialTheme.typography.displayLarge,
                    unitColor = IronTextSecondary,
                )
                Text(
                    text = when {
                        days == null -> "Nenhum treino registrado"
                        days == 0 -> "Você treinou hoje. Bom trabalho."
                        days <= 2 -> "Ritmo em dia."
                        days <= 4 -> "Dá pra voltar hoje."
                        else -> "Faz tempo. Hora de voltar."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = IronTextSecondary,
                )
            }
            StreakBadge(weeks = stats.streak.weeks, days = stats.streak.days)
        }

        if (last != null) {
            Spacer(Modifier.height(Spacing.lg))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                PillTag(text = Format.parseDate(last.date)?.let(Format::dateWithWeekday) ?: last.date)
                PillTag(text = Format.minutes(last.minutes))
                PillTag(text = Format.volume(last.volumeKg))
            }
            if (last.muscleGroups.isNotEmpty()) {
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    text = last.muscleGroups.joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = IronTextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(Modifier.height(Spacing.lg))
        PrimaryButton(
            text = "Treinar agora",
            onClick = onStartWorkout,
            iconRes = R.drawable.ic_dumbbell,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun StreakBadge(weeks: Int, days: Int) {
    Column(horizontalAlignment = Alignment.End) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_flame),
                contentDescription = null,
                tint = IronRecord,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.size(Spacing.xs))
            Text(
                text = "$weeks sem",
                style = IronText.metricSmall,
                color = IronRecord,
            )
        }
        Text(
            text = if (days > 0) "$days dias seguidos" else "sequência semanal",
            style = MaterialTheme.typography.labelMedium,
            color = IronTextMuted,
        )
    }
}

@Composable
private fun MonthTiles(stats: StatsDto, modifier: Modifier = Modifier) {
    val current = stats.monthly.lastOrNull()
    val spark = stats.monthly.takeLast(6).map { it.volumeKg }

    Row(
        modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        StatTile(
            label = "volume",
            value = Format.volume(current?.volumeKg ?: 0.0).substringBefore(' '),
            unit = Format.volume(current?.volumeKg ?: 0.0).substringAfter(' '),
            caption = current?.month?.let { month ->
                Format.parseMonth(month)?.let { "em ${Format.monthLong(it)}" }
            },
            spark = spark,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        )
        StatTile(
            label = "sessões",
            value = Format.integer(current?.sessions ?: 0),
            caption = "${Format.minutes(current?.minutes)} no mês",
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        )
        StatTile(
            label = "semana",
            value = Format.weightValue(stats.averages.sessionsPerWeek),
            caption = "${Format.integer(stats.totals.sessions)} em 12m",
            accent = IronPositive,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        )
    }
}

@Composable
private fun ConsistencyCard(calendar: List<CalendarDayDto>, modifier: Modifier = Modifier) {
    val days = heatDays(calendar)

    IronCard(modifier) {
        CardHeader(
            title = "Constância",
            caption = "${calendar.size} dias treinados nas últimas 26 semanas",
        )
        Spacer(Modifier.height(Spacing.lg))
        Heatmap(days = days, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun VolumeCard(monthly: List<MonthlyDto>, modifier: Modifier = Modifier) {
    val bars = monthly.takeLast(12).map { month ->
        BarDatum(
            label = Format.parseMonth(month.month)?.let(Format::monthShort) ?: month.month.takeLast(2),
            value = month.volumeKg,
        )
    }
    val best = monthly.maxByOrNull { it.volumeKg }

    IronCard(modifier) {
        CardHeader(
            title = "Volume por mês",
            caption = best?.let { "melhor mês: ${Format.volume(it.volumeKg)}" },
        )
        Spacer(Modifier.height(Spacing.lg))
        BarChart(bars = bars, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun MuscleGroupCard(stats: StatsDto, modifier: Modifier = Modifier) {
    val rows = stats.byMuscleGroup.take(6).mapIndexed { index, group ->
        ShareRow(
            label = group.muscleGroup ?: "Sem grupo",
            value = "${Format.volume(group.volumeKg)} · ${Format.percent(group.share)}",
            share = group.share,
            color = MuscleGroupColors[index % MuscleGroupColors.size],
        )
    }

    IronCard(modifier) {
        CardHeader(title = "Distribuição por grupo", caption = "volume dos últimos 12 meses")
        Spacer(Modifier.height(Spacing.lg))
        ShareBars(rows = rows, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun RecordsCard(records: List<RecordDto>, modifier: Modifier = Modifier) {
    IronCard(modifier) {
        CardHeader(title = "Recordes recentes", caption = "últimos 30 dias")
        Spacer(Modifier.height(Spacing.sm))
        records.take(5).forEach { record ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = record.name,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = buildString {
                            append(if (record.type == "weight") "carga máxima" else "1RM estimado")
                            append(" · ")
                            append(Format.parseDate(record.date)?.let(Format::dayMonth) ?: record.date)
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = IronTextMuted,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = Format.weight(record.value), style = IronText.metricSmall, color = IronRecord)
                    DeltaBadge(delta = record.previous?.let { record.value - it })
                }
            }
            Spacer(Modifier.height(Spacing.md))
        }
    }
}

@Composable
private fun TopExercisesCard(stats: StatsDto, modifier: Modifier = Modifier) {
    IronCard(modifier) {
        CardHeader(title = "Exercícios que puxam o volume", caption = "12 meses")
        Spacer(Modifier.height(Spacing.sm))
        stats.topExercises.take(5).forEach { exercise ->
            KeyValueRow(
                label = exercise.name,
                value = Format.volume(exercise.volumeKg),
            )
        }
    }
}

@Composable
private fun SyncFooter(syncedAt: Long?, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            text = syncedAt?.let {
                val minutes = ((System.currentTimeMillis() - it) / 60_000L).toInt()
                when {
                    minutes < 1 -> "Sincronizado agora"
                    minutes < 60 -> "Sincronizado há $minutes min"
                    else -> "Sincronizado há ${minutes / 60} h"
                }
            } ?: "Nunca sincronizado",
            style = MaterialTheme.typography.labelMedium,
            color = IronTextMuted,
        )
    }
}
