package com.archieapps.exercises.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.archieapps.exercises.ui.design.IronPrimary
import com.archieapps.exercises.ui.design.IronSurfaceHigh
import com.archieapps.exercises.ui.design.IronTextMuted
import com.archieapps.exercises.ui.design.IronTextSecondary
import com.archieapps.exercises.ui.design.Spacing

data class BarDatum(val label: String, val value: Double)

data class ShareRow(val label: String, val value: String, val share: Double, val color: Color)

data class HeatDay(val date: java.time.LocalDate, val intensity: Float)

fun heatColor(intensity: Float): Color = if (intensity <= 0f) {
    IronSurfaceHigh
} else {
    lerp(IronPrimary.copy(alpha = 0.35f), IronPrimary, intensity.coerceIn(0f, 1f))
}

@Composable
fun Sparkline(
    values: List<Double>,
    modifier: Modifier = Modifier,
    color: Color = IronPrimary,
) {
    Canvas(modifier) {
        if (values.size < 2) return@Canvas

        val max = values.maxOrNull() ?: return@Canvas
        val min = values.minOrNull() ?: return@Canvas
        val span = (max - min).takeIf { it > 0.0 } ?: 1.0
        val stepX = size.width / (values.size - 1)
        val points = values.mapIndexed { index, value ->
            Offset(
                x = index * stepX,
                y = (size.height - 2f) * (1f - ((value - min) / span).toFloat()) + 1f,
            )
        }

        val line = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
        }

        val area = Path().apply {
            addPath(line)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }

        drawPath(area, Brush.verticalGradient(listOf(color.copy(alpha = 0.28f), Color.Transparent)))
        drawPath(line, color, style = Stroke(width = 2.dp.toPx()))
    }
}

@Composable
fun BarChart(
    bars: List<BarDatum>,
    modifier: Modifier = Modifier,
    barColor: Color = IronPrimary,
    labelEvery: Int = 2,
) {
    if (bars.isEmpty()) return

    Column(modifier) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(120.dp)
        ) {
            val max = bars.maxOf { it.value }.takeIf { it > 0.0 } ?: 1.0
            val gap = 6.dp.toPx()
            val slot = (size.width + gap) / bars.size
            val barWidth = (slot - gap).coerceAtLeast(2f)
            val radius = CornerRadius(3.dp.toPx(), 3.dp.toPx())

            bars.forEachIndexed { index, bar ->
                val ratio = (bar.value / max).toFloat().coerceIn(0f, 1f)
                val barHeight = (size.height * ratio).coerceAtLeast(if (bar.value > 0) 3.dp.toPx() else 0f)
                val x = index * slot
                val isLast = index == bars.lastIndex

                drawRoundRect(
                    color = IronSurfaceHigh,
                    topLeft = Offset(x, 0f),
                    size = Size(barWidth, size.height),
                    cornerRadius = radius,
                )

                if (barHeight > 0f) {
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            listOf(
                                if (isLast) barColor else barColor.copy(alpha = 0.72f),
                                if (isLast) barColor.copy(alpha = 0.6f) else barColor.copy(alpha = 0.32f),
                            )
                        ),
                        topLeft = Offset(x, size.height - barHeight),
                        size = Size(barWidth, barHeight),
                        cornerRadius = radius,
                    )
                }
            }
        }

        Spacer(Modifier.height(Spacing.sm))

        Row(Modifier.fillMaxWidth()) {
            bars.forEachIndexed { index, bar ->
                Text(
                    text = if (index % labelEvery == 0 || index == bars.lastIndex) bar.label else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (index == bars.lastIndex) IronTextSecondary else IronTextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
fun ShareBars(rows: List<ShareRow>, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        rows.forEach { row ->
            Column {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(row.label, style = MaterialTheme.typography.bodyMedium)
                    Text(row.value, style = MaterialTheme.typography.labelMedium, color = IronTextSecondary)
                }
                Spacer(Modifier.height(Spacing.xs))
                Canvas(
                    Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                ) {
                    val radius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                    drawRoundRect(color = IronSurfaceHigh, cornerRadius = radius)
                    drawRoundRect(
                        color = row.color,
                        size = Size(size.width * row.share.toFloat().coerceIn(0f, 1f), size.height),
                        cornerRadius = radius,
                    )
                }
            }
        }
    }
}

@Composable
fun Heatmap(
    days: List<HeatDay>,
    modifier: Modifier = Modifier,
    cell: androidx.compose.ui.unit.Dp = 13.dp,
    gap: androidx.compose.ui.unit.Dp = 3.dp,
) {
    val density = LocalDensity.current
    val cellPx = with(density) { cell.toPx() }
    val gapPx = with(density) { gap.toPx() }
    val byDate = days.associateBy { it.date }
    val lastDate = days.maxOfOrNull { it.date } ?: java.time.LocalDate.now()
    val lastColumnStart = lastDate.with(java.time.DayOfWeek.MONDAY)

    Row(modifier, verticalAlignment = Alignment.Top) {
        Column(
            modifier = Modifier.padding(end = Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(gap),
        ) {
            listOf("S", "T", "Q", "Q", "S", "S", "D").forEach { letter ->
                Text(
                    text = letter,
                    style = MaterialTheme.typography.labelSmall,
                    color = IronTextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .width(10.dp)
                        .height(cell),
                )
            }
        }

        Canvas(
            Modifier
                .fillMaxWidth()
                .height(cell * 7 + gap * 6)
        ) {
            val columns = ((size.width + gapPx) / (cellPx + gapPx)).toInt().coerceAtLeast(1)
            val radius = CornerRadius(3.dp.toPx(), 3.dp.toPx())

            for (column in 0 until columns) {
                val weekStart = lastColumnStart.minusWeeks((columns - 1 - column).toLong())

                for (row in 0 until 7) {
                    val date = weekStart.plusDays(row.toLong())
                    if (date.isAfter(lastDate)) continue

                    drawRoundRect(
                        color = heatColor(byDate[date]?.intensity ?: 0f),
                        topLeft = Offset(column * (cellPx + gapPx), row * (cellPx + gapPx)),
                        size = Size(cellPx, cellPx),
                        cornerRadius = radius,
                    )
                }
            }
        }
    }
}

@Composable
fun LineChart(
    values: List<Double>,
    modifier: Modifier = Modifier,
    color: Color = IronPrimary,
    showDots: Boolean = true,
) {
    if (values.size < 2) return

    Canvas(modifier) {
        val max = values.max()
        val min = values.min()
        val span = (max - min).takeIf { it > 0.0 } ?: (max.takeIf { it > 0.0 } ?: 1.0)
        val stepX = size.width / (values.size - 1)
        val radius = 3.dp.toPx()
        val points = values.mapIndexed { index, value ->
            Offset(
                x = index * stepX,
                y = (size.height - radius * 2) * (1f - ((value - min) / span).toFloat()) + radius,
            )
        }

        val line = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
        }

        val area = Path().apply {
            addPath(line)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }

        drawPath(area, Brush.verticalGradient(listOf(color.copy(alpha = 0.22f), Color.Transparent)))
        drawPath(line, color, style = Stroke(width = 2.5.dp.toPx()))

        if (showDots) {
            points.forEach { point ->
                drawCircle(color = IronSurfaceHigh, radius = radius + 1.5f, center = point)
                drawCircle(color = color, radius = radius, center = point)
            }
        }
    }
}
