package com.archieapps.exercises

import com.archieapps.exercises.core.data.PlanDraft
import com.archieapps.exercises.core.net.ApiJson
import com.archieapps.exercises.core.net.Envelope
import com.archieapps.exercises.core.net.dto.PlanMiscDto
import com.archieapps.exercises.core.net.dto.PlanOptionsDto
import com.archieapps.exercises.core.net.dto.PlanProposalDto
import com.archieapps.exercises.ui.feature.plan.PLAN_MAX_SETS
import com.archieapps.exercises.ui.feature.plan.PLAN_MIN_SETS
import com.archieapps.exercises.ui.feature.plan.daySets
import com.archieapps.exercises.ui.feature.plan.newCanonicals
import com.archieapps.exercises.ui.feature.plan.toConfirmRequest
import kotlinx.serialization.json.decodeFromJsonElement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanContractTest {
    @Test
    fun `the proposal decodes with every field the review screen needs`() {
        val proposal = proposal()

        assertTrue(proposal.planUuid.isNotBlank())
        assertEquals("hypertrophy", proposal.objective)
        assertEquals(3, proposal.daysPerWeek)
        assertEquals(60, proposal.minutesPerDay)
        assertTrue(proposal.setsBudgetPerDay > 0)
        assertEquals(3, proposal.days.size)
        assertEquals(listOf(1, 2, 3), proposal.days.map { it.day })

        proposal.days.forEach { day ->
            assertTrue(day.name.isNotBlank())
            assertTrue(day.focus.isNotBlank())
            assertTrue(day.exercises.isNotEmpty())
            assertEquals(day.totalSets, daySets(day))
            assertTrue(day.estimatedMinutes > 0)
        }
    }

    @Test
    fun `an exercise already in the catalog carries its id and one that is new does not`() {
        val items = proposal().days.flatMap { it.exercises }

        assertTrue(items.any { !it.isNew })
        assertTrue(items.any { it.isNew })

        items.forEach { item ->
            if (item.isNew) {
                assertNull("exercício novo não pode trazer id: ${item.name}", item.exerciseId)
                assertTrue(item.ref.startsWith("canon:"))
            } else {
                assertNotNull("exercício do catálogo tem que trazer id: ${item.name}", item.exerciseId)
                assertTrue(item.ref.startsWith("cat:"))
            }
        }
    }

    @Test
    fun `the sets of every exercise stay inside the range the ui can edit`() {
        proposal().days.flatMap { it.exercises }.forEach { item ->
            assertTrue(
                "${item.name} veio com ${item.targetSets} séries",
                item.targetSets in PLAN_MIN_SETS..PLAN_MAX_SETS,
            )
        }
    }

    @Test
    fun `no day exceeds its own set budget`() {
        val proposal = proposal()

        proposal.days.forEach { day ->
            assertTrue(
                "${day.name} passou do orçamento",
                daySets(day) <= proposal.setsBudgetPerDay,
            )
        }
    }

    @Test
    fun `no exercise repeats across the whole plan`() {
        val refs = proposal().days.flatMap { it.exercises }.map { it.ref }

        assertEquals(refs.distinct(), refs)
    }

    @Test
    fun `a timed exercise carries no reps and explains the duration in the notes`() {
        val timed = proposal().days.flatMap { it.exercises }.filter { it.executionType == "time" }

        assertTrue("a fixture precisa de um exercício por tempo", timed.isNotEmpty())

        timed.forEach { item ->
            assertNull(item.targetReps)
            assertNotNull(item.notes)
        }
    }

    @Test
    fun `a new exercise never arrives with a weight`() {
        newCanonicals(proposal()).forEach { item ->
            assertNull("${item.name} veio com carga sem histórico", item.targetWeightKg)
        }
    }

    @Test
    fun `every exercise names the muscles it works`() {
        proposal().days.flatMap { it.exercises }.forEach { item ->
            assertTrue("${item.name} não diz o que trabalha", item.muscles.primary.isNotEmpty())
            assertTrue(item.muscles.primary.all { it.name.isNotBlank() && it.slug.isNotBlank() })
        }
    }

    @Test
    fun `the misc block carries what the review screen shows`() {
        val misc = misc()

        assertTrue(misc.source == "deterministic" || misc.source == "ai")
        assertTrue(misc.poolSize > 0)
        assertTrue(misc.deficitsConsidered > 0)
        assertEquals(newCanonicals(proposal()).size, misc.newExercisesCount)
    }

    @Test
    fun `the confirm request keeps every ref and target of the proposal`() {
        val proposal = proposal()
        val request = toConfirmRequest(proposal)

        assertEquals(proposal.planUuid, request.planUuid)
        assertEquals(proposal.source, request.source)
        assertEquals(proposal.days.size, request.days.size)

        proposal.days.zip(request.days).forEach { (day, requested) ->
            assertEquals(day.name, requested.name)
            assertEquals(day.exercises.map { it.ref }, requested.exercises.map { it.ref })
            assertEquals(day.exercises.map { it.targetSets }, requested.exercises.map { it.targetSets })
            assertEquals(day.exercises.map { it.targetReps }, requested.exercises.map { it.targetReps })
            assertEquals(day.exercises.map { it.restSeconds }, requested.exercises.map { it.restSeconds })
        }
    }

    @Test
    fun `the draft survives a round trip through the cache`() {
        val draft = PlanDraft(proposal = proposal(), misc = misc())
        val encoded = ApiJson.encodeToString(PlanDraft.serializer(), draft)
        val decoded = ApiJson.decodeFromString(PlanDraft.serializer(), encoded)

        assertEquals(draft, decoded)
    }

    @Test
    fun `the options payload fills the whole form`() {
        val options = decode<PlanOptionsDto>("plan_options_response.json").body!!

        assertEquals(listOf("strength", "hypertrophy", "endurance"), options.objectives.map { it.value })
        assertTrue(options.objectives.all { it.label.isNotBlank() })
        assertEquals(1, options.daysPerWeek.min)
        assertEquals(6, options.daysPerWeek.max)
        assertEquals(30, options.minutesPerDay.min)
        assertEquals(120, options.minutesPerDay.max)
        assertEquals(15, options.minutesPerDay.step)
        assertTrue(options.equipment.isNotEmpty())
        assertFalse(options.equipment.any { it.isBlank() })
        assertEquals(listOf(PLAN_MIN_SETS, PLAN_MAX_SETS), options.setsPerExerciseRange)
    }

    @Test
    fun `every equipment the plan uses is offered by the options endpoint`() {
        val offered = decode<PlanOptionsDto>("plan_options_response.json").body!!.equipment.toSet()
        val used = proposal().days.flatMap { it.exercises }.mapNotNull { it.equipment }.toSet()

        assertEquals(emptySet<String>(), used - offered)
    }

    private fun envelope(): Envelope<PlanProposalDto> = decode("plan_response.json")

    private fun proposal(): PlanProposalDto = envelope().body!!

    private fun misc(): PlanMiscDto =
        ApiJson.decodeFromJsonElement(requireNotNull(envelope().misc))

    private inline fun <reified T> decode(fixture: String): Envelope<T> {
        val json = requireNotNull(javaClass.classLoader?.getResourceAsStream(fixture)) {
            "fixture $fixture não encontrado"
        }.bufferedReader().use { it.readText() }

        return ApiJson.decodeFromString(json)
    }
}
