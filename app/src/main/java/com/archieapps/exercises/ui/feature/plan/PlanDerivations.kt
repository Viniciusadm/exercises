package com.archieapps.exercises.ui.feature.plan

import com.archieapps.exercises.core.data.PlanDraft
import com.archieapps.exercises.core.db.ExerciseEntity
import com.archieapps.exercises.core.net.dto.PlanConfirmDayRequest
import com.archieapps.exercises.core.net.dto.PlanConfirmExerciseRequest
import com.archieapps.exercises.core.net.dto.PlanConfirmRequest
import com.archieapps.exercises.core.net.dto.PlanDayDto
import com.archieapps.exercises.core.net.dto.PlanExerciseDto
import com.archieapps.exercises.core.net.dto.PlanMuscleDto
import com.archieapps.exercises.core.net.dto.PlanProposalDto

const val PLAN_OVERHEAD_MINUTES = 10
const val PLAN_MINUTES_PER_SET = 2.5
const val PLAN_MIN_SETS = 2
const val PLAN_MAX_SETS = 5
const val PLAN_MIN_REPS = 1
const val PLAN_MAX_REPS = 100

val PLAN_OBJECTIVE_LABELS = mapOf(
    "strength" to "Força",
    "hypertrophy" to "Hipertrofia",
    "endurance" to "Resistência",
)

fun daySets(day: PlanDayDto): Int = day.exercises.sumOf { it.targetSets }

fun dayMinutes(day: PlanDayDto): Int =
    (PLAN_OVERHEAD_MINUTES + daySets(day) * PLAN_MINUTES_PER_SET).toInt()

fun recomputeDay(day: PlanDayDto): PlanDayDto =
    day.copy(totalSets = daySets(day), estimatedMinutes = dayMinutes(day))

fun newCanonicals(proposal: PlanProposalDto): List<PlanExerciseDto> =
    proposal.days
        .flatMap { it.exercises }
        .filter { it.isNew }
        .distinctBy { it.ref }

fun planSummary(proposal: PlanProposalDto): String {
    val days = proposal.days.size
    val sets = proposal.days.sumOf { daySets(it) }
    val news = newCanonicals(proposal).size

    val parts = mutableListOf(
        if (days == 1) "1 dia" else "$days dias",
        "$sets séries",
    )

    if (news > 0) parts += if (news == 1) "1 exercício novo" else "$news exercícios novos"

    return parts.joinToString(" · ")
}

fun planProblems(proposal: PlanProposalDto): List<String> {
    val problems = mutableListOf<String>()

    if (proposal.days.isEmpty()) problems += "O plano ficou sem nenhum dia."
    if (proposal.planUuid.isBlank()) problems += "O plano perdeu a identificação. Gere outro."

    proposal.days.forEach { day ->
        if (day.exercises.isEmpty()) problems += "${day.name} ficou sem exercícios."
        if (day.name.isBlank()) problems += "Um dos dias ficou sem nome."
    }

    val duplicated = proposal.days
        .flatMap { it.exercises }
        .groupBy { it.ref }
        .filterValues { it.size > 1 }
        .keys

    if (duplicated.isNotEmpty()) problems += "Há exercício repetido no plano."

    return problems
}

fun canConfirm(proposal: PlanProposalDto): Boolean = planProblems(proposal).isEmpty()

fun withSets(draft: PlanDraft, dayNumber: Int, ref: String, sets: Int): PlanDraft =
    mapItem(draft, dayNumber, ref) { it.copy(targetSets = sets.coerceIn(PLAN_MIN_SETS, PLAN_MAX_SETS)) }

fun withReps(draft: PlanDraft, dayNumber: Int, ref: String, reps: Int?): PlanDraft =
    mapItem(draft, dayNumber, ref) {
        it.copy(targetReps = reps?.coerceIn(PLAN_MIN_REPS, PLAN_MAX_REPS))
    }

fun withoutItem(draft: PlanDraft, dayNumber: Int, ref: String): PlanDraft {
    val days = draft.proposal.days.mapNotNull { day ->
        if (day.day != dayNumber) return@mapNotNull day

        val kept = day.exercises.filterNot { it.ref == ref }

        if (kept.isEmpty()) null else recomputeDay(day.copy(exercises = kept))
    }

    return draft.copy(proposal = draft.proposal.copy(days = renumber(days)))
}

fun withReplacement(
    draft: PlanDraft,
    dayNumber: Int,
    ref: String,
    replacement: PlanExerciseDto,
): PlanDraft = mapItem(draft, dayNumber, ref) { current ->
    replacement.copy(
        targetSets = current.targetSets,
        targetReps = if (replacement.executionType == "time") null else replacement.targetReps ?: current.targetReps,
        restSeconds = replacement.restSeconds ?: current.restSeconds,
    )
}

fun usedRefs(proposal: PlanProposalDto): Set<String> =
    proposal.days.flatMap { it.exercises }.map { it.ref }.toSet()

fun alternativesFor(
    item: PlanExerciseDto,
    catalog: List<ExerciseEntity>,
    primaryOf: (Int) -> List<PlanMuscleDto>,
    used: Set<String>,
    limit: Int = 12,
): List<PlanExerciseDto> {
    val wanted = item.muscles.primary.map { it.slug }.toSet()

    if (wanted.isEmpty()) return emptyList()

    return catalog
        .sortedWith(compareByDescending<ExerciseEntity> { it.sessionsCount }.thenBy { it.name })
        .asSequence()
        .mapNotNull { exercise ->
            val ref = "cat:${exercise.id}"

            if (ref in used) return@mapNotNull null

            val primary = primaryOf(exercise.id)

            if (primary.none { it.slug in wanted }) return@mapNotNull null

            PlanExerciseDto(
                ref = ref,
                exerciseId = exercise.id,
                name = exercise.name,
                isNew = false,
                equipment = exercise.equipment,
                executionType = exercise.executionType,
                usesWeight = exercise.usesWeight,
                compound = primary.size > 1,
                region = item.region,
                muscles = item.muscles.copy(primary = primary, secondary = emptyList()),
                targetSets = item.targetSets,
                targetReps = if (exercise.executionType == "time") null else item.targetReps,
                targetWeightKg = null,
                restSeconds = exercise.restSeconds ?: item.restSeconds,
                notes = null,
            )
        }
        .take(limit)
        .toList()
}

fun toConfirmRequest(proposal: PlanProposalDto): PlanConfirmRequest = PlanConfirmRequest(
    planUuid = proposal.planUuid,
    source = proposal.source,
    days = proposal.days.map { day ->
        PlanConfirmDayRequest(
            name = day.name,
            exercises = day.exercises.map { item ->
                PlanConfirmExerciseRequest(
                    ref = item.ref,
                    targetSets = item.targetSets,
                    targetReps = item.targetReps,
                    targetWeightKg = item.targetWeightKg,
                    restSeconds = item.restSeconds,
                    notes = item.notes,
                )
            },
        )
    },
)

fun equipmentOptions(fromServer: List<String>, fromCatalog: List<String>): List<String> =
    (fromServer + fromCatalog)
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinctBy { it.lowercase() }
        .sortedBy { it.lowercase() }

fun objectiveLabel(value: String): String = PLAN_OBJECTIVE_LABELS[value] ?: value

private fun mapItem(
    draft: PlanDraft,
    dayNumber: Int,
    ref: String,
    transform: (PlanExerciseDto) -> PlanExerciseDto,
): PlanDraft {
    val days = draft.proposal.days.map { day ->
        if (day.day != dayNumber) return@map day

        recomputeDay(day.copy(exercises = day.exercises.map { if (it.ref == ref) transform(it) else it }))
    }

    return draft.copy(proposal = draft.proposal.copy(days = days))
}

private fun renumber(days: List<PlanDayDto>): List<PlanDayDto> =
    days.mapIndexed { index, day -> day.copy(day = index + 1) }
