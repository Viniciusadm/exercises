package com.archieapps.exercises

import com.archieapps.exercises.core.data.PlanDraft
import com.archieapps.exercises.core.db.ExerciseEntity
import com.archieapps.exercises.core.net.dto.PlanDayDto
import com.archieapps.exercises.core.net.dto.PlanExerciseDto
import com.archieapps.exercises.core.net.dto.PlanMuscleDto
import com.archieapps.exercises.core.net.dto.PlanMusclesDto
import com.archieapps.exercises.core.net.dto.PlanProposalDto
import com.archieapps.exercises.ui.feature.plan.PLAN_MAX_SETS
import com.archieapps.exercises.ui.feature.plan.PLAN_MIN_SETS
import com.archieapps.exercises.ui.feature.plan.alternativesFor
import com.archieapps.exercises.ui.feature.plan.canConfirm
import com.archieapps.exercises.ui.feature.plan.dayMinutes
import com.archieapps.exercises.ui.feature.plan.daySets
import com.archieapps.exercises.ui.feature.plan.equipmentOptions
import com.archieapps.exercises.ui.feature.plan.newCanonicals
import com.archieapps.exercises.ui.feature.plan.planProblems
import com.archieapps.exercises.ui.feature.plan.planSummary
import com.archieapps.exercises.ui.feature.plan.toConfirmRequest
import com.archieapps.exercises.ui.feature.plan.usedRefs
import com.archieapps.exercises.ui.feature.plan.withReplacement
import com.archieapps.exercises.ui.feature.plan.withReps
import com.archieapps.exercises.ui.feature.plan.withSets
import com.archieapps.exercises.ui.feature.plan.withoutItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanDerivationsTest {
    private fun item(
        ref: String,
        name: String = "Exercício",
        sets: Int = 3,
        reps: Int? = 10,
        isNew: Boolean = false,
        primary: List<String> = listOf("quadriceps"),
        executionType: String = "sets",
    ) = PlanExerciseDto(
        ref = ref,
        exerciseId = if (isNew) null else ref.removePrefix("cat:").toIntOrNull(),
        name = name,
        isNew = isNew,
        executionType = executionType,
        muscles = PlanMusclesDto(primary = primary.map { PlanMuscleDto(it, it) }),
        targetSets = sets,
        targetReps = reps,
        restSeconds = 60,
    )

    private fun draft(vararg days: PlanDayDto) = PlanDraft(
        proposal = PlanProposalDto(
            planUuid = "5f3ab1e6-2c4d-4f0a-9b6e-8d1c2a3b4c5d",
            source = "deterministic",
            daysPerWeek = days.size,
            setsBudgetPerDay = 20,
            days = days.toList(),
        )
    )

    private fun day(number: Int, vararg items: PlanExerciseDto) = PlanDayDto(
        day = number,
        focus = "Foco",
        name = "Dia $number",
        totalSets = items.sumOf { it.targetSets },
        estimatedMinutes = 0,
        exercises = items.toList(),
    )

    @Test
    fun `the minutes of a day follow the same convention as the backend budget`() {
        val target = day(1, item("cat:1", sets = 4), item("cat:2", sets = 4))

        assertEquals(8, daySets(target))
        assertEquals(30, dayMinutes(target))
    }

    @Test
    fun `changing the sets recomputes the day totals`() {
        val updated = withSets(draft(day(1, item("cat:1", sets = 3))), 1, "cat:1", 5)
        val target = updated.proposal.days.first()

        assertEquals(5, target.exercises.first().targetSets)
        assertEquals(5, target.totalSets)
        assertEquals(dayMinutes(target), target.estimatedMinutes)
    }

    @Test
    fun `the sets stay inside the range the backend accepts`() {
        val base = draft(day(1, item("cat:1", sets = 3)))

        assertEquals(PLAN_MAX_SETS, withSets(base, 1, "cat:1", 99).proposal.days[0].exercises[0].targetSets)
        assertEquals(PLAN_MIN_SETS, withSets(base, 1, "cat:1", 0).proposal.days[0].exercises[0].targetSets)
    }

    @Test
    fun `an edit only touches the day it was made on`() {
        val base = draft(day(1, item("cat:1")), day(2, item("cat:1", name = "Outro dia")))
        val updated = withSets(base, 2, "cat:1", 5)

        assertEquals(3, updated.proposal.days[0].exercises[0].targetSets)
        assertEquals(5, updated.proposal.days[1].exercises[0].targetSets)
    }

    @Test
    fun `changing the reps keeps them positive`() {
        val base = draft(day(1, item("cat:1", reps = 10)))

        assertEquals(1, withReps(base, 1, "cat:1", 0).proposal.days[0].exercises[0].targetReps)
        assertNull(withReps(base, 1, "cat:1", null).proposal.days[0].exercises[0].targetReps)
    }

    @Test
    fun `removing the last exercise of a day drops the day and renumbers the rest`() {
        val base = draft(
            day(1, item("cat:1")),
            day(2, item("cat:2")),
            day(3, item("cat:3")),
        )

        val updated = withoutItem(base, 2, "cat:2")

        assertEquals(2, updated.proposal.days.size)
        assertEquals(listOf(1, 2), updated.proposal.days.map { it.day })
        assertEquals(listOf("cat:1", "cat:3"), updated.proposal.days.map { it.exercises[0].ref })
    }

    @Test
    fun `a day with no exercises is unreachable so confirming stays possible`() {
        val updated = withoutItem(draft(day(1, item("cat:1"), item("cat:2"))), 1, "cat:1")

        assertEquals(1, updated.proposal.days.size)
        assertEquals(1, updated.proposal.days[0].exercises.size)
        assertTrue(canConfirm(updated.proposal))
    }

    @Test
    fun `a swap keeps the sets the user already adjusted`() {
        val base = draft(day(1, item("cat:1", sets = 5)))
        val replacement = item("cat:9", name = "Substituto", sets = 2)

        val updated = withReplacement(base, 1, "cat:1", replacement)
        val swapped = updated.proposal.days[0].exercises[0]

        assertEquals("cat:9", swapped.ref)
        assertEquals("Substituto", swapped.name)
        assertEquals(5, swapped.targetSets)
    }

    @Test
    fun `swapping for a timed exercise clears the reps`() {
        val base = draft(day(1, item("cat:1", reps = 12)))
        val replacement = item("cat:9", executionType = "time", reps = null)

        val updated = withReplacement(base, 1, "cat:1", replacement)

        assertNull(updated.proposal.days[0].exercises[0].targetReps)
    }

    @Test
    fun `the summary counts days sets and new exercises`() {
        val proposal = draft(
            day(1, item("cat:1", sets = 3), item("canon:novo", isNew = true, sets = 4)),
            day(2, item("cat:2", sets = 3)),
        ).proposal

        assertEquals("2 dias · 10 séries · 1 exercício novo", planSummary(proposal))
    }

    @Test
    fun `a plan with no new exercise says nothing about it`() {
        val proposal = draft(day(1, item("cat:1", sets = 3))).proposal

        assertEquals("1 dia · 3 séries", planSummary(proposal))
    }

    @Test
    fun `the new exercises are listed once even when they repeat`() {
        val proposal = draft(
            day(1, item("canon:novo", isNew = true)),
            day(2, item("canon:novo", isNew = true)),
        ).proposal

        assertEquals(listOf("canon:novo"), newCanonicals(proposal).map { it.ref })
    }

    @Test
    fun `an empty plan cannot be confirmed`() {
        val proposal = PlanProposalDto(planUuid = "5f3ab1e6-2c4d-4f0a-9b6e-8d1c2a3b4c5d")

        assertFalse(canConfirm(proposal))
        assertTrue(planProblems(proposal).any { it.contains("sem nenhum dia") })
    }

    @Test
    fun `a plan without an identifier cannot be confirmed`() {
        val proposal = draft(day(1, item("cat:1"))).proposal.copy(planUuid = "")

        assertFalse(canConfirm(proposal))
    }

    @Test
    fun `a repeated exercise blocks confirming`() {
        val proposal = draft(day(1, item("cat:1")), day(2, item("cat:1"))).proposal

        assertFalse(canConfirm(proposal))
        assertTrue(planProblems(proposal).any { it.contains("repetido") })
    }

    @Test
    fun `the confirm request carries the day names and the refs in order`() {
        val proposal = draft(day(1, item("cat:1"), item("canon:novo", isNew = true))).proposal
        val request = toConfirmRequest(proposal)

        assertEquals("5f3ab1e6-2c4d-4f0a-9b6e-8d1c2a3b4c5d", request.planUuid)
        assertEquals(listOf("Dia 1"), request.days.map { it.name })
        assertEquals(listOf("cat:1", "canon:novo"), request.days[0].exercises.map { it.ref })
    }

    @Test
    fun `the equipment list merges catalog and server without duplicates or case noise`() {
        val options = equipmentOptions(
            fromServer = listOf("Barra", "Halter", " "),
            fromCatalog = listOf("halter", "Máquina", ""),
        )

        assertEquals(listOf("Barra", "Halter", "Máquina"), options)
    }

    @Test
    fun `the alternatives only offer exercises that work the same muscle`() {
        val target = item("canon:novo", isNew = true, primary = listOf("gluteo-medio"))
        val catalog = listOf(
            exercise(1, "Cadeira abdutora", sessions = 5),
            exercise(2, "Supino", sessions = 40),
        )

        val muscles = mapOf(
            1 to listOf(PlanMuscleDto("gluteo-medio", "Glúteo médio")),
            2 to listOf(PlanMuscleDto("peitoral", "Peitoral")),
        )

        val options = alternativesFor(target, catalog, { muscles[it].orEmpty() }, emptySet())

        assertEquals(listOf("Cadeira abdutora"), options.map { it.name })
        assertEquals(listOf("cat:1"), options.map { it.ref })
    }

    @Test
    fun `the alternatives never offer something already in the plan`() {
        val proposal = draft(day(1, item("cat:1"), item("cat:2"))).proposal
        val target = proposal.days[0].exercises[0]
        val catalog = listOf(exercise(1, "Já está"), exercise(2, "Também está"), exercise(3, "Livre"))
        val muscles = (1..3).associateWith { listOf(PlanMuscleDto("quadriceps", "Quadríceps")) }

        val options = alternativesFor(target, catalog, { muscles[it].orEmpty() }, usedRefs(proposal))

        assertEquals(listOf("cat:3"), options.map { it.ref })
    }

    @Test
    fun `the alternatives put the most trained exercise first`() {
        val target = item("cat:9", primary = listOf("quadriceps"))
        val catalog = listOf(
            exercise(1, "Pouco usado", sessions = 2),
            exercise(2, "Muito usado", sessions = 30),
        )
        val muscles = mapOf(
            1 to listOf(PlanMuscleDto("quadriceps", "Quadríceps")),
            2 to listOf(PlanMuscleDto("quadriceps", "Quadríceps")),
        )

        val options = alternativesFor(target, catalog, { muscles[it].orEmpty() }, emptySet())

        assertEquals(listOf("Muito usado", "Pouco usado"), options.map { it.name })
    }

    @Test
    fun `an exercise with no mapped muscle has no alternatives to offer`() {
        val target = item("cat:1", primary = emptyList())
        val catalog = listOf(exercise(2, "Qualquer"))

        assertTrue(alternativesFor(target, catalog, { emptyList() }, emptySet()).isEmpty())
    }

    private fun exercise(id: Int, name: String, sessions: Int = 0) = ExerciseEntity(
        id = id,
        name = name,
        executionType = "sets",
        usesWeight = true,
        muscleGroup = null,
        difficulty = null,
        equipment = "Máquina",
        restSeconds = 60,
        notes = null,
        lastPerformedAt = null,
        sessionsCount = sessions,
        bestWeightKg = null,
        totalVolumeKg = 0.0,
    )
}
