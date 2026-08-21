package com.archieapps.exercises.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.archieapps.exercises.core.format.Format
import com.archieapps.exercises.core.net.dto.TimelineSegmentDto
import com.archieapps.exercises.core.net.dto.WorkoutTimeDto
import com.archieapps.exercises.core.session.LocalTimelineSegment
import com.archieapps.exercises.core.session.SessionPhaseKind
import com.archieapps.exercises.core.session.SessionTimeBreakdown
import com.archieapps.exercises.ui.design.IronBorder
import com.archieapps.exercises.ui.design.IronPrimary
import com.archieapps.exercises.ui.design.IronRecord
import com.archieapps.exercises.ui.design.IronSurfaceHigh
import com.archieapps.exercises.ui.design.IronText
import com.archieapps.exercises.ui.design.IronTextMuted
import com.archieapps.exercises.ui.design.IronTextPrimary
import com.archieapps.exercises.ui.design.IronTextSecondary
import com.archieapps.exercises.ui.design.Spacing

val ExecutionColor: Color = IronPrimary
val RestColor: Color = IronRecord
val IdleColor: Color = IronTextMuted

fun phaseColor(phase: String): Color = when (phase) {
    SessionPhaseKind.EXECUTION -> ExecutionColor
    SessionPhaseKind.REST -> RestColor
    else -> IdleColor
}

data class SessionTimeMetrics(
    val totalSeconds: Long,
    val executionSeconds: Long,
    val restSeconds: Long,
    val idleSeconds: Long,
    val setsCount: Int,
    val avgSetSeconds: Int?,
    val avgRestSeconds: Int?,
) {
    val executionShare: Float get() = share(executionSeconds)

    val restShare: Float get() = share(restSeconds)

    val idleShare: Float get() = share(idleSeconds)

    private fun share(part: Long): Float = if (totalSeconds > 0) part.toFloat() / totalSeconds else 0f

    companion object {
        fun of(time: WorkoutTimeDto): SessionTimeMetrics = SessionTimeMetrics(
            totalSeconds = time.totalSeconds.toLong(),
            executionSeconds = time.executionSeconds.toLong(),
            restSeconds = time.restSeconds.toLong(),
            idleSeconds = time.idleSeconds.toLong(),
            setsCount = time.setsCount,
            avgSetSeconds = time.avgSetSeconds,
            avgRestSeconds = time.avgRestSeconds,
        )

        fun of(breakdown: SessionTimeBreakdown): SessionTimeMetrics = SessionTimeMetrics(
            totalSeconds = breakdown.totalSeconds,
            executionSeconds = breakdown.executionSeconds,
            restSeconds = breakdown.restSeconds,
            idleSeconds = breakdown.idleSeconds,
            setsCount = breakdown.setsCount,
            avgSetSeconds = breakdown.avgSetSeconds,
            avgRestSeconds = breakdown.avgRestSeconds,
        )
    }
}

data class TimelineEntry(
    val sequence: Int,
    val phase: String,
    val label: String,
    val atEpochMillis: Long?,
    val durationSeconds: Long,
    val editable: Boolean = false,
    val adjusted: Boolean = false,
) {
    companion object {
        fun of(segment: TimelineSegmentDto): TimelineEntry = TimelineEntry(
            sequence = segment.sequence,
            phase = segment.phase,
            label = segment.label,
            atEpochMillis = Format.parseInstant(segment.at),
            durationSeconds = segment.durationSeconds.toLong(),
            editable = segment.editable,
            adjusted = segment.adjusted,
        )

        fun of(segment: LocalTimelineSegment): TimelineEntry = TimelineEntry(
            sequence = segment.sequence,
            phase = segment.phase,
            label = segment.label,
            atEpochMillis = segment.atEpochMillis,
            durationSeconds = segment.durationSeconds,
        )
    }
}

@Composable
fun SessionTimeCard(
    metrics: SessionTimeMetrics?,
    modifier: Modifier = Modifier,
    title: String = "Tempo do treino",
    fallbackTotalSeconds: Long? = null,
) {
    IronCard(modifier) {
        if (metrics == null) {
            CardHeader(title = title, caption = "sem detalhamento")
            Spacer(Modifier.height(Spacing.md))

            StatTile(
                label = "tempo total",
                value = Format.clock(fallbackTotalSeconds),
                caption = "única medida disponível",
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(Spacing.md))
            Text(
                text = "Detalhamento de tempo não disponível — treino registrado antes da segregação de tempo.",
                style = MaterialTheme.typography.bodyMedium,
                color = IronTextMuted,
            )

            return@IronCard
        }

        CardHeader(
            title = title,
            caption = "${metrics.setsCount} séries · ${Format.clock(metrics.totalSeconds)}",
        )

        Spacer(Modifier.height(Spacing.md))
        PhaseStackBar(metrics)
        Spacer(Modifier.height(Spacing.md))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            StatTile(
                label = "total",
                value = Format.clock(metrics.totalSeconds),
                caption = "100%",
                modifier = Modifier.weight(1f),
            )
            StatTile(
                label = "execução",
                value = Format.duration(metrics.executionSeconds),
                caption = Format.percent(metrics.executionShare.toDouble()),
                accent = ExecutionColor,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(Spacing.sm))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            StatTile(
                label = "descanso",
                value = Format.duration(metrics.restSeconds),
                caption = Format.percent(metrics.restShare.toDouble()),
                accent = RestColor,
                modifier = Modifier.weight(1f),
            )
            StatTile(
                label = "ocioso",
                value = Format.duration(metrics.idleSeconds),
                caption = Format.percent(metrics.idleShare.toDouble()),
                accent = IdleColor,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(Spacing.md))
        HairLine()
        Spacer(Modifier.height(Spacing.sm))

        KeyValueRow(label = "Séries realizadas", value = Format.integer(metrics.setsCount))
        KeyValueRow(label = "Tempo médio por série", value = Format.duration(metrics.avgSetSeconds))
        KeyValueRow(label = "Tempo médio de descanso", value = Format.duration(metrics.avgRestSeconds))
    }
}

@Composable
fun PhaseStackBar(metrics: SessionTimeMetrics, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(10.dp)
        ) {
            val radius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
            drawRoundRect(color = IronSurfaceHigh, cornerRadius = radius)

            var offset = 0f

            listOf(
                metrics.executionShare to ExecutionColor,
                metrics.restShare to RestColor,
                metrics.idleShare to IdleColor,
            ).forEach { (share, color) ->
                val width = size.width * share.coerceIn(0f, 1f)

                if (width > 0f) {
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(offset, 0f),
                        size = Size(width, size.height),
                        cornerRadius = radius,
                    )
                    offset += width
                }
            }
        }

        Spacer(Modifier.height(Spacing.sm))

        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            PhaseLegend("execução", ExecutionColor)
            PhaseLegend("descanso", RestColor)
            PhaseLegend("ocioso", IdleColor)
        }
    }
}

@Composable
private fun PhaseLegend(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(Spacing.xs))
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = IronTextMuted)
    }
}

@Composable
fun SessionTimelineList(
    entries: List<TimelineEntry>,
    modifier: Modifier = Modifier,
    title: String = "Linha do tempo",
    startLabel: String? = null,
    endLabel: String? = null,
    onEditSegment: ((TimelineEntry) -> Unit)? = null,
) {
    IronCard(modifier) {
        CardHeader(title = title, caption = "${entries.size} trechos")
        Spacer(Modifier.height(Spacing.md))

        if (entries.isEmpty()) {
            Text(
                text = "Sem trechos registrados nesta sessão.",
                style = MaterialTheme.typography.bodyMedium,
                color = IronTextMuted,
            )

            return@IronCard
        }

        startLabel?.let { TimelineMarker(time = it, label = "Treino iniciado") }

        entries.forEach { entry ->
            TimelineRow(
                entry = entry,
                onClick = onEditSegment?.takeIf { entry.editable }?.let { handler -> { handler(entry) } },
            )
        }

        endLabel?.let { TimelineMarker(time = it, label = "Treino finalizado") }
    }
}

@Composable
private fun TimelineMarker(time: String, label: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = time,
            style = IronText.tabular,
            color = IronTextSecondary,
            modifier = Modifier.width(52.dp),
        )
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(IronTextSecondary)
        )
        Spacer(Modifier.width(Spacing.md))
        Text(text = label, style = MaterialTheme.typography.titleMedium, color = IronTextPrimary)
    }
}

@Composable
private fun TimelineRow(entry: TimelineEntry, onClick: (() -> Unit)?) {
    val color = phaseColor(entry.phase)

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .then(if (onClick == null) Modifier else Modifier.clickable(onClick = onClick))
            .padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = Format.timeOfDay(entry.atEpochMillis),
            style = IronText.tabular,
            color = IronTextSecondary,
            modifier = Modifier.width(52.dp),
        )
        Box(
            Modifier
                .width(10.dp)
                .height(26.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(color.copy(alpha = 0.35f))
                .border(1.dp, color, RoundedCornerShape(5.dp))
        )
        Column(
            Modifier
                .weight(1f)
                .padding(start = Spacing.md)
        ) {
            Text(
                text = entry.label,
                style = MaterialTheme.typography.bodyLarge,
                color = IronTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (entry.adjusted) {
                Text(
                    text = "duração ajustada",
                    style = MaterialTheme.typography.labelSmall,
                    color = IronRecord,
                )
            }
        }
        Text(
            text = Format.duration(entry.durationSeconds),
            style = IronText.tabular,
            color = color,
        )
        if (onClick != null) {
            Spacer(Modifier.width(Spacing.sm))
            Text(text = "editar", style = MaterialTheme.typography.labelSmall, color = IronTextMuted)
        }
    }

    Box(
        Modifier
            .padding(start = 57.dp)
            .width(1.dp)
            .height(6.dp)
            .background(IronBorder)
    )
}
