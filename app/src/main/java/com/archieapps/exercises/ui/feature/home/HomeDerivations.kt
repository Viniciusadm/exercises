package com.archieapps.exercises.ui.feature.home

import com.archieapps.exercises.core.format.Format
import com.archieapps.exercises.core.net.dto.CalendarDayDto
import com.archieapps.exercises.ui.component.HeatDay

private const val MIN_INTENSITY = 0.3f
private const val NO_VOLUME_INTENSITY = 0.45f

/**
 * Normaliza o volume diário pelo percentil 90 para o heatmap: dias muito acima da média não
 * achatam a escala do resto, e um dia treinado sem carga (só tempo) ainda aparece preenchido.
 */
fun heatDays(calendar: List<CalendarDayDto>): List<HeatDay> {
    val reference = referenceVolume(calendar)

    return calendar.mapNotNull { day ->
        Format.parseDate(day.date)?.let { date ->
            HeatDay(
                date = date,
                intensity = if (day.volumeKg <= 0.0) {
                    NO_VOLUME_INTENSITY
                } else {
                    (day.volumeKg / reference).toFloat().coerceIn(MIN_INTENSITY, 1f)
                },
            )
        }
    }
}

fun referenceVolume(calendar: List<CalendarDayDto>): Double {
    val volumes = calendar.map { it.volumeKg }.filter { it > 0.0 }.sorted()
    if (volumes.isEmpty()) return 1.0

    val index = ((volumes.size - 1) * 0.9).toInt()

    return volumes[index].takeIf { it > 0.0 } ?: 1.0
}
