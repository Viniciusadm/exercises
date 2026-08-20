package com.archieapps.exercises

import com.archieapps.exercises.core.net.dto.CalendarDayDto
import com.archieapps.exercises.ui.feature.home.heatDays
import com.archieapps.exercises.ui.feature.home.referenceVolume
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeDerivationsTest {
    @Test
    fun `intensity is normalized by the ninetieth percentile so outliers do not flatten the scale`() {
        val calendar = (1..10).map { day ->
            CalendarDayDto(date = "2026-08-%02d".format(day), sessions = 1, volumeKg = day * 1000.0)
        } + CalendarDayDto(date = "2026-08-20", sessions = 1, volumeKg = 90_000.0)

        val reference = referenceVolume(calendar)
        val days = heatDays(calendar)

        assertEquals(10_000.0, reference, 0.01)
        assertEquals(11, days.size)
        assertEquals(1f, days.last().intensity, 0.001f)
        assertEquals(0.5f, days.first { it.date == LocalDate.of(2026, 8, 5) }.intensity, 0.001f)
        assertTrue(days.all { it.intensity in 0.3f..1f })
    }

    @Test
    fun `a trained day without load still shows up`() {
        val days = heatDays(listOf(CalendarDayDto(date = "2026-08-13", sessions = 1, volumeKg = 0.0)))

        assertEquals(1, days.size)
        assertTrue(days.first().intensity > 0f)
    }

    @Test
    fun `invalid dates are dropped instead of crashing the heatmap`() {
        val days = heatDays(
            listOf(
                CalendarDayDto(date = "2026-08-13", sessions = 1, volumeKg = 100.0),
                CalendarDayDto(date = "sem data", sessions = 1, volumeKg = 100.0),
            )
        )

        assertEquals(1, days.size)
    }

    @Test
    fun `an empty calendar has a safe reference volume`() {
        assertEquals(1.0, referenceVolume(emptyList()), 0.001)
        assertTrue(heatDays(emptyList()).isEmpty())
    }
}
