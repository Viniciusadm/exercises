package com.archieapps.exercises

import com.archieapps.exercises.core.net.ApiJson
import com.archieapps.exercises.core.net.Envelope
import com.archieapps.exercises.core.net.errorText
import com.archieapps.exercises.core.net.dto.CalendarDayDto
import com.archieapps.exercises.core.net.dto.ExerciseDto
import com.archieapps.exercises.core.net.dto.StatsDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Os fixtures em src/test/resources foram capturados da API real (banco com 506 treinos),
 * então este teste trava o contrato entre o backend Laravel e os DTOs do app.
 */
class ApiContractTest {
    @Test
    fun `stats payload maps into the dashboard model`() {
        val envelope = decode<StatsDto>("stats_response.json")
        val stats = assertNotNull(envelope.body).let { envelope.body!! }

        assertTrue(envelope.success)
        assertEquals(180, stats.totals.sessions)
        assertEquals(838_749.0, stats.totals.volumeKg, 0.01)
        assertEquals(10, stats.streak.weeks)
        assertEquals(50, stats.streak.bestWeeks)
        assertEquals(7, stats.byWeekday.size)
        assertNotNull(stats.lastWorkout)
        assertEquals("Treino", stats.lastWorkout?.name)
        assertTrue(stats.monthly.isNotEmpty())
        assertTrue(stats.byMuscleGroup.isNotEmpty())
        assertTrue(stats.topExercises.isNotEmpty())
        assertEquals(1.0, stats.byMuscleGroup.sumOf { it.share }, 0.02)
    }

    @Test
    fun `calendar payload maps into heatmap days`() {
        val envelope = decode<List<CalendarDayDto>>("calendar_response.json")
        val days = envelope.body!!

        assertTrue(envelope.success)
        assertEquals(45, days.size)
        assertTrue(days.all { it.date.length == 10 })
        assertTrue(days.all { it.sessions >= 1 })
        assertTrue(days.any { it.volumeKg > 0.0 })
    }

    @Test
    fun `exercise list carries usage stats and integer pagination`() {
        val envelope = decode<List<ExerciseDto>>("exercises_response.json")
        val exercises = envelope.body!!

        assertEquals(5, exercises.size)
        assertEquals(5, envelope.pagination?.limit)
        assertEquals(36, envelope.pagination?.total)
        assertEquals(8, envelope.pagination?.last)

        val first = exercises.first()
        assertTrue(first.sessionsCount > 0)
        assertNotNull(first.lastPerformedAt)
        assertEquals("Abdômen", first.muscleGroup)
    }

    @Test
    fun `failed login envelope exposes the backend message`() {
        val envelope = decode<String>("login_error_response.json")

        assertFalse(envelope.success)
        assertEquals("Email ou senha inválidos.", envelope.errorText())
    }

    private inline fun <reified T> decode(fixture: String): Envelope<T> {
        val json = requireNotNull(javaClass.classLoader?.getResourceAsStream(fixture)) {
            "fixture $fixture não encontrado"
        }.bufferedReader().use { it.readText() }

        return ApiJson.decodeFromString(json)
    }
}
